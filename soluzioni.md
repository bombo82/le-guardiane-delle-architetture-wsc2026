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

## 3. Cosa non è stato ancora risolto (e perché)

Le seguenti violazioni rimangono attive sul branch `solutions` perché richiedono scelte architetturali più ampie o sono volute per il percorso didattico.

### 3.1 API → Infrastructure

`BookingApi` e `GiftCardApi` dipendono direttamente dai rispettivi repository SQLite.

**Perché non è stato risolto:** l'introduzione di un layer `application/query` (o di un read model) richiede di modellare esplicitamente i DTO di lettura e di decidere se adottare CQRS. È un punto centrale del workshop che viene lasciato ai partecipanti.

**Direzione:** o si introduce un `BookingQueryService`/`GiftCardQueryService` tra API e repository, oppure si adotta un read model CQRS in cui la query legge direttamente una rappresentazione ottimizzata.

### 3.2 Cross-Bounded Context dependencies

`booking` e `giftcard` dipendono direttamente da tipi di `payment` e, parzialmente, tra loro.

**Perché non è stato risolto:** il disegno dei confini tra BC è l'argomento più ampio del workshop. Richiede di introdurre pattern di integrazione come:

- eventi di integrazione (separati dagli eventi interni);
- API esposte pubblicamente da ciascun BC;
- anti-corruption layer per isolare i modelli esterni.

Questo punto viene approfondito nelle soluzioni finali del workshop.

---

## 4. Evoluzioni future

Il branch `feature/usecase-aff-rule` contiene un'ulteriore evoluzione: una regola `useCasesMustImplementUseCase` che verifica che tutte le classi in `application/usecases` implementino l'interfaccia `UseCase`. Quella regola rileva che `PaymentExpiring`, `TransactionAccepting` e `TransactionRejecting` sono in realtà `EventSubscriber`, non use case: apre il discorso su dove collocare gli orchestratori event-driven e su come distinguere use case, servizi applicativi e event handler. Il tema verrà affrontato in un momento successivo del workshop.
