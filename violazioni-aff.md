# Schema riassuntivo delle violazioni architetturali rilevate dalle AFF

> Schema generato a partire dall'esecuzione dei test di Architectural Fitness Functions su Java (ArchUnit) e TypeScript (archunit-ts).
> Le violazioni elencate sono quelle **attualmente presenti e intenzionali** nel codice di partenza del workshop.

---

## Sintesi esecutiva

| Implementazione | Test AFF totali | Falliti | Passati |
|---|---|---|---|
| Java | 46 | **0** | 46 |
| TypeScript | 46 | **0** | 46 |

> I numeri includono le regole ArchUnit/TS su cross-BC dependencies, Published Language / ACL, hexagonal architecture, shape rules e domain/application purity per ciascun bounded context.

Tutte le relazioni cross-BC sono state disaccoppiate tramite **Published Language** e **Anti-Corruption Layer**:

- `booking → giftcard` (risultati prenotazione);
- `payment → giftcard` e `payment → booking` (esiti pagamento);
- `booking → payment` e `giftcard → payment` (richiesta pagamento / rimborso).

Layer interni esagonali e shape rules sono **tutti a posto**.

> ✅ Sono state aggiunte **nuove regole AFF** sulla definizione dei moduli (sezione 5). Tutte le violazioni evidenziate sono state risolte: il campo `watcher` è `final`/`readonly`, le sottoscrizioni interne sono nel costruttore, l'error handler JSON è in `Application` e i moduli non dipendono più direttamente dai tipi di configurazione del framework (`JavalinConfig` / `Express`).

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
| I moduli non devono esporre handler di integrazione nel loro contratto pubblico | ✅ Risolta | `booking`, `giftcard` | I moduli espongono ora metodi di sottoscrizione semantici. |
| Il composition root non deve dipendere dagli anti-corruption layer interni dei moduli | ✅ Risolta | `booking`, `giftcard` | `Application` dipende solo dalla superficie pubblica dei moduli e dalla Published Language di `payment`. |

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

## 5. Definizione dei moduli

| Regola AFF | Stato | BC coinvolto | Dettaglio |
|---|---|---|---|
| I campi dei moduli devono essere `final` / `readonly` | ✅ Risolta | `payment` | `PaymentModule` dichiara ora `watcher` / `_watcher` come `final`/`readonly`, costruito nel costruttore e avviato in `configure()`. |
| I moduli non devono dipendere dai tipi di configurazione del framework | ✅ Risolta | `booking`, `giftcard`, `payment` | I moduli espongono `webApis()` che restituisce una lista di adapter web; ogni controller (`*Api`) implementa `WebApi`; il Composition Root itera sulla lista e chiama `configure(...)`. |
| I metodi pubblici dei moduli non devono restituire tipi dei layer interni | ✅ OK | tutti | Nessun metodo pubblico restituisce handler, use case, repository o altri tipi interni; `webApis()` restituisce solo adapter web del layer `api`. |

### 5.1 Campo `watcher` non final

**Risolto.** In `PaymentModule` il watcher della scadenza pagamenti è ora costruito nel costruttore e avviato in `configure()`:

```java
// Java
private final PaymentDeadlineWatcher watcher;
```

```ts
// TypeScript
private readonly _watcher: PaymentDeadlineWatcher;
```

In questo modo lo stato del modulo è immutabile dopo la costruzione e `stop()` può essere chiamato in sicurezza.

### ✅ 5.2 Dipendenza diretta dal framework

**Risolto.** Il metodo `configure(...)` è stato rimosso dal contratto pubblico del modulo. `ApplicationModule` dichiara un'interfaccia comune `WebApi` (in `common.module`) con il metodo `configure(...)`; ogni controller (`*Api`) implementa direttamente tale interfaccia e il modulo restituisce una lista di adapter web:

```java
// Java
public interface WebApi {
    void configure(JavalinConfig config);
}

public final class BookingApi implements WebApi { /* ... */ }
public final class PaymentApi implements WebApi { /* ... */ }
public final class PaymentInternalApi implements WebApi { /* ... */ }
```

```ts
// TypeScript
export interface WebApi {
  configure(app: Express): void;
}

export class BookingApi implements WebApi { /* ... */ }
export class PaymentApi implements WebApi { /* ... */ }
export class PaymentInternalApi implements WebApi { /* ... */ }
```

Il Composition Root itera sulla lista restituita da ciascun modulo e chiama `configure(...)` su ogni adapter:

```java
// Java
bookingModule.webApis().forEach(api -> api.configure(config));
giftCardModule.webApis().forEach(api -> api.configure(config));
paymentModule.webApis().forEach(api -> api.configure(config));
paymentModule.start();
```

```ts
// TypeScript
this._bookingModule.webApis().forEach((api) => api.configure(app));
this._giftCardModule.webApis().forEach((api) => api.configure(app));
this._paymentModule.webApis().forEach((api) => api.configure(app));
this._paymentModule.start();
```

In questo modo la facciata del modulo non dipende più direttamente da `JavalinConfig` / `Express`; la dipendenza dal framework è spostata sui singoli controller, che il Composition Root configura esplicitamente uno per uno.

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
- Sono state introdotte regole AFF sulla **definizione dei moduli** (`ModuleDefinitionRulesTest` / `moduleDefinitionArchitecture.test.ts`). Tutte le violazioni evidenziate sono state risolte: campo `watcher` `final`/`readonly`, sottoscrizioni interne nel costruttore, error handler JSON in `Application` e dipendenza framework rimossa dalla facciata dei moduli tramite `webApi()` / `webApis()`.
- Sono state aggiunte regole AFF esplicite per proteggere la Published Language di `payment`, in modo simmetrico a quanto già fatto per `booking`.
- Il branch `feature/usecase-aff-rule` contiene invece l'evoluzione con la regola `useCasesMustImplementUseCase`, da approfondire in un momento successivo del workshop.
