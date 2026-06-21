# Soluzioni adottate per risolvere le violazioni architetturali

> Questo documento descrive le scelte di refactoring operate sul branch `solutions` per risolvere le violazioni rilevate dalle Architectural Fitness Functions (AFF).

---

## 1. Le policy non appartengono al dominio

### Problema

Le policy erano collocate nel layer `domain`:

- le implementazioni concrete risiedevano in `<bc>.domain.policies`;
- l'interfaccia condivisa `Policy` risiedeva in `common.domain.model`.

Entrambe dipendevano però da costrutti del layer `application` (`Command`), invertendo la dipendenza esagonale: il domain non deve conoscere i dettagli dei casi d'uso.

Esempi di violazione:

```text
booking.domain.policies.PaymentPolicy
  → booking.application.commands.BookingConfirmationCommands

giftcard.domain.policies.ConfirmTopUpPolicy
  → giftcard.application.commands.ConfirmTopUp

payment.domain.policies.PaymentCompletion
  → payment.application.commands.AcceptTransaction

common.domain.model.Policy
  → common.application.Command
```

### Soluzione

Le policy sono state ricollocate tutte nel layer `application`:

- le implementazioni concrete in `<bc>.application.policies`;
- l'interfaccia `Policy` in `common.application`.

```text
src/main/java/it/giannibombelli/wsc2026/<bc>/application/policies/
src/main/java/it/giannibombelli/wsc2026/common/application/Policy.java
```

### Motivazione

Le policy non sono logica di dominio pura. Sono **orchestrazione reattiva**: ricevono un evento di dominio e decidono quale comando eseguire. Il loro posto naturale è quindi nel layer `application`, che coordina l'evoluzione degli aggregate e la pubblicazione di nuovi eventi.

Spostando le policy concrete in `application.policies`:

- si ripristina la dipendenza corretta (`application` può dipendere da `domain`, non viceversa);
- il modello di dominio resta isolato dai dettagli dei casi d'uso;
- la regola AFF `domainMustNotDependOnOuterLayers` torna a passare per tutti i BC.

Spostando l'interfaccia `Policy` in `common.application`:

- si elimina l'ultima dipendenza `common.domain → common.application`;
- il package `common.domain` torna a dipendere solo da sé stesso;
- le policy di ciascun BC implementano un contratto coerente, anch'esso nel layer `application`.

---

## 2. Ricollocazione di `PaymentCharging` in `application/services`

### Problema

`PaymentCharging` era collocato in `payment.domain.policies` ma non implementava `Policy`. La regola `policiesMustImplementPolicy` falliva perché il metodo `charge(TransactionStarted)` restituisce `PaymentProviderResult`, non un `Command`.

### Soluzione

`PaymentCharging` è stato spostato in `payment.application.services`.

### Motivazione

`PaymentCharging` non è una policy: è un componente che, a fronte di un evento, invoca direttamente il provider esterno per processare il pagamento. Il suo ruolo è più vicino a quello di un **adapter/processor** o di un **servizio applicativo event-driven** che a quello di una policy decisionale. Collocarlo in `application.services`:

- lo toglie dal package delle policy, facendo passare la shape rule;
- riflette meglio la sua natura di orchestratore tecnico verso l'esterno;
- lascia aperto il punto su dove posizionarlo quando verranno creati gli adapter verso i provider reali (PayPal, Klarna, ecc.).

> Nota: il BC `payment` è parzialmente out-of-scope nel workshop. Il ruolo definitivo di `PaymentCharging` verrà rivalutato quando gli adapter verso i payment provider saranno creati e integrati.

---

## 3. Introduzione del layer `application/query`

### Problema

`BookingApi` e `GiftCardApi` dipendevano direttamente dai rispettivi repository SQLite (`SqliteBookingRepository` e `SqliteGiftCardRepository`), violando la regola esagonale che vuole l'`api` separata dall'`infrastructure`.

### Soluzione

È stato introdotto un layer `application/query` per ciascun BC:

- `BookingQueryService` + `BookingDetails` in `booking.application.query`;
- `GiftCardQueryService` + `GiftCardDetails` in `giftcard.application.query`.

L'`api` ora dipende solo dal query service; il query service dipende dalla porta del repository (`domain.ports`), non dall'implementazione SQLite.

```text
booking.api.BookingApi
  → booking.application.query.BookingQueryService
  → booking.domain.ports.BookingRepository
  → booking.infrastructure.SqliteBookingRepository

giftcard.api.GiftCardApi
  → giftcard.application.query.GiftCardQueryService
  → giftcard.domain.ports.GiftCardRepository
  → giftcard.infrastructure.SqliteGiftCardRepository
```

### Motivazione

Il query layer:

- disaccoppia il contratto HTTP dai dettagli di persistenza;
- permette di far evolvere indipendentemente il modello di dominio e il DTO di risposta;
- mantiene la dipendenza esagonale corretta, perché `application.query` dipende solo dalla porta del repository;
- fornisce un punto naturale in cui introdurre in futuro un read model CQRS, senza toccare l'`api`.

