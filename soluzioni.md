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

## 5. Decoupling completo tra `booking` e `giftcard` con Published Language e ACL

### Problema

Dopo l'introduzione di `GiftCardReference`, la dipendenza `booking → giftcard` era risolta, ma `giftcard` dipendeva ancora direttamente da `booking.domain.events.BookingResultEvents`:

```text
giftcard.application.policies.CreditGiftCardPolicy  → booking.domain.events.BookingResultEvents
giftcard.application.policies.RefundGiftCardPolicy  → booking.domain.events.BookingResultEvents
giftcard.application.integration.booking.handlers.CreditFromBooking → booking.domain.events.BookingResultEvents
giftcard.application.integration.booking.handlers.RefundFromBooking  → booking.domain.events.BookingResultEvents
```

Questo violava il confine cross-BC: `giftcard` conosceva il modello interno di `booking`.

### Soluzione adottata

Sono stati introdotti **Published Language** lato `booking` e **Anti-Corruption Layer** lato `giftcard`:

1. **Published Language di `booking`** in `booking.integration.giftcard`:
   - `BookingResultIntegrationEvent` (sealed interface / union type);
   - `BookingCompletedIntegrationEvent`, `BookingRefusedIntegrationEvent`, `BookingRejectedIntegrationEvent`.
   - I campi usano solo tipi stabili (`UUID`/`String`, `Money`).

2. **Pubblicazione degli eventi di integrazione** in `BookingModule`:
   - `BookingModule` continua a pubblicare eventi interni sul proprio event bus;
   - quando invoca handler cross-BC, traduce `BookingConfirmed`/`BookingRefused`/`BookingRejected` nei corrispondenti eventi di integrazione.

3. **Anti-Corruption Layer** in `giftcard.application.integration.booking`:
   - `adapter.BookingResult` traduce `BookingResultIntegrationEvent` in `CreditGiftCard`;
   - traduce `BookingRejectedIntegrationEvent` in `RefundGiftCard`.
   - È l'unico punto di `giftcard` che conosce il contratto pubblicato da `booking`.
   - `handlers.CreditFromBooking` e `handlers.RefundFromBooking` orchestrano l'adapter e i rispettivi use case.

4. **Rimozione delle vecchie policy** `CreditGiftCardPolicy` e `RefundGiftCardPolicy`, che non potevano più implementare `Policy` (gli eventi di integrazione non estendono `Event`) e il cui ruolo era in realtà quello di adapter/ACL, non di policy decisionale.

```text
booking
  └── integration/giftcard
      └── BookingResultIntegrationEvent          <- Published Language

giftcard
  └── application/integration/booking
      ├── adapter
      │   └── BookingResult                         <- ACL
      │       ├── adapt(BookingCompleted) → CreditGiftCard
      │       ├── adapt(BookingRefused)   → CreditGiftCard
      │       └── adaptRejected(BookingRejected) → RefundGiftCard
      └── handlers
          ├── CreditFromBooking                     <- usa BookingResult + GiftCardCrediting
          └── RefundFromBooking                     <- usa BookingResult + GiftCardRefunding
```

### Motivazione

- **Separazione tra eventi interni e pubblicati**: il modello interno di `booking` può evolvere senza impattare `giftcard`, purché la Published Language resti stabile.
- **ACL esplicito**: `adapter.BookingResult` isola il modello di `giftcard` dalle convenzioni di `booking`. Se `booking` cambia il nome degli eventi interni o aggiunge campi, solo l'adapter e il punto di pubblicazione in `BookingModule` vengono toccati.
- **Nessuna forzatura del pattern `Policy`**: gli eventi di integrazione non sono eventi di dominio (non hanno `aggregateId` di `booking`), quindi non implementano `Policy`. L'ACL è una classe di mapping, non una policy.
- **Pilot per `payment`**: lo stesso pattern verrà applicato per risolvere `booking → payment` e `giftcard → payment`, introducendo `payment.integration` unico e ACL nei due BC downstream.

### Dipendenze rimanenti

Dopo questa modifica:

- ✅ `booking` non dipende più da `giftcard`.
- ✅ `giftcard` non dipende più da `booking`.
- ❌ `booking` dipende ancora da `payment`.
- ❌ `giftcard` dipende ancora da `payment`.

---

## 6. Decoupling del flusso eventi `payment → giftcard` con Published Language e ACL

### Problema

`giftcard` dipendeva direttamente dagli eventi interni di `payment` per confermare una ricarica:

```text
giftcard.application.policies.ConfirmTopUpPolicy      → payment.domain.events.PaymentResultEvents
giftcard.application.services.TopUpConfirmation       → payment.domain.events.PaymentResultEvents
```

Questo violava il confine cross-BC: `giftcard` conosceva il modello interno di `payment`.

### Soluzione adottata

Sono stati introdotti **Published Language** lato `payment` e **Anti-Corruption Layer** lato `giftcard`:

1. **Published Language di `payment`** in `payment.integration`:
   - `PaymentResultIntegrationEvent` (sealed interface / union type);
   - `PaymentAcceptedIntegrationEvent`, `PaymentRejectedIntegrationEvent`, `PaymentExpiredIntegrationEvent`.
   - I campi usano solo tipi stabili (`UUID`/`String`, `Money`).

2. **Pubblicazione degli eventi di integrazione** in `PaymentModule`:
   - `PaymentModule` continua a pubblicare eventi interni sul proprio event bus;
   - quando riceve `PaymentAccepted`/`PaymentRejected`/`PaymentExpired`, traduce ciascuno nel corrispondente evento di integrazione e lo notifica agli handler cross-BC registrati (`onPaymentAccepted`, `onPaymentRejected`, `onPaymentExpired`).

