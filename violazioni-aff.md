# Schema riassuntivo delle violazioni architetturali rilevate dalle AFF

> Schema generato a partire dall'esecuzione dei test di Architectural Fitness Functions su Java (ArchUnit) e TypeScript (archunit-ts).
> Le violazioni elencate sono quelle **attualmente presenti e intenzionali** nel codice di partenza del workshop.

---

## Sintesi esecutiva

| Implementazione | Test AFF totali | Falliti | Passati |
|---|---|---|---|
| Java | 33 | **4** | 29 |
| TypeScript | 34 | **4** | 30 |

Le violazioni si concentrano in **tre aree**:

1. **Layer interni esagonali** — l'`api` dipende direttamente dall'`infrastructure` (handler → repository concreto).
2. **Shape rules** — nessuna violazione attiva; `PaymentCharging` è stato ricollocato in `application/services` e tutte le policy implementano `Policy`.
3. **Confini tra Bounded Context** — `booking` e `giftcard` dipendono direttamente da altri BC.

---

## Mappa delle violazioni per area

### 1. Regole esagonali interne ai BC

| BC | Regola AFF | Stato | Dettaglio |
|---|---|---|---|
| `booking` | `domain` non dipende da outer layers | ✅ OK | Le policy sono state spostate in `application/policies`. |
| `booking` | `api` non dipende da `infrastructure` | ❌ Fallita | `BookingApi` dipende direttamente da `SqliteBookingRepository`. |
| `booking` | `application` non dipende da adapter | ✅ OK | — |
| `booking` | `infrastructure` non dipende da `api` | ✅ OK | — |
| `giftcard` | `domain` non dipende da outer layers | ✅ OK | Le policy sono state spostate in `application/policies`. |
| `giftcard` | `api` non dipende da `infrastructure` | ❌ Fallita | `GiftCardApi` dipende direttamente da `SqliteGiftCardRepository`. |
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
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│                     SHAPE RULES                                 │
├─────────────────────────────────────────────────────────────────┤
│  Policy in application/policies implementano Policy     ✅      │
│  PaymentCharging ricollocato in application/services    ✅      │
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
| API → Infrastructure | L'adapter di ingresso conosce l'adapter di uscita, bypassando la porta. | Impossibile sostituire il repository senza toccare l'handler HTTP. |
| Cross-BC dependencies | I BC non sono più isolati; un cambiamento in `payment` si propaga in `booking` e `giftcard`. | Rottura del confine modulare; difficile estrarre un BC in micro-servizio. |

---

## Note

- I test AFF che rilevano queste violazioni sono **attivi** (non `@Disabled`) sia in Java che in TypeScript.
- Le violazioni rimanenti sono quelle presenti nel branch `solutions` a valle dello spostamento delle policy in `application`.
- Il branch `feature/usecase-aff-rule` contiene invece l'evoluzione con la regola `useCasesMustImplementUseCase`, da approfondire in un momento successivo del workshop.