> Nota sulle regole di purity: i DTO di query (`BookingDetails`, `GiftCardDetails`) riutilizzano i value object di dominio (`Description`, `Money`) e tipi semplici (`UUID`, `String`), mantenendo il package `application.query` allineato alle regole di purity degli altri layer application. Non è stata introdotta alcuna esclusione specifica, perché i query service non dipendono da librerie/framework e i read model sono ancora parte del contratto applicativo.

---

## 4. Decoupling del riferimento a gift card nel BC `booking`

### Problema

L'aggregato `Booking` e i suoi eventi di dominio (`BookingPlaced`, `BookingResultEvents`) importavano direttamente `GiftCardId` dal BC `giftcard` (`giftcard.domain.giftcard.GiftCardId`).

Questa dipendenza rendeva `booking` sensibile ai cambiamenti interni di `giftcard`: un refactor del tipo identificativo della gift card avrebbe propagato il cambiamento in `booking`, rompendo il confine del BC.

### Soluzione adottata

È stato introdotto un value object **proprietà di `booking`**:

```text
booking.domain.primitive.GiftCardReference
```

`Booking` ora modella il riferimento a una gift card esterna come un concetto del proprio dominio:

```text
Booking.place(id, description, giftCardReference)
Booking.confirm(amount)
Booking.reject(amount)
Booking.giftCardReference() -> GiftCardReference
```

Gli eventi di dominio di `booking` espongono il riferimento come **published language** sotto forma di primitiva (`UUID` in Java, `string` in TypeScript):

```text
BookingPlaced.giftCardReference: UUID/String
BookingConfirmed.giftCardReference: UUID/String
BookingRefused.giftCardReference: UUID/String
BookingRejected.giftCardReference: UUID/String
```

Il BC `giftcard`, quando reagisce a questi eventi, traduce la primitiva nel proprio `GiftCardId` solo al proprio confine, nelle policy:

```text
giftcard.application.policies.CreditGiftCardPolicy
  → new GiftCardId(event.giftCardReference())
```

### Motivazione

- **Ogni BC possiede il proprio linguaggio**: `GiftCardReference` rappresenta il concetto "una gift card vista da booking", non il concetto interno di `giftcard`.
- **Nessun accoppiamento sui tipi di dominio**: `booking` non conosce più `giftcard.GiftCardId`.
- **Published language stabile**: `UUID`/`String` è un contratto semplice e stabile per l'integrazione.
- **Traduzione al confine**: la conversione in `GiftCardId` avviene nel BC destinatario, dove il concetto è rilevante.

### Alternative considerate

| Approccio | Pro | Contro |
|---|---|---|
| **Shared kernel** (`GiftCardId` in `common`) | Nessuna traduzione; stesso tipo usato da entrambi i BC. | Aumenta l'accoppiamento; un cambiamento del tipo in `giftcard` impatta `booking`; rischia di espandersi in un "big ball of shared kernel". |
| **Duplicare `GiftCardId` in `booking`** | Rimuove la dipendenza incrociata. | Due classi con lo stesso nome e struttura suggeriscono un'identità di concetto che invece non dovrebbe esistere; confusione nel modello. |
| **Usare una primitiva nuda (`UUID`/`String`) direttamente in `Booking`** | Massimo decoupling; nessun value object aggiuntivo. | Perde espressività nel linguaggio di dominio di `booking`; il campo diventa un generico identificatore senza semantica. |
| **Eventi di integrazione separati dagli eventi interni** | Isolamento completo; i BC comunicano solo tramite contratti pubblicati. | Maggiore complessità; richiede un adapter/ACL esplicito per ogni direzione di integrazione. |

L'approccio con `GiftCardReference` è stato scelto come compromesso tra **pulizia del modello di dominio** e **semplicità**: esplicita la relazione tra i due BC senza condividerne i tipi interni.

---

## 5. Cosa non è stato ancora risolto (e perché)

Le violazioni rimaste attive sul branch `solutions` riguardano altre dipendenze cross-BC:

- `booking` dipende da `payment` (`BookingPaymentRequestPolicy`, `BookingRefundRequestPolicy`, `PaymentPolicy`, `PaymentResultOutcome`).
- `giftcard` dipende da `payment` (`ConfirmTopUpPolicy`, `TopUpPaymentRequestPolicy`, `TopUpConfirmation`) e da `booking` (`CreditGiftCardPolicy`, `RefundGiftCardPolicy`, `BookingResultCrediting`, `BookingResultRefunding`).

### Perché non è stato risolto

Il disegno completo dei confini tra BC è l'argomento più ampio del workshop. Richiede di introdurre pattern di integrazione come:

- eventi di integrazione separati dagli eventi interni;
- API esposte pubblicamente da ciascun BC;
- anti-corruption layer per isolare i modelli esterni.

Questo punto viene approfondito nelle soluzioni finali del workshop.

---

## 6. Evoluzioni future

Il branch `feature/usecase-aff-rule` contiene un'ulteriore evoluzione: una regola `useCasesMustImplementUseCase` che verifica che tutte le classi in `application/usecases` implementino l'interfaccia `UseCase`. Quella regola rileva che `PaymentExpiring`, `TransactionAccepting` e `TransactionRejecting` sono in realtà `EventSubscriber`, non use case: apre il discorso su dove collocare gli orchestratori event-driven e su come distinguere use case, servizi applicativi e event handler. Il tema verrà affrontato in un momento successivo del workshop.
