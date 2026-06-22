# Schema riassuntivo delle violazioni architetturali rilevate dalle AFF

> Schema generato a partire dall'esecuzione dei test di Architectural Fitness Functions su Java (ArchUnit) e TypeScript (archunit-ts).
> Le violazioni elencate sono quelle **attualmente presenti e intenzionali** nel codice di partenza del workshop.

---

## Sintesi esecutiva

| Implementazione | Test AFF totali | Falliti | Passati |
|---|---|---|---|
| Java | 43 | **0** | 43 |
| TypeScript | 44 | **0** | 44 |

> I numeri includono le regole ArchUnit/TS su cross-BC dependencies, Published Language / ACL, hexagonal architecture, shape rules e domain/application purity per ciascun bounded context.

Tutte le relazioni cross-BC sono state disaccoppiate tramite **Published Language** e **Anti-Corruption Layer**:

- `booking → giftcard` (risultati prenotazione);
- `payment → giftcard` e `payment → booking` (esiti pagamento);
- `booking → payment` e `giftcard → payment` (richiesta pagamento / rimborso).

Layer interni esagonali e shape rules sono **tutti a posto**.

> ✅ La violazione relativa al **Composition Root** è stata risolta in entrambe le implementazioni: `Application` dipende ora solo dalla superficie pubblica dei moduli. Vedi sezione 4.

---

## Mappa delle violazioni per area

### 1. Regole esagonali interne ai BC

| BC | Regola AFF | Stato | Dettaglio |
|---|---|---|---|
| `booking` | `domain` non dipende da outer layers | ✅ OK | Le policy sono state spostate in `application/policies`. |
| `booking` | `api` non dipende da `infrastructure` | ✅ OK | `BookingApi` usa `BookingQueryService` in `application/query`. |
| `booking` | `application` non dipende da adapter | ✅ OK | — |
| `booking` | `infrastructure` non dipende da `api` | ✅ OK | — |
| `giftcard` | `domain` non dipende da outer layers | ✅ OK | Le policy sono state spostate in `application/policies`. |
| `giftcard` | `api` non dipende da `infrastructure` | ✅ OK | `GiftCardApi` usa `GiftCardQueryService` in `application/query`. |
| `giftcard` | `application` non dipende da adapter | ✅ OK | — |
| `giftcard` | `infrastructure` non dipende da `api` | ✅ OK | — |
| `payment` | `domain` non dipende da outer layers | ✅ OK | Le policy sono state spostate in `application/policies`. |
| `payment` | `api` non dipende da `infrastructure` | ✅ OK | — |
| `payment` | `application` non dipende da adapter | ✅ OK | — |
| `payment` | `infrastructure` non dipende da `api` | ✅ OK | — |
| `common` | `domain` non dipende da `application`/`module` | ✅ OK | L'interfaccia `Policy` è stata spostata in `common.application`. |

> **Nota didattica — dove collocare le policy?**  
> Le policy non sono logica di dominio pura: sono **orchestrazione reattiva** che, ricevuto un evento, decide quale comando eseguire. Concettualmente appartengono quindi al layer `application`. Nel corso della soluzione:
> - le policy di ciascun BC sono state spostate da `domain.policies` ad `application.policies`;
> - l'interfaccia `Policy` è stata spostata da `common.domain.model` a `common.application`;
> - `PaymentCharging`, che non restituiva un command ma invocava direttamente il provider, è stato ricollocato in `payment.application.services`.

#### Pattern risolto: query layer tra API e infrastructure

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

