# Schema riassuntivo delle violazioni architetturali rilevate dalle AFF

> Schema generato a partire dall'esecuzione dei test di Architectural Fitness Functions su Java (ArchUnit) e TypeScript (archunit-ts).
> Le violazioni elencate sono quelle **attualmente presenti e intenzionali** nel codice di partenza del workshop.

---

## Sintesi esecutiva

| Implementazione | Test AFF totali | Falliti | Passati |
|---|---|---|---|
| Java | 33 | **9** | 24 |
| TypeScript | 34 | **9** | 25 |

Le violazioni si concentrano in **tre aree**:

1. **Layer interni esagonali** — il layer `domain` vede il layer `application` (policy → command) e l'`api` vede l'`infrastructure` (handler → repository concreto).
2. **Shape rules** — una policy nel BC `payment` non implementa l'interfaccia `Policy`.
3. **Confini tra Bounded Context** — `booking` e `giftcard` dipendono direttamente da altri BC.

---

## Mappa delle violazioni per area

### 1. Regole esagonali interne ai BC

| BC | Regola AFF | Stato | Dettaglio |
|---|---|---|---|
| `booking` | `domain` non dipende da outer layers | ❌ Fallita | `PaymentPolicy` (domain) dipende da `BookingConfirmationCommands` (application). |
| `booking` | `api` non dipende da `infrastructure` | ❌ Fallita | `BookingApi` dipende direttamente da `SqliteBookingRepository`. |
| `booking` | `application` non dipende da adapter | ✅ OK | — |
| `booking` | `infrastructure` non dipende da `api` | ✅ OK | — |
| `giftcard` | `domain` non dipende da outer layers | ❌ Fallita | Policy in `domain/policies` dipendono da command in `application/commands`. |
| `giftcard` | `api` non dipende da `infrastructure` | ❌ Fallita | `GiftCardApi` dipende direttamente da `SqliteGiftCardRepository`. |
| `giftcard` | `application` non dipende da adapter | ✅ OK | — |
| `giftcard` | `infrastructure` non dipende da `api` | ✅ OK | — |
| `payment` | `domain` non dipende da outer layers | ❌ Fallita | Policy in `domain/policies` dipendono da command in `application/commands`. |
| `payment` | `api` non dipende da `infrastructure` | ✅ OK | — |
| `payment` | `application` non dipende da adapter | ✅ OK | — |
| `payment` | `infrastructure` non dipende da `api` | ✅ OK | — |
| `common` | `domain` non dipende da `application`/`module` | ❌ Fallita | `Policy` (common.domain.model) dipende da `Command` (common.application). |

#### Pattern ricorrente: domain → application

La violazione più diffusa all'interno dei BC (e di `common`) è che il layer `domain` dipende dal layer `application`.

##### Nei BC: policy del domain che producono command dell'application

```text
domain.policies.*Policy
  → application.commands.*Command
```

Esempi concreti:

- `booking.domain.policies.PaymentPolicy` → `booking.application.commands.BookingConfirmationCommands`
- `giftcard.domain.policies.ConfirmTopUpPolicy` → `giftcard.application.commands.ConfirmTopUp`
- `giftcard.domain.policies.CreditGiftCardPolicy` → `giftcard.application.commands.CreditGiftCard`
- `giftcard.domain.policies.RefundGiftCardPolicy` → `giftcard.application.commands.RefundGiftCard`
- `payment.domain.policies.PaymentCompletion` → `payment.application.commands.AcceptTransaction`
- `payment.domain.policies.PaymentExpiration` → `payment.application.commands.ExpirePayment`
- `payment.domain.policies.PaymentRejection` → `payment.application.commands.RejectTransaction`
- `payment.domain.policies.TransactionRefund` → `payment.application.commands.RefundTransaction`

> **Nota didattica — dove collocare le policy?**  
> Le policy non sono logica di dominio pura: sono **orchestrazione reattiva** che, ricevuto un evento, decide quale comando eseguire. Concettualmente appartengono quindi al layer `application`, anche quando fisicamente risiedono nel package `domain.policies`. Collocarle in `application.policies` (o in `application.services` se agiscono da event handler) risolve la dipendenza verso i command e mantiene il domain isolato dagli use case.

##### In `common`: `Policy` del domain che dipende da `Command` dell'application

```text
common.domain.model.Policy<C extends Command>
  → common.application.Command
```

