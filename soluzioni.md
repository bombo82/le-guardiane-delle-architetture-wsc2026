# Soluzioni adottate per risolvere le violazioni architetturali

> Questo documento elenca i problemi architetturali presenti nella codebase e descrive le soluzioni adottate sul branch `solutions`.
> L'elenco dei problemi è completo fin dal primo commit del branch; ogni soluzione compare nel commit che risolve il problema corrispondente.

---

## Elenco dei problemi

### 1. Le policy risiedono nel layer `domain`

Le policy sono collocate nel layer `domain`:

- le implementazioni concrete in `<bc>.domain.policies`;
- l'interfaccia condivisa `Policy` in `common.domain.model`.

Entrambe dipendono però da costrutti del layer `application` (`Command`), invertendo la dipendenza esagonale: il domain non deve conoscere i dettagli dei casi d'uso.

```text
booking.domain.policies.PaymentPolicy        → booking.application.commands.BookingConfirmationCommands
giftcard.domain.policies.ConfirmTopUpPolicy  → giftcard.application.commands.ConfirmTopUp
payment.domain.policies.PaymentCompletion    → payment.application.commands.AcceptTransaction
common.domain.model.Policy                   → common.application.Command
```

### 2. `PaymentCharging` è collocato tra le policy senza essere una policy

`PaymentCharging` risiede in `payment.domain.policies` ma non implementa `Policy`: il metodo `charge(TransactionStarted)` restituisce `PaymentProviderResult`, non un `Command`. La regola di shape `policiesMustImplementPolicy` fallisce.

### 3. L'`api` dipende direttamente dall'`infrastructure`

`BookingApi` e `GiftCardApi` dipendono dai rispettivi repository SQLite (`SqliteBookingRepository`, `SqliteGiftCardRepository`), saltando il layer `application` e violando la separazione esagonale tra `api` e `infrastructure`.

### 4. `booking` dipende da `giftcard` sul tipo `GiftCardId`

L'aggregato `Booking` e i suoi eventi di dominio (`BookingPlaced`, `BookingResultEvents`) importano direttamente `giftcard.domain.giftcard.GiftCardId`. Un refactor del tipo identificativo della gift card si propagherebbe in `booking`, rompendo il confine del BC.

### 5. `giftcard` dipende dagli eventi interni di `booking`

```text
giftcard.application.policies.CreditGiftCardPolicy   → booking.domain.events.BookingResultEvents
giftcard.application.policies.RefundGiftCardPolicy   → booking.domain.events.BookingResultEvents
giftcard.application.integration.booking.handlers.*  → booking.domain.events.BookingResultEvents
```

### 6. `giftcard` e `booking` dipendono dagli eventi interni di `payment`

```text
giftcard.application.policies.ConfirmTopUpPolicy     → payment.domain.events.PaymentResultEvents
giftcard.application.services.TopUpConfirmation      → payment.domain.events.PaymentResultEvents
booking.application.policies.PaymentPolicy           → payment.domain.events.PaymentResultEvents
booking.application.services.PaymentResultOutcome    → payment.domain.events.PaymentResultEvents
```

### 7. `booking` e `giftcard` dipendono dai command interni di `payment`

```text
booking.application.policies.BookingPaymentRequestPolicy  → payment.application.commands.RequestPayment
booking.application.policies.BookingRefundRequestPolicy   → payment.application.commands.RefundTransaction
giftcard.application.policies.TopUpPaymentRequestPolicy   → payment.application.commands.RequestPayment
```

### 8. Il composition root dipende dagli internals dei moduli

`Application` consuma handler concreti esposti pubblicamente dai moduli:

```text
booking.BookingModule.handlePaymentResultFromPayment()
giftcard.GiftCardModule.confirmTopUpFromPayment()
giftcard.GiftCardModule.creditFromBooking()
giftcard.GiftCardModule.refundFromBooking()
```

e invoca direttamente gli adapter ACL dei moduli downstream per tradurre eventi di dominio in command verso `payment`:

```java
giftCardModule.onTopUpRequested(event -> paymentModule.requestPayment(PaymentRequest.fromTopUp(event)));
bookingModule.onBookingPlaced(event -> paymentModule.requestPayment(PaymentRequest.fromBookingPlaced(event)));
bookingModule.onBookingResult(event -> { ... RefundRequest.fromBookingRefused(refused) ... });
```