> **Nota didattica — perché un layer `application/query`?**  
> L'`api` non dovrebbe dipendere direttamente dall'`infrastructure`. Tra i due è stato introdotto il layer `application/query`, composto da query service e read model (`BookingDetails`, `GiftCardDetails`), che espone i dati nel formato richiesto dall'API. Il query service dipende dalla porta del repository (`domain.ports`), non dall'implementazione concreta SQLite. Questo strato intermedio disaccoppia la rappresentazione interna del dominio dalla rappresentazione pubblica esposta tramite API.
>
> I DTO di query riutilizzano i value object di dominio (`Description`, `Money`) e tipi semplici (`UUID`, `String`), quindi rimangono all'interno delle regole di purity del layer `application`: non è necessaria alcuna esclusione specifica per `application.query`.
>
> Se l'architettura interna fosse **CQRS** con un **read model** dedicato, il bypass del layer applicativo diventerebbe accettabile: in quel caso la rappresentazione dei dati nel database è già ottimizzata e strettamente dipendente dalla query che si sta servendo, quindi l'API può leggere direttamente dal repository/read store senza perdere disaccoppiamento.

---

### 2. Shape rules

| BC | Regola AFF | Stato | Dettaglio |
|---|---|---|---|
| `payment` | Tutte le policy concrete implementano `Policy` | ✅ OK | Le policy sono in `application/policies` e implementano `Policy`; `PaymentCharging` è stato ricollocato in `application/services`. |
| `payment` | Tutti i command implementano `Command` | ✅ OK | — |
| `booking` / `giftcard` | Shape rules analoghe | ✅ OK | — |

> **Nota didattica — `PaymentCharging` e lo scope del BC `payment`**  
> `PaymentCharging` non implementava `Policy` perché non restituisce un `Command`: riceve `TransactionStarted` e restituisce `PaymentProviderResult`, interagendo direttamente con il provider esterno. È stato quindi ricollocato in `payment.application.services`, fuori dal package delle policy. Nel contesto del workshop il BC `payment` è parzialmente out-of-scope: gli adapter verso i payment provider reali (PayPal, Klarna, ecc.) non sono ancora integrati. Il ruolo di `PaymentCharging` andrà rivalutato quando tali adapter verranno creati.

---

### 3. Cross-Bounded Context dependencies

> **Nota didattica**  
> Il disegno dei confini tra Bounded Context è l'argomento più ampio e complesso del workshop. Le dipendenze rilevate qui saranno approfondite e affrontate nelle soluzioni, insieme ai pattern di integrazione (eventi, API esposte, anti-corruption layer) che permettono di eliminarle.

| Regola AFF | Stato | BC coinvolto | Dettaglio |
|---|---|---|---|
| `booking` non deve dipendere da `giftcard` | ✅ Risolta | `booking` | La dipendenza è stata eliminata con `GiftCardReference`; gli eventi di dominio di `booking` espongono il riferimento come primitiva. |
| `booking` non deve dipendere da `payment` | ✅ Risolta | `booking` | Il flusso **eventi di risultato** (`payment → booking`) è stato disaccoppiato con `payment.integration.PaymentResultIntegrationEvent` e ACL `booking.application.integration.payment.adapter.PaymentResult`. Il flusso **command** (`booking → payment`) è stato disaccoppiato con `payment.integration.PaymentRequestIntegrationCommand` / `RefundRequestIntegrationCommand` e ACL `booking.application.integration.payment.adapter.PaymentRequest` / `RefundRequest`. |
| `giftcard` non deve dipendere da `booking` | ✅ Risolta | `giftcard` | `giftcard` consuma ora la Published Language `booking.integration.giftcard` attraverso l'ACL `giftcard.application.integration.booking.adapter.BookingResult`. |
| `giftcard` non deve dipendere da `payment` | ✅ Risolta | `giftcard` | Il flusso **eventi di risultato** (`payment → giftcard`) è stato disaccoppiato con `payment.integration.PaymentResultIntegrationEvent` e ACL `giftcard.application.integration.payment.adapter.PaymentResult`. Il flusso **command** (`giftcard → payment`) è stato disaccoppiato con `payment.integration.PaymentRequestIntegrationCommand` e ACL `giftcard.application.integration.payment.adapter.PaymentRequest`. |
| `payment → giftcard` eventi di risultato | ✅ Risolta | `payment` / `giftcard` | `payment` espone `PaymentResultIntegrationEvent`; `giftcard` lo traduce in `ConfirmTopUp` tramite ACL. |
| `payment → booking` eventi di risultato | ✅ Risolta | `payment` / `booking` | `payment` espone `PaymentResultIntegrationEvent`; `booking` lo traduce in `ConfirmBooking`/`RejectBooking` tramite ACL. |
| `payment` non deve dipendere da altri BC | ✅ OK | `payment` | Nessuna dipendenza verso `booking` o `giftcard`. |
| `common` non deve dipendere dai BC | ✅ OK | `common` | Nessuna dipendenza verso i bounded context. |