#### Pattern: API che bypassa il layer application/query

```text
booking.api.BookingApi
  → booking.infrastructure.SqliteBookingRepository

giftcard.api.GiftCardApi
  → giftcard.infrastructure.SqliteGiftCardRepository
```

> **Nota didattica — perché non bypassare il layer application/query?**  
> L'`api` non dovrebbe dipendere direttamente dall'`infrastructure`. Tra i due deve esistere il layer `application/query` (o un read model) che espone i dati nel formato richiesto dall'API. Questo strato intermedio disaccoppia la rappresentazione interna del dominio (entità, aggregate, value object) dalla rappresentazione pubblica esposta tramite API, permettendo di far evolvere indipendentemente gli internals e il contratto HTTP.
>
> Se l'architettura interna fosse **CQRS** con un **read model** dedicato, il bypass del layer applicativo diventerebbe accettabile: in quel caso la rappresentazione dei dati nel database è già ottimizzata e strettamente dipendente dalla query che si sta servendo, quindi l'API può leggere direttamente dal repository/read store senza perdere disaccoppiamento.

---

### 2. Shape rules

| BC | Regola AFF | Stato | Dettaglio |
|---|---|---|---|
| `payment` | Tutte le policy concrete implementano `Policy` | ❌ Fallita | `payment.domain.policies.PaymentCharging` non implementa `Policy`. |
| `payment` | Tutti i command implementano `Command` | ✅ OK | — |
| `booking` / `giftcard` | Shape rules analoghe | ✅ OK | — |

> **Nota didattica — `PaymentCharging` e lo scope del BC `payment`**  
> `PaymentCharging` non implementa `Policy` perché non restituisce un `Command`: riceve `TransactionStarted` e restituisce `PaymentProviderResult`, interagendo direttamente con il provider esterno. Nel contesto del workshop il BC `payment` è parzialmente out-of-scope: gli adapter verso i payment provider reali (PayPal, Klarna, ecc.) non sono ancora integrati. Questa violazione va quindi rivalutata quando tali adapter verranno creati e il ruolo di `PaymentCharging` sarà chiaramente quello di adapter/processor piuttosto che di policy di dominio.

---

### 3. Cross-Bounded Context dependencies

> **Nota didattica**  
> Il disegno dei confini tra Bounded Context è l'argomento più ampio e complesso del workshop. Le dipendenze rilevate qui saranno approfondite e affrontate nelle soluzioni, insieme ai pattern di integrazione (eventi, API esposte, anti-corruption layer) che permettono di eliminarle.

| Regola AFF | Stato | BC coinvolto | Dettaglio |
|---|---|---|---|
| `booking` non deve dipendere da `giftcard` o `payment` | ❌ Fallita | `booking` | 52 violazioni rilevate; 77 riferimenti a tipi di altri BC (ogni riga può contenere più FQN). |
| `giftcard` non deve dipendere da `booking` o `payment` | ❌ Fallita | `giftcard` | 21 violazioni rilevate; 40 riferimenti a tipi di altri BC. |
| `payment` non deve dipendere da altri BC | ✅ OK | `payment` | Nessuna dipendenza verso `booking` o `giftcard`. |
| `common` non deve dipendere dai BC | ✅ OK | `common` | Nessuna dipendenza verso i bounded context. |

#### Dettaglio per tipo importato (con conteggio occorrenze)

> I conteggi sono calcolati sul report ArchUnit Java; TypeScript ha le stesse classi/tipi con i nomi camelCase.

##### `booking` → `giftcard`

| Tipo importato | Occorrenze |
|---|---|
| `giftcard.domain.giftcard.GiftCardId` | 43 |

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

##### `giftcard` → `booking`

| Tipo importato | Occorrenze |
|---|---|
| `booking.domain.events.BookingResultEvents` | 18 |
| `booking.domain.events.BookingResultEvents$BookingConfirmed` | 2 |
| `booking.domain.events.BookingResultEvents$BookingRefused` | 2 |
| `booking.domain.events.BookingResultEvents$BookingRejected` | 2 |

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
  → giftcard.domain.giftcard.GiftCardId        (43 occorrenze)
  → payment.domain.events.PaymentResultEvents  (12 occorrenze)
  → payment.application.commands.RequestPayment (4 occorrenze)
  → payment.application.commands.RefundTransaction (4 occorrenze)
  → payment.domain.ports.PaymentRepository     (4 occorrenze)
  → payment.domain.payment.PaymentId           (3 occorrenze)
  → payment.domain.payment.Payment             (1 occorrenza)

