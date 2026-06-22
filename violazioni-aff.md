# Schema riassuntivo delle violazioni architetturali rilevate dalle AFF

> Schema generato a partire dall'esecuzione dei test di Architectural Fitness Functions su Java (ArchUnit) e TypeScript (archunit-ts).
> Le violazioni elencate sono quelle **attualmente presenti e intenzionali** nel codice di partenza del workshop.

---

## Sintesi esecutiva

| Implementazione | Test AFF totali | Falliti | Passati |
|---|---|---|---|
| Java | 34 | **2** | 32 |
| TypeScript | 37 | **2** | 35 |

> I test AFF totali includono: cross-BC dependencies, Published Language / ACL, hexagonal architecture, shape rules e domain/application purity per ciascun bounded context.

Le violazioni rimanenti si concentrano in **un'unica area**:

1. **Confini tra Bounded Context** — `booking` e `giftcard` dipendono ancora direttamente da `payment`.

La relazione `booking ↔ giftcard` è stata disaccoppiata tramite **Published Language** e **Anti-Corruption Layer**.

Layer interni esagonali e shape rules sono ora **tutti a posto**.

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
| `booking` non deve dipendere da `payment` | ❌ Fallita | `booking` | Dipendenze da eventi, command e porta di `payment`. |
| `giftcard` non deve dipendere da `booking` | ✅ Risolta | `giftcard` | `giftcard` consuma ora la Published Language `booking.integration.giftcard` attraverso l'ACL `giftcard.application.integration.booking.adapter.BookingResult`. |
| `giftcard` non deve dipendere da `payment` | ❌ Fallita | `giftcard` | Dipendenze da eventi e command di `payment`. |
| `payment` non deve dipendere da altri BC | ✅ OK | `payment` | Nessuna dipendenza verso `booking` o `giftcard`. |
| `common` non deve dipendere dai BC | ✅ OK | `common` | Nessuna dipendenza verso i bounded context. |

#### Dettaglio per tipo importato (con conteggio occorrenze)

> I conteggi sono calcolati sul report ArchUnit Java; TypeScript ha le stesse classi/tipi con i nomi camelCase.

> **Nota didattica — dipendenza `booking → giftcard` risolta**  
> L'import di `giftcard.domain.giftcard.GiftCardId` in `booking` è stato rimosso. `Booking` e i suoi command ora usano `booking.domain.primitive.GiftCardReference`; gli eventi di dominio di `booking` espongono il riferimento come primitiva (`UUID`/`String`), che `giftcard` traduce nel proprio `GiftCardId` solo al proprio confine.

> **Nota didattica — dipendenza `giftcard → booking` risolta con PL + ACL**  
> `giftcard` non importa più `booking.domain.events.BookingResultEvents`. `booking` espone una Published Language dedicata in `booking.integration.giftcard` (`BookingResultIntegrationEvent` e i suoi sottotipi). `giftcard` accoglie questi eventi in un Anti-Corruption Layer (`giftcard.application.integration.booking.adapter.BookingResult`) che li traduce nei command interni `CreditGiftCard` e `RefundGiftCard`. Questo è lo stesso pattern che verrà applicato a `payment.integration` per risolvere le rimanenti violazioni.

##### `booking` → `payment`

| Tipo importato | Occorrenze |
|---|---|
| `payment.domain.events.PaymentResultEvents` | 12 |
| `payment.application.commands.RefundTransaction` | 4 |
| `payment.application.commands.RequestPayment` | 4 |
| `payment.domain.ports.PaymentRepository` | 4 |
| `payment.domain.events.PaymentResultEvents$PaymentAccepted` | 3 |
| `payment.domain.events.PaymentResultEvents$PaymentRejected` | 3 |
| `payment.domain.payment.PaymentId` | 3 |
| `payment.domain.payment.Payment` | 1 |

##### `giftcard` → `payment`

| Tipo importato | Occorrenze |
|---|---|
| `payment.domain.events.PaymentResultEvents` | 8 |
| `payment.application.commands.RequestPayment` | 4 |
| `payment.domain.events.PaymentResultEvents$PaymentAccepted` | 2 |
| `payment.domain.payment.PaymentId` | 2 |

#### Esempi di dipendenze illecite (Java / TS equivalente)

```text
booking
  → payment.domain.events.PaymentResultEvents  (12 occorrenze)
  → payment.application.commands.RequestPayment (4 occorrenze)
  → payment.application.commands.RefundTransaction (4 occorrenze)
  → payment.domain.ports.PaymentRepository     (4 occorrenze)
  → payment.domain.payment.PaymentId           (3 occorrenze)
  → payment.domain.payment.Payment             (1 occorrenza)

giftcard
  → payment.domain.events.PaymentResultEvents  (8 + 2 occorrenze sul sottotipo)
  → payment.application.commands.RequestPayment (4 occorrenze)
  → payment.domain.payment.PaymentId           (2 occorrenze)
```

#### File classi / moduli principali coinvolti

- `booking.application.policies.BookingPaymentRequestPolicy`
- `booking.application.policies.BookingRefundRequestPolicy`
- `booking.application.policies.PaymentPolicy`
- `giftcard.application.policies.ConfirmTopUpPolicy`
- `giftcard.application.policies.TopUpPaymentRequestPolicy`
- `giftcard.application.services.TopUpConfirmation`

#### File del nuovo confine `booking → giftcard`

```text
booking.integration.giftcard.BookingResultIntegrationEvent
  → booking.publish(BookingConfirmed / BookingRefused / BookingRejected)
  → giftcard.application.integration.booking.adapter.BookingResult
    → CreditGiftCard / RefundGiftCard
  → giftcard.application.integration.booking.handlers.CreditFromBooking / RefundFromBooking
```

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
│   booking ────────────────► payment (eventi, command, port)     │
│   giftcard ───────────────► payment (eventi, command)           │
│                              ❌ Da risolvere                    │
│                                                                 │
│   payment  ───► nessuna dipendenza verso altri BC  ✅           │
└─────────────────────────────────────────────────────────────────┘
```

---

## Impatto architetturale

| Violazione | Perché è un problema | Effetto sul refactoring |
|---|---|---|
| Cross-BC dependencies | I BC non sono più isolati; un cambiamento in `payment` si propaga in `booking` e `giftcard`. | Rottura del confine modulare; difficile estrarre un BC in micro-servizio. |

---

## Note

- I test AFF che rilevano queste violazioni sono **attivi** (non `@Disabled`) sia in Java che in TypeScript.
- Le violazioni rimanenti nel branch `solutions` sono solo le dipendenze cross-BC.
- Il branch `feature/usecase-aff-rule` contiene invece l'evoluzione con la regola `useCasesMustImplementUseCase`, da approfondire in un momento successivo del workshop.