#### Dettaglio per tipo importato (con conteggio occorrenze)

> I conteggi sono calcolati sul report ArchUnit Java; TypeScript ha le stesse classi/tipi con i nomi camelCase.

> **Nota didattica — dipendenza `booking → giftcard` risolta**  
> L'import di `giftcard.domain.giftcard.GiftCardId` in `booking` è stato rimosso. `Booking` e i suoi command ora usano `booking.domain.primitive.GiftCardReference`; gli eventi di dominio di `booking` espongono il riferimento come primitiva (`UUID`/`String`), che `giftcard` traduce nel proprio `GiftCardId` solo al proprio confine.

> **Nota didattica — dipendenza `giftcard → booking` risolta con PL + ACL**  
> `giftcard` non importa più `booking.domain.events.BookingResultEvents`. `booking` espone una Published Language dedicata in `booking.integration.giftcard` (`BookingResultIntegrationEvent` e i suoi sottotipi). `giftcard` accoglie questi eventi in un Anti-Corruption Layer (`giftcard.application.integration.booking.adapter.BookingResult`) che li traduce nei command interni `CreditGiftCard` e `RefundGiftCard`.
>
> **Nota didattica — dipendenze `booking → payment` e `giftcard → payment` risolte con PL + ACL**  
> `booking` e `giftcard` non importano più command o eventi interni di `payment`. `payment` espone una Published Language in `payment.integration`:
> - `PaymentResultIntegrationEvent` per gli esiti di pagamento (già consumato tramite ACL dagli eventi);
> - `PaymentRequestIntegrationCommand` e `RefundRequestIntegrationCommand` per le richieste di pagamento/rimborso.
> I BC downstream traducono i propri eventi di dominio nei command di integrazione attraverso ACL dedicati:
> - `booking.application.integration.payment.adapter.PaymentRequest` e `RefundRequest`;
> - `giftcard.application.integration.payment.adapter.PaymentRequest`.
> Il Composition Root (`Application`) riceve i command di integrazione e li invia a `PaymentModule.requestPayment(...)` / `requestRefund(...)`.

#### File del nuovo confine `booking → giftcard`

```text
booking.integration.giftcard.BookingResultIntegrationEvent
  → booking.publish(BookingConfirmed / BookingRefused / BookingRejected)
  → giftcard.application.integration.booking.adapter.BookingResult
    → CreditGiftCard / RefundGiftCard
  → giftcard.application.integration.booking.handlers.CreditFromBooking / RefundFromBooking
```

#### File del nuovo confine `booking / giftcard → payment` (command)

```text
giftcard.domain.events.GiftCardTopUpRequested
  → giftcard.application.integration.payment.adapter.PaymentRequest
    → payment.integration.PaymentRequestIntegrationCommand
      → PaymentModule.requestPayment(...)

booking.domain.events.BookingPlaced
  → booking.application.integration.payment.adapter.PaymentRequest
    → payment.integration.PaymentRequestIntegrationCommand
      → PaymentModule.requestPayment(...)

booking.domain.events.BookingResultEvents.BookingRefused
  → booking.application.integration.payment.adapter.RefundRequest
    → payment.integration.RefundRequestIntegrationCommand
      → PaymentModule.requestRefund(...)
```