3. **Anti-Corruption Layer** in `giftcard.application.integration.payment`:
   - `adapter.PaymentResult` traduce `PaymentResultIntegrationEvent` in `ConfirmTopUp` (solo per `PaymentAcceptedIntegrationEvent`);
   - `handlers.ConfirmTopUpFromPayment` orchestra l'adapter e il caso d'uso `TopUpConfirming`.
   - È l'unico punto di `giftcard` che conosce il contratto pubblicato da `payment`.

4. **Rimozione delle vecchie policy/servizio** `ConfirmTopUpPolicy` e `TopUpConfirmation`, che non potevano più restare nel dominio/application interni di `giftcard` perché importavano direttamente `payment`.

```text
payment
  └── integration
      └── PaymentResultIntegrationEvent          <- Published Language generica

giftcard
  └── application/integration/payment
      ├── adapter
      │   └── PaymentResult                       <- ACL
      │       └── adapt(PaymentAccepted) → ConfirmTopUp
      └── handlers
          └── ConfirmTopUpFromPayment             <- usa PaymentResult + TopUpConfirming
```

### Motivazione

- **Published Language stabile e riutilizzabile**: `payment.integration.PaymentResultIntegrationEvent` è un contratto generico che può essere consumato da qualsiasi BC downstream (`giftcard` e `booking`) senza esporre il modello interno di `payment`.
- **ACL esplicito**: ciascun BC downstream ha il proprio adapter (`giftcard.application.integration.payment.adapter.PaymentResult` e `booking.application.integration.payment.adapter.PaymentResult`) che isola il proprio modello dalle convenzioni di `payment`. Se `payment` cambia nome o struttura degli eventi interni, solo il punto di pubblicazione in `PaymentModule` e gli ACL vengono toccati.
- **Nessuna forzatura del pattern `Policy`**: gli eventi di integrazione non sono eventi di dominio di `payment` (non hanno `aggregateId` di `payment`), quindi non implementano `Policy`. L'handler cross-BC è un semplice orchestratore event-driven.

### Estensione del pattern a `booking`

La stessa `PaymentResultIntegrationEvent` è stata consumata anche da `booking`, introducendo:

- `booking.application.integration.payment.adapter.PaymentResult` — traduce `PaymentAcceptedIntegrationEvent`/`PaymentRejectedIntegrationEvent` in `ConfirmBooking`/`RejectBooking`. A differenza dell'adapter di `giftcard`, qui è necessario caricare il booking dal repository perché il `clientReference` dell'evento di pagamento è l'ID della prenotazione.
- `booking.application.integration.payment.handlers.HandlePaymentResultFromPayment` — orchestra l'adapter e i casi d'uso `BookingConfirming`/`BookingRejecting`.

Sono stati rimossi `PaymentPolicy` e `PaymentResultOutcome`, che importavano direttamente `payment.domain.events.PaymentResultEvents`.

```text
payment
  └── integration
      └── PaymentResultIntegrationEvent          <- Published Language generica

booking
  └── application/integration/payment
      ├── adapter
      │   └── PaymentResult                       <- ACL
      │       ├── adapt(PaymentAccepted) → ConfirmBooking
      │       └── adapt(PaymentRejected) → RejectBooking
      └── handlers
          └── HandlePaymentResultFromPayment      <- usa PaymentResult + BookingConfirming/Rejecting
```

### Dipendenze rimanenti

Dopo questa modifica:

- ✅ `booking` non dipende più da `giftcard`.
- ✅ `giftcard` non dipende più da `booking`.
- ✅ `giftcard` non dipende più dagli eventi interni di `payment`.
- ✅ `booking` non dipende più dagli eventi interni di `payment`.
- ❌ `booking` dipende ancora da `payment` sul flusso **command** (`BookingPaymentRequestPolicy`, `BookingRefundRequestPolicy`).
- ❌ `giftcard` dipende ancora da `payment` sul flusso **command** (`TopUpPaymentRequestPolicy`).

---

## 7. Cosa non è stato ancora risolto (e perché)

Le uniche violazioni rimaste attive sul branch `solutions` riguardano il flusso **command** verso `payment`:

- `booking` dipende da `payment` (`BookingPaymentRequestPolicy`, `BookingRefundRequestPolicy`).
- `giftcard` dipende da `payment` (`TopUpPaymentRequestPolicy`).

### Perché non è stato risolto

Il disegno completo dei confini tra BC è l'argomento più ampio del workshop. Richiede di introdurre pattern di integrazione come:

- eventi di integrazione separati dagli eventi interni;
- API esposte pubblicamente da ciascun BC;
- anti-corruption layer per isolare i modelli esterni.

Questo punto viene approfondito nelle soluzioni finali del workshop.

---

## 8. Evoluzioni future

Il branch `feature/usecase-aff-rule` contiene un'ulteriore evoluzione: una regola `useCasesMustImplementUseCase` che verifica che tutte le classi in `application/usecases` implementino l'interfaccia `UseCase`. Quella regola rileva che `PaymentExpiring`, `TransactionAccepting` e `TransactionRejecting` sono in realtà `EventSubscriber`, non use case: apre il discorso su dove collocare gli orchestratori event-driven e su come distinguere use case, servizi applicativi e event handler. Il tema verrà affrontato in un momento successivo del workshop.