giftcard
  → booking.domain.events.BookingResultEvents  (18 + 6 occorrenze sui sottotipi)
  → payment.domain.events.PaymentResultEvents  (8 + 2 occorrenze sul sottotipo)
  → payment.application.commands.RequestPayment (4 occorrenze)
  → payment.domain.payment.PaymentId           (2 occorrenze)
```

#### File classi / moduli principali coinvolti

- `booking.domain.policies.BookingPaymentRequestPolicy`
- `booking.domain.policies.BookingRefundRequestPolicy`
- `booking.domain.policies.PaymentPolicy`
- `booking.application.commands.PlaceBooking`
- `booking.application.commands.BookingConfirmationCommands`
- `booking.domain.booking.Booking` (campo `giftCardId`)
- `booking.infrastructure.SqliteBookingRepository`
- `giftcard.domain.policies.ConfirmTopUpPolicy`
- `giftcard.domain.policies.CreditGiftCardPolicy`
- `giftcard.domain.policies.RefundGiftCardPolicy`
- `giftcard.domain.policies.TopUpPaymentRequestPolicy`
- `giftcard.application.services.BookingResultCrediting`
- `giftcard.application.services.BookingResultRefunding`
- `giftcard.application.services.TopUpConfirmation`

---

## Rappresentazione grafica ad alto livello

```text
┌─────────────────────────────────────────────────────────────────┐
│              VIOLAZIONI LAYER INTERNE (per BC)                  │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│   API ──────────────┐                                           │
│   │                 │  (bypass applicazione/query)              │
│   ▼                 ▼                                           │
│   Infra ◄──X── API  booking, giftcard                           │
│                                                                 │
│   Domain ───────────┐                                           │
│   │                 │  (policy producono command)               │
│   ▼                 ▼                                           │
│   Application ◄──X── Domain  booking, giftcard, payment         │
│                                                                 │
│   Common.Domain ────┐                                           │
│   │                 │  (Policy dipende da Command)              │
│   ▼                 ▼                                           │
│   Common.Application ◄──X── Common.Domain                       │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│                     VIOLAZIONI SHAPE RULES                      │
├─────────────────────────────────────────────────────────────────┤
│  payment.domain.policies.PaymentCharging                        │
│    non implementa common.domain.model.Policy        ❌          │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│                     VIOLAZIONI CROSS-BC                         │
├─────────────────────────────────────────────────────────────────┤
│  booking ──────────────┐                                        │
│  giftcard ─────────────┼──► payment (eventi, command, port)     │
│                        │                                        │
│  booking ──────────────┼──► giftcard (GiftCardId)               │
│  giftcard ─────────────┘──► booking (eventi)                    │
│                                                                 │
│  payment  ───► nessuna dipendenza verso altri BC  ✅            │
└─────────────────────────────────────────────────────────────────┘
```

---

## Impatto architetturale

| Violazione | Perché è un problema | Effetto sul refactoring |
|---|---|---|
| Domain → Application | Il cuore del dominio conosce i dettagli dei casi d'uso, invertendo la dipendenza esagonale. | Il dominio diventa fragile al variare degli use case. |
| API → Infrastructure | L'adapter di ingresso conosce l'adapter di uscita, bypassando la porta. | Impossibile sostituire il repository senza toccare l'handler HTTP. |
| Common.Domain → Common.Application | L'astrazione condivisa più interna dipende da un concetto del layer superiore. | Il common non può più essere riutilizzato senza trascinare `Command`. |
| `PaymentCharging` non implementa `Policy` | Inconsistenza nel modello policy; alcune classi sfuggono al contratto. | Regole generiche sulle policy non si applicano a tutte le istanze. |
| Cross-BC dependencies | I BC non sono più isolati; un cambiamento in `payment` si propaga in `booking` e `giftcard`. | Rottura del confine modulare; difficile estrarre un BC in micro-servizio. |

---

## Note

- I test AFF che rilevano queste violazioni sono **attivi** (non `@Disabled`) sia in Java che in TypeScript.
- Le violazioni sono **deliberatamente** presenti nel branch `main` come materiale del workshop.
- Per il branch di riferimento con le violazioni risolte, consultare `solution` (solo post-esercizio).