---

## 4. Composition root e incapsulamento dei moduli

| Regola AFF | Stato | BC coinvolto | Dettaglio |
|---|---|---|---|
| I moduli non devono esporre handler di integrazione nel loro contratto pubblico | ❌ Violata | `booking`, `giftcard` | `Application` accede direttamente agli handler concreti per registrare le reazioni cross-BC. |
| Il composition root non deve dipendere dagli anti-corruption layer interni dei moduli | ❌ Violata | `booking`, `giftcard` | `Application` chiama direttamente gli adapter in `application.integration.*.adapter` per tradurre eventi in command di integrazione. |

### 4.1 Esposizione degli handler

I seguenti metodi pubblici dei moduli espongono tipi appartenenti al package `application.integration.handlers`:

```text
booking.BookingModule.handlePaymentResultFromPayment()
  → booking.application.integration.payment.handlers.HandlePaymentResultFromPayment

giftcard.GiftCardModule.confirmTopUpFromPayment()
  → giftcard.application.integration.payment.handlers.ConfirmTopUpFromPayment

giftcard.GiftCardModule.creditFromBooking()
  → giftcard.application.integration.booking.handlers.CreditFromBooking

giftcard.GiftCardModule.refundFromBooking()
  → giftcard.application.integration.booking.handlers.RefundFromBooking
```

`Application` li consuma così:

```java
paymentModule.onPaymentResult(bookingModule.handlePaymentResultFromPayment()::handle);
paymentModule.onPaymentResult(giftCardModule.confirmTopUpFromPayment()::handle);
bookingModule.onBookingResultIntegration(giftCardModule.creditFromBooking()::handle);
bookingModule.onBookingRejectedIntegration(giftCardModule.refundFromBooking()::handle);
```

### 4.2 Accesso diretto agli adapter di integrazione

`Application` chiama direttamente gli adapter dei moduli downstream per tradurre eventi di dominio in command verso `payment`:

```java
giftCardModule.onTopUpRequested(event ->
    paymentModule.requestPayment(PaymentRequest.fromTopUp(event))
);

bookingModule.onBookingPlaced(event ->
    paymentModule.requestPayment(PaymentRequest.fromBookingPlaced(event))
);

bookingModule.onBookingResult(event -> {
    if (event instanceof BookingResultEvents.BookingRefused refused) {
        paymentModule.requestRefund(RefundRequest.fromBookingRefused(refused));
    }
});
```

Le classi usate appartengono agli ACL dei moduli downstream:

```text
giftcard.application.integration.payment.adapter.PaymentRequest
booking.application.integration.payment.adapter.PaymentRequest
booking.application.integration.payment.adapter.RefundRequest
```

### Nota didattica

Il **Composition Root** dovrebbe essere il punto in cui i moduli vengono collegati, non il punto in cui si conoscono i dettagli interni di ciascun modulo. Esporre handler concreti rompe l’incapsulamento: un refactor interno (rinominare uno handler, sostituirlo con una policy, dividerlo in più passaggi) si propaga in `Application`.

Anche l’accesso agli **adapter di integrazione** dal composition root è problematico: l’ACL appartiene al modulo downstream e dovrebbe restare al suo interno. Se `Application` traduce eventi di dominio in command di integrazione, conosce sia il modello interno di `booking`/`giftcard` sia il contratto di integrazione di `payment`.

La soluzione completa consiste nel far esporre a ciascun modulo metodi di **sottoscrizione semantici** che restituiscano già i command/eventi di integrazione. Ad esempio:

```text
giftcard.GiftCardModule.onTopUpRequested(Consumer<PaymentRequestIntegrationCommand>)
booking.BookingModule.onBookingPlaced(Consumer<PaymentRequestIntegrationCommand>)
booking.BookingModule.onBookingRefused(Consumer<RefundRequestIntegrationCommand>)
```