Il composition root così conosce la struttura interna dei moduli e il loro linguaggio di dominio (`GiftCardTopUpRequested`, `BookingPlaced`, `BookingRefused`), rompendo l'incapsulamento.

### 9. Il contratto pubblico dei moduli non è definito

I moduli non espongono un'interfaccia esplicita per l'adapter web: `configure(...)` fa parte del contratto pubblico, i controller HTTP sono eterogenei, le sottoscrizioni agli eventi sono registrate fuori dal costruttore e il watcher di `payment` è un campo mutabile. Il composition root non ha quindi una superficie pubblica minima e verificabile su cui appoggiarsi.

---

## Soluzioni

### 1. Policy spostate nel layer `application`

**Soluzione**

Le policy concrete sono state ricollocate in `<bc>.application.policies` e l'interfaccia `Policy` in `common.application`. Le policy che attraversavano i confini dei Bounded Context (`BookingPaymentRequestPolicy`, `BookingRefundRequestPolicy`, `TopUpPaymentRequestPolicy`) non sono state semplicemente spostate: sono state eliminate e sostituite da Anti-Corruption Layer + Published Language (sezioni 5 e 7), perché una policy cross-BC introduce coupling sul modello interno di un altro BC.

**Motivazione**

Le policy non sono logica di dominio pura: sono orchestrazione reattiva (evento → comando). Il loro posto naturale è il layer `application`, che coordina aggregati e pubblicazione di eventi. Lo spostamento:

- ripristina la dipendenza corretta (`application` dipende da `domain`, non viceversa);
- mantiene il modello di dominio isolato dai dettagli dei casi d'uso;
- elimina l'ultima dipendenza `common.domain → common.application`, rendendo `common.domain` autonomo;
- fa tornare verde la regola AFF `domainMustNotDependOnOuterLayers` per tutti i BC.

**Alternative considerate**

- *Spostare `Command` in `domain`*: reintrodurrebbe un concetto applicativo nel cuore del modello.
- *Tenere le policy in `domain` con un'eccezione AFF*: un'eccezione difficile da motivare e da mantenere nel tempo.
- *Introdurre un layer "reactive" dedicato*: complessità strutturale ingiustificata per questo codebase.

### 2. `PaymentCharging` ricollocato in `application/services`

**Soluzione**

`PaymentCharging` è stato spostato da `payment.domain.policies` a `payment.application.services`.

**Motivazione**

Non è una policy: a fronte di un evento invoca direttamente il provider esterno, senza produrre un `Command`. È più vicino a un servizio applicativo event-driven che a una policy decisionale. La ricollocazione fa passare la shape rule `policiesMustImplementPolicy` e riflette la natura del componente.

> Il BC `payment` è parzialmente out-of-scope nel workshop: il ruolo definitivo di `PaymentCharging` verrà rivalutato quando esisteranno adapter reali verso i provider (PayPal, Klarna, ecc.).

**Alternative considerate**

- *Forzarlo a implementare `Policy`*: snaturerebbe il componente, il cui esito è un risultato di provider, non un comando.
- *Spostarlo in `infrastructure` come adapter*: invoca una porta di dominio e viene attivato da eventi applicativi; collocazione prematura finché gli adapter reali non esistono.

### 3. Introduzione del layer `application/query`

**Soluzione**

È stato introdotto un layer `application/query` per ciascun BC (`BookingQueryService` + `BookingDetails`, `GiftCardQueryService` + `GiftCardDetails`). L'`api` ora dipende solo dal query service; il query service dipende dalla porta del repository (`domain.ports`), non dall'implementazione SQLite.

```text
booking.api.BookingApi
  → booking.application.query.BookingQueryService
  → booking.domain.ports.BookingRepository
  → booking.infrastructure.SqliteBookingRepository
```

**Motivazione**

Il query layer disaccoppia il contratto HTTP dai dettagli di persistenza, lascia evolvere indipendentemente modello di dominio e DTO di risposta, ripristina la dipendenza esagonale corretta e fornisce un punto naturale per un futuro read model CQRS senza toccare l'`api`.

