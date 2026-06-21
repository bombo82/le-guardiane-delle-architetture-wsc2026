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