In questo modo:

- `Application` dipende solo dalla superficie pubblica dei moduli e dalla Published Language di `payment`;
- i moduli restano black box: nascondono handler, adapter e eventi di dominio interni;
- il wiring manuale ed esplicito — principio fondamentale del workshop — viene preservato senza perdere leggibilità.

---

## Rappresentazione grafica ad alto livello

```text
┌─────────────────────────────────────────────────────────────────┐
│              LAYER INTERNI (nessuna violazione)                 │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│   API ──► Query Service ──► Porta Repository ──► Infra          │
│          booking, giftcard                                      │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│                     SHAPE RULES                                 │
├─────────────────────────────────────────────────────────────────┤
│  Policy in application/policies implementano Policy     ✅      │
│  PaymentCharging ricollocato in application/services    ✅      │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│                     CONFINI CROSS-BC                            │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│   booking ──────[PL]──────► giftcard                            │
│   (BookingResultIntegrationEvent)                               │
│                                                                 │
│   giftcard ─────[ACL]──────► booking.integration.giftcard       │
│   (adapter.BookingResult)                                       │
│                              ✅ Risolto                         │
│                                                                 │
│   payment ──────[PL]──────► giftcard                            │
│   (PaymentResultIntegrationEvent)                               │
│                                                                 │
│   giftcard ─────[ACL]──────► payment.integration                │
│   (adapter.PaymentResult → ConfirmTopUp)                        │
│                              ✅ Risolto                         │
│                                                                 │
│   payment ──────[PL]──────► booking                             │
│   (PaymentResultIntegrationEvent)                               │
│                                                                 │
│   booking ──────[ACL]──────► payment.integration                │
│   (adapter.PaymentResult → ConfirmBooking/RejectBooking)        │
│                              ✅ Risolto                         │
│                                                                 │
│   booking ──────[PL]──────► payment (command)                   │
│   (PaymentRequestIntegrationEvent / RefundRequestIntegrationEvent)
│                                                                 │
│   booking ──────[ACL]──────► payment.integration                │
│   (adapter.PaymentRequest / RefundRequest)                      │
│   giftcard ─────[ACL]──────► payment.integration                │
│   (adapter.PaymentRequest)                                      │
│                              ✅ Risolto                         │
│                                                                 │
│   payment  ───► nessuna dipendenza verso altri BC  ✅           │
└─────────────────────────────────────────────────────────────────┘
```

---

## Impatto architetturale

| Pattern | Perché è importante | Effetto sul refactoring |
|---|---|---|
| Published Language + ACL | I BC non sono più isolati; un cambiamento in `payment` si propaga in `booking` e `giftcard`. | Rottura del confine modulare; difficile estrarre un BC in micro-servizio. |
| Incapsulamento del Composition Root | Esporre handler concreti nel contratto pubblico di un modulo lega il composition root ai dettagli implementativi. | Rinominare o sostituire uno handler interno richiede di modificare `Application`. |

Tutte le relazioni cross-BC sono ora protette da PL + ACL. Un refactor interno a `payment` (nomi eventi, struttura aggregate, command interni) non impatta più `booking` o `giftcard`, purché la Published Language in `payment.integration` resti stabile.

---

## Note

- In Java e in TypeScript la violazione sul Composition Root è **stata risolta**: le regole `CompositionRootArchitectureTest` / `compositionRootArchitecture.test.ts` sono ora verdi.
- Non rimangono violazioni architetturali cross-BC nel branch `solutions`.
- Sono state aggiunte regole AFF esplicite per proteggere la Published Language di `payment`, in modo simmetrico a quanto già fatto per `booking`.
- Il branch `feature/usecase-aff-rule` contiene invece l'evoluzione con la regola `useCasesMustImplementUseCase`, da approfondire in un momento successivo del workshop.