> I DTO di query riutilizzano value object di dominio (`Description`, `Money`) e tipi semplici (`UUID`, `String`): il package `application.query` resta allineato alle regole di purity degli altri layer application, senza bisogno di esclusioni specifiche.

**Alternative considerate**

- *Far dipendere l'`api` dalla porta repository*: resterebbe il bypass del layer `application` e l'esposizione del contratto di persistenza nel layer di ingresso.
- *Restituire l'aggregato di dominio dall'`api`*: esporrebbe il modello interno nel contratto HTTP.

### 4. `GiftCardReference` come linguaggio proprio di `booking`

**Soluzione**

È stato introdotto il value object `booking.domain.primitive.GiftCardReference`, proprietà di `booking`: `Booking` modella il riferimento a una gift card esterna come concetto del proprio dominio. Gli eventi di `booking` lo espongono come published language sotto forma di primitiva (`UUID`/`String`); il BC `giftcard` lo traduce nel proprio `GiftCardId` solo al proprio confine.

**Motivazione**

- Ogni BC possiede il proprio linguaggio: `GiftCardReference` è "una gift card vista da booking", non il concetto interno di `giftcard`.
- Nessun accoppiamento sui tipi di dominio: `booking` non conosce più `GiftCardId`.
- La primitiva `UUID`/`String` è un contratto di integrazione semplice e stabile.
- La traduzione avviene nel BC destinatario, dove il concetto è rilevante.

**Alternative considerate**

| Approccio | Pro | Contro |
|---|---|---|
| Shared kernel (`GiftCardId` in `common`) | Nessuna traduzione. | Accoppiamento crescente; rischio di "big ball of shared kernel". |
| Duplicare `GiftCardId` in `booking` | Rimuove la dipendenza. | Due tipi identici suggeriscono un'identità di concetto inesistente. |
| Primitiva nuda (`UUID`/`String`) in `Booking` | Massimo decoupling. | Perde espressività nel linguaggio di `booking`. |
| Eventi di integrazione separati | Isolamento completo. | Richiede un ACL esplicito per direzione (adottato poi nelle sezioni 5-7). |

`GiftCardReference` è il compromesso scelto tra pulizia del modello e semplicità.

### 5. Decoupling `booking` ↔ `giftcard` con Published Language e ACL

**Soluzione**

1. Published Language di `booking` in `booking.integration.giftcard`: `BookingResultIntegrationEvent` (sealed/union) con i sottotipi `BookingCompleted`, `BookingRefused`, `BookingRejected`; campi solo di tipi stabili (`UUID`/`String`, `Money`).
2. `BookingModule` traduce gli eventi interni `BookingConfirmed`/`BookingRefused`/`BookingRejected` nei corrispondenti eventi di integrazione quando invoca handler cross-BC.
3. ACL in `giftcard.application.integration.booking`: `adapter.BookingResult` traduce gli eventi di integrazione in `CreditGiftCard`/`RefundGiftCard`; `handlers.CreditFromBooking` e `RefundFromBooking` orchestrano adapter e use case.
4. Rimozione delle vecchie policy `CreditGiftCardPolicy` e `RefundGiftCardPolicy`: gli eventi di integrazione non sono eventi di dominio (niente `aggregateId` di `booking`), quindi non possono implementare `Policy`; il loro ruolo era già quello di adapter.

```text
booking
  └── integration/giftcard
      └── BookingResultIntegrationEvent          <- Published Language

giftcard
  └── application/integration/booking
      ├── adapter/BookingResult                   <- ACL
      └── handlers/{CreditFromBooking, RefundFromBooking}
```

**Motivazione**

- Separazione tra eventi interni e pubblicati: il modello di `booking` evolve senza impattare `giftcard`, purché la PL resti stabile.
- ACL esplicito: un solo punto di `giftcard` conosce il contratto pubblicato da `booking`.
- Nessuna forzatura del pattern `Policy`: l'ACL è mapping, non decisione reattiva.

**Alternative considerate**

- *Condividere gli eventi di dominio come contratto pubblico*: espone il modello interno di `booking` a ogni sua evoluzione.
- *Tradurre gli eventi nel composition root*: sposta il coupling nel punto meno coeso del sistema (vedi problema 8).
- *Message broker con schema registry*: oltre lo scope didattico del workshop.

### 6. Decoupling del flusso eventi `payment` → `giftcard` / `booking`

**Soluzione**

1. Published Language di `payment` in `payment.integration`: `PaymentResultIntegrationEvent` con i sottotipi `PaymentAccepted`, `PaymentRejected`, `PaymentExpired`; campi solo di tipi stabili.
2. `PaymentModule` traduce gli eventi interni nei corrispondenti eventi di integrazione e li notifica agli handler cross-BC registrati.
3. ACL in `giftcard.application.integration.payment`: `adapter.PaymentResult` traduce `PaymentAcceptedIntegrationEvent` in `ConfirmTopUp`; `handlers.ConfirmTopUpFromPayment` orchestra l'adapter e `TopUpConfirming`. Rimossi `ConfirmTopUpPolicy` e `TopUpConfirmation`.
4. Estensione a `booking`: `booking.application.integration.payment.adapter.PaymentResult` traduce `PaymentAccepted`/`PaymentRejected` in `ConfirmBooking`/`RejectBooking` (qui serve caricare il booking dal repository, perché il `clientReference` dell'evento è l'ID della prenotazione); `handlers.HandlePaymentResultFromPayment` orchestra adapter e use case. Rimossi `PaymentPolicy` e `PaymentResultOutcome`.

**Motivazione**

- PL generica e riutilizzabile: un unico contratto consumato da più BC downstream senza esporre il modello interno di `payment`.
- ACL per ciascun downstream: se `payment` cambia gli eventi interni, solo il punto di pubblicazione e gli adapter vengono toccati.
- Nessuna forzatura del pattern `Policy`: gli handler cross-BC sono orchestratori event-driven, non policy.

**Alternative considerate**

- *Una PL distinta per ogni consumatore (customer-supplier)*: più contratti da mantenere con lo stesso contenuto.
- *Sottoscrivere gli eventi di dominio filtrandoli nei moduli downstream*: reintrodurrebbe la conoscenza del contratto interno di `payment`.

### 6.1 Semplificazione del wiring applicativo

Problema emerso durante il refactoring (non presente nella codebase iniziale): dopo l'introduzione di PL e ACL, il composition root era diventato verboso — moduli costruiti con liste di handler e registrazioni ripetute con tre chiamate separate (`onPaymentAccepted`/`onPaymentRejected`/`onPaymentExpired`).

**Soluzione**

- Unificate le tre liste di handler di integrazione di `PaymentModule` in una sola, registrata con `onPaymentResult(handler)`.
- Spostata la registrazione degli handler dai costruttori a metodi post-costruzione (`onBookingPlaced`, `onBookingResult`, `onTopUpRequested`, ...): i costruttori dei moduli restano minimi.
- Unificate le liste `bookingConfirmedHandlers`/`bookingRejectedHandlers` in `bookingResultHandlers`.
- Incapsulato il wiring in metodi privati di `Application`: `wireTopUpRequests()`, `wireBookingResults()`, `wirePaymentResults()`.

**Motivazione**

Il composition root resta il punto unico e leggibile in cui i BC vengono collegati, senza il rumore delle liste vuote e delle registrazioni ripetute, e senza introdurre framework di DI: le dipendenze continuano a essere cablate esplicitamente a mano.

**Alternative considerate**

- *Lasciare il wiring verboso*: rumore nel punto architetturalmente più importante.
- *Adottare un DI container*: contro la scelta di minimalità ed esplicità del progetto.

### 7. Decoupling del flusso command `booking` / `giftcard` → `payment`

**Soluzione**

1. Published Language di `payment` in `payment.integration`: `PaymentRequestIntegrationCommand` e `RefundRequestIntegrationCommand`, con campi solo di tipi stabili (`clientReference` come stringa UUID, `amount` come `Money`).
2. ACL in `booking.application.integration.payment`: `adapter.PaymentRequest` traduce `BookingPlaced`, `adapter.RefundRequest` traduce `BookingRefused`.
3. ACL in `giftcard.application.integration.payment`: `adapter.PaymentRequest` traduce `GiftCardTopUpRequested`.
4. Gateway in `PaymentModule`: `requestPayment(...)` e `requestRefund(...)` traducono i command di integrazione nei command interni `RequestPayment`/`RefundTransaction` e li eseguono.
5. Rimozione delle policy cross-BC `BookingPaymentRequestPolicy`, `BookingRefundRequestPolicy`, `TopUpPaymentRequestPolicy`.
6. Regole AFF simmetriche a protezione della nuova PL (`paymentPublishedLanguageMustBeIndependent`, `paymentPublishedLanguageMustNotDependOnBooking/GiftCard`, `onlyBooking/GiftCardAclMayConsumePaymentPublishedLanguage`).

**Motivazione**

- Simmetria con il flusso eventi: `payment` espone una PL sia per gli esiti sia per le richieste, con ACL dedicati nei downstream per entrambe le direzioni.
- I BC downstream non conoscono più `RequestPayment`, `RefundTransaction` o `PaymentId`.
- Il gateway di `PaymentModule` è l'unico punto di ingresso verso `payment` per i command cross-BC.

**Alternative considerate**

- *Esporre direttamente `RequestPayment`/`RefundTransaction` come "command pubblici"*: sono DTO interni che evolvono col dominio; esporli ricreerebbe il coupling.
- *Costruire i command interni nel composition root*: stessa violazione di incapsulamento descritta dal problema 8.

### 8. Incapsulamento dei moduli: sottoscrizioni semantiche

**Soluzione**

I moduli espongono ora metodi di sottoscrizione semantici che nascondono handler e adapter, scambiando solo Published Language:

```text
booking.BookingModule
  ├── handlePaymentResult(PaymentResultIntegrationEvent)
  ├── onBookingPlaced(Consumer<PaymentRequestIntegrationCommand>)
  └── onBookingRefused(Consumer<RefundRequestIntegrationCommand>)

giftcard.GiftCardModule
  ├── handlePaymentResult(PaymentResultIntegrationEvent)
  ├── onTopUpRequested(Consumer<PaymentRequestIntegrationCommand>)
  ├── onBookingCompleted(BookingCompletedIntegrationEvent)
  ├── onBookingRefused(BookingRefusedIntegrationEvent)
  └── onBookingRejected(BookingRejectedIntegrationEvent)
```

I dettagli interni (handler e adapter) restano privati e vengono cablati dal modulo stesso. Il wiring in `Application` diventa:

```java
paymentModule.onPaymentResult(bookingModule::handlePaymentResult);
paymentModule.onPaymentResult(giftCardModule::handlePaymentResult);
bookingModule.onBookingCompletedIntegration(giftCardModule::onBookingCompleted);
bookingModule.onBookingRefusedIntegration(giftCardModule::onBookingRefused);
bookingModule.onBookingRejectedIntegration(giftCardModule::onBookingRejected);

giftCardModule.onTopUpRequested(paymentModule::requestPayment);
bookingModule.onBookingPlaced(paymentModule::requestPayment);
bookingModule.onBookingRefused(paymentModule::requestRefund);
```

Regole AFF aggiunte: `modulesMustNotExposeIntegrationHandlers` (nessun metodo pubblico di un modulo restituisce tipi da `..application.integration.handlers..`) e `compositionRootMustNotDependOnIntegrationAdapters` (`Application` non dipende dai package `integration` interni dei moduli downstream).

> I flussi `BookingRefused` e `BookingRejected` possono restare distinti o essere unificati in un unico `onBookingResult(Consumer<BookingResultIntegrationEvent>)`, a seconda di quanto si vuole rendere generico il contratto pubblico di `giftcard`.

**Motivazione**

- Superficie pubblica minima: i moduli espongono operazioni semantiche, non oggetti implementativi.
- Rinominare, scomporre o sostituire uno handler o un adapter interno non impatta `Application`.
- L'ACL resta dentro il modulo downstream invece di essere invocato dal composition root.
- Wiring esplicito preservato, coerente con i metodi `onTopUpRequested`/`onBookingPlaced`/`onBookingResult` già presenti.

**Alternative considerate**

- *Mantenere handler e adapter pubblici usati dal composition root*: è il problema stesso.
- *Event bus globale condiviso tra i moduli*: accoppia i BC sul bus e sui tipi pubblicati e rende il wiring meno esplicito.
