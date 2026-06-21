# Note per i Facilitatori
**Workshop "Le Guardiane delle Architetture"**  
*Working Software 2026*

## Note e Suggerimenti per l'Implementazione
- **Materiali necessari**: codebase di esempio pronti con chiara struttura modulare (uno per Java, uno per TypeScript, pre-strutturati con Bounded Context e violazioni intenzionali dei confini; esempio per AFF su architettura interna esagonale), slide, template per le AFF su boundary, lista di tool.
- **Adattamenti**: i tempi sono indicativi e flessibili; la teoria sulla modularità è prioritaria e non va ridotta.
- **Coinvolgimento continuo**: domande durante la teoria e fare in modo che i gruppi condividano progressi a metà della sessione hands-on principale, focalizzandosi sui boundary.

## Specifiche Tecniche e Struttura Progetto

### Struttura del Repository
Il progetto è organizzato come un Monolite Modulare. Ogni Bounded Context è isolato a livello di package/directory e gestisce la propria persistenza.
```
wsc2026/
├── java/                   # Implementazione Java 21+
├── typescript/             # Implementazione TypeScript 5+
├── FACILITATOR_NOTES.md    # Note per la conduzione
├── AGENTS.md               # Istruzioni per gli agent AI che operano sul codice
├── README.md               # Istruzioni generali partecipanti
└── TODO.md                 # Azioni in sospeso per l'evoluzione del workshop
```

### Perché le Migrazioni non sono una complicazione?
Sebbene per un workshop di 3 ore possa sembrare un eccesso, l'uso di **Flyway** e **Drizzle-kit** è consigliato per:
1. **Evolutionary Architecture**: Mostra come il database evolve insieme al codice, un pilastro del software moderno.
2. **Setup Zero-Config**: Entrambi i tool permettono di avere un database pronto all'uso senza che i partecipanti debbano eseguire script SQL manuali.
3. **Boundary Enforcement**: In entrambi i linguaggi, definire lo schema (con **jOOQ** in Java e **Drizzle** in TypeScript) rende i confini tra moduli espliciti. Per interrogare una tabella, il partecipante deve importare i relativi metadata (es. `Tables.GIFT_CARD` o `giftCardSchema`). Se questo import avviene tra moduli diversi, le AFF lo rilevano istantaneamente, cosa che non accadrebbe usando semplici stringhe SQL.

### Architettura Interna (Hands-on 1)
- **Layering**: Spiegare che l'architettura esagonale non è solo "una serie di cartelle", ma un sistema di protezione del dominio.
- **Porte vs Adattatori**: Le porte (interfacce) vivono nel `domain`, gli adattatori (implementazioni) vivono nell'`infrastructure`. Questo è il punto chiave che le AFF devono proteggere: "Nessuna classe nel Domain può importare classi dall'Infrastructure".
- **Naming Convention**: un buon esercizio per le AFF è verificare che gli adattatori e le porte rispettino una convenzione coerente e che risiedano nel layer corretto. Nella codebase di riferimento si è scelto di evitare suffissi tecnici (`Adapter`, `RepositoryImpl`, `Port`, `Impl`, `Abstract`, `Default`) in produzione, per non nascondere il linguaggio di dominio dietro terminologia infrastrutturale. L'unica eccezione didattica accettata è il suffisso `*Service`.

### Violazioni Intenzionali Presenti nella Codebase

Le violazioni seguenti sono **deliberatamente** presenti e costituiscono il materiale dell'esercizio. Non devono essere risolte automaticamente; servono ai partecipanti per scoprire, misurare e correggere con le AFF.

- **Cross-BC imports**: classi di un Bounded Context importano tipi di un altro BC.
  - Esempio: `giftcard.domain.policies.ConfirmTopUpPolicy` importa `payment.domain.events.PaymentResultEvents`.
  - Esempio: `booking.domain.payments.BookingPaymentRequestPolicy` importa comandi ed entità di `payment`.
- **Layer bypass interno al BC**: gli API layer di `booking` e `giftcard` dipendono direttamente dai rispettivi repository SQLite, bypassando il layer `application/query`.
  - Esempio: `BookingApi` dipende da `SqliteBookingRepository`.
  - Esempio: `GiftCardApi` dipende da `SqliteGiftCardRepository`.
- **Dipendenze cicliche tra moduli**: i BC si referenziano a vicenda tramite eventi, policy e repository.
- **Dipendenze domain → application**: le policy in `domain.policies` restituiscono oggetti `Command` definiti in `application.commands`, violando la regola esagonale che il dominio non deve dipendere dall'application layer.
- **Shape dei costrutti architetturali**: `PaymentCharging` risiede in `payment.domain.policies` ma non implementa l'interfaccia `Policy`, violando il contratto shape previsto per le policy. La regola AFF `policiesMustImplementPolicy` (TypeScript) / `allPoliciesMustImplementPolicy` (Java) lo rileva esplicitamente.
- **Naming conventions**: la classe `*Service` è accettata come unica eccezione didattica; suffissi come `*Adapter`, `*RepositoryImpl`, `*Port`, `*Impl`, `Abstract*`, `Default*` non sono presenti in produzione.

## Scaletta Dettagliata e Supporto Facilitatore
(da scaletta-workshop.md - da usare durante il workshop)

**1. Benvenuto e introduzione (10 min)**
- Icebreaker: "Qual è il vostro peggior incubo architetturale o di design?"
- Obiettivi: accoppiamento/coesione → modularità, Bounded Context, AFF come filo conduttore.
- Formazione gruppi (coppie/max 4, 1 PC/gruppo).

**2. Hands-on 1 (60 min)**
- Teoria minima (10 min) + Live-coding facilitatore (15 min).
- Esercizio gruppo (30 min).
- Share Back (5 min).
- *Supporto*: facilitatore gira tra gruppi per aiutare su scelte architetturali.

**3. Hands-on 2 (45 min)**
- Teoria minima (10 min).
- Esercizio (30 min).
- Share Back (5 min).
- *Supporto*: facilitatore aiuta su trade-off coesione/boundary.

**4. Hands-on 3 (45 min) - Principale**
- Esercizio gruppo (30 min): risolvere violazioni senza rompere AFF.
- Share Back (15 min): domande su regole salvate, Monolite Modulare, estrazione microservizi.
- *Supporto*: facilitatore fornisce hint e facilita discussioni.

**5. Prossimi passi (5 min)**
- Idee per continuare a casa (nuove feature, estrarre BC, tool AFF, visualizzazione).

**6. Conclusioni (15 min)**
- Riepilogo, riflessione su estrazione microservizi a basso rischio.
- Feedback rapido ("una cosa che porto a casa").

## Obiettivi Workshop per Facilitatore
- 50% tempo hands-on collaborativo.
- Focus su modularità evolutiva e AFF come meccanismi di verifica.
- Non rivelare la "solution" durante l'esercizio principale.

## Eventi Logici vs Eventi Tecnici
È fondamentale chiarire questa distinzione per evitare che il design venga guidato dalla tecnologia invece che dal business:

- **Eventi Logici (Business Facts)**:
    - Rappresentano fatti avvenuti nel dominio (es. "Prenotazione Confermata").
    - Sono il risultato dell'**Event Storming** (i post-it arancioni).
    - Esistono a prescindere dal software; descrivono l'evoluzione dello stato del business.
    - Sono il cuore del **Linguaggio Ubiquitario**.
- **Eventi Tecnici (Implementation Details)**:
    - Sono gli strumenti dell'**Architettura Event-Driven** (EDA).
    - Riguardano il *come* un fatto viene propagato (es. messaggi su Kafka, eventi di un bus interno, trigger del database).
    - Includono eventi necessari solo al funzionamento tecnico (es. `ConnectionEstablished`, `MessageAcked`, `CacheExpired`).
    - **Punto Chiave**: Il Dominio deve emettere *Eventi Logici*. L'Infrastruttura si occupa di trasformarli in *Eventi Tecnici* (messaggi, pacchetti, ecc.) per la distribuzione.

## Eventi di Dominio vs Eventi di Integrazione
È importante che il facilitatore chiarisca la distinzione durante l'Hands-on 3:
- **Domain Events**: Eventi che descrivono qualcosa di accaduto *all'interno* di un singolo Bounded Context. Sono spesso dettagliati e legati alla consistenza immediata dell'aggregato.
- **Integration Events**: Eventi esplicitamente progettati per essere consumati da *altri* Bounded Context. 
  - **Rilevanza**: Sono lo strumento principale per il disaccoppiamento. Invece di far chiamare a BOOKING il modulo GIFTCARD, BOOKING pubblica un `BookingCreated` (Integration Event) e GIFTCARD reagisce per scalare il credito.
  - **Payload**: Spesso contengono meno dettagli tecnici o sono versionati in modo più rigido per non esporre l'implementazione interna del modulo che li emette (evitando il "distributed monolith").


## Utilizzo della Board Miro (WSC2026)
> [!IMPORTANT]
> **TODO**: Inserire nella board Miro il diagramma dell'**Architettura Interna** (Esagonale/Ports & Adapters) citato nel README e necessario per l'Hands-on 1.

- **Introduzione**: Mostrare il **Big Picture Event Storming** per spiegare come emergono i Bounded Context dagli eventi di business e discutere gli Hot Spot (punti critici). Successivamente, passare al **Software Modelling** per mostrare il flusso dettagliato.
- **Legenda Colori**: Spiegare la legenda (Azzurro chiaro: Comando, Arancio: Evento, Giallo: Aggregato, Viola: Policy, Rosa: Sistema Esterno, Giallo chiaro: Attore, Rosso: Hot Spot).
- **Esempi di Flusso**: Mostrare come la creazione della prenotazione in BOOKING innesca una richiesta di pagamento verso il PAYMENT BC tramite una Policy, e come il risultato del pagamento venga poi propagato a BOOKING e GIFTCARD tramite eventi di integrazione.
- **Visualizzazione Bounded Context**: Nei frame di Software Modelling, i BC sono identificati dalle etichette in alto per rendere evidente il cambio di responsabilità.
- **Hands-on 1**: Usare il diagramma dell'Architettura Interna per mostrare visivamente le dipendenze permesse e vietate all'interno di un modulo.
- **Hands-on 2**: Utilizzare la Context Map "Before" per stimolare la discussione sulle violazioni identificate dai gruppi, confrontandola con il flusso ideale dell'Event Storming.
- **Hands-on 3**: Mostrare la Context Map "After" durante lo share back finale come esempio di possibile soluzione di decoupling che rispetta i confini definiti nell'Event Storming.

## Scenari Avanzati: Pagamento Parziale (Split Payment)
Esistono due approcci principali per gestire il pagamento misto (GiftCard + Elettronico), entrambi ottimi spunti di discussione:

### Opzione A: Coordinamento nel Booking (Orchestratore di Business)
1. **Aggregato Booking**: Traccia `TotalAmount` e `PaidAmount`.
2. **Policy di Coordinamento**: 
   - Reagisce a `BookingStarted` inviando `RedeemGiftCard`.
   - Reagisce a `GiftCardRedeemed` calcolando il residuo: `Total - Redeemed`.
   - Se residuo > 0, invia `InitializePayment` per la differenza.
3. **Invariante**: La prenotazione passa a `Confirmed` solo quando `PaidAmount == TotalAmount`.

### Opzione B: GiftCard come Payment Provider (Gateway Pattern)
In questo scenario, il `Payment BC` funge da **Payment Gateway** universale.
1. **Disaccoppiamento**: Il `Booking BC` chiede solo di pagare il totale via `InitializePayment`.
2. **Responsabilità**: Il `Payment BC` tratta il `GiftCard BC` come un provider al pari di Stripe o PayPal.
3. **Flusso**:
   - `Payment BC` riceve la richiesta di pagamento.
   - Coordina internamente il prelievo dalla GiftCard e/o da Stripe.
   - Restituisce un unico evento `Payment Accepted` al Booking.
*Vantaggio*: Il `Booking` è totalmente agnostico riguardo ai metodi di pagamento.

*Nota per il facilitatore*: In questa versione della codebase, l'Opzione B non è ancora implementata. Il modulo `GiftCard` ha le funzioni di `redeem`/`refund`, ma il `Payment BC` non le utilizza ancora e il flusso non è integrato.

> **Aggiornamento (2026-06-11)**: il **Big Picture Event Storming** sulla board Miro è stato allineato all'**Opzione B**. `GiftCard Redeemed` non è più uno step cross-BC autonomo: è diventato una transazione di pagamento (`Make Transaction`) che può avvenire in parallelo su più provider. Il concetto di *residuo* è stato rimosso dal flusso principale.

## Top-Up Handoff to Payment BC

> **Risolto (2026-06)**: il hand-off da `GiftCardTopUpRequested` a `RequestPayment` è ora cablato nel Composition Root (`Main.java` / `Application`), come gli altri cross-BC reactor. La `TopUpPaymentRequestPolicy` non è più uno stub deferred.

## Sviluppo Futuro dell'Integrazione del Payment BC

> Questa sezione raccoglie le strategie evolutive per l'integrazione del `Payment BC` con `Booking` e `GiftCard`, basate sulle analisi in `.ai/analyses/` e in particolare sulle strategie di integrazione.

### Stato attuale

La codebase Java implementa la **Versione 5 — Direct Wiring in Composition Root**: `Main.java` collega esplicitamente use case e handler dei vari BC. Il `Payment BC` riceve handler generici iniettati dall'esterno e rimane agnostico su chi li implementa. Le violazioni cross-BC (import diretti tra BC) sono intenzionalmente conservate come artefatto didattico.

Flussi cablati:

- **Payment → Booking / GiftCard**: `PaymentAccepted`, `PaymentRejected`, `PaymentExpired`
- **Booking → GiftCard**: `BookingConfirmed`, `BookingRefused`, `BookingRejected`
- **Booking → Payment**: `BookingRefused` richiede rimborso transazione
- **GiftCard → Payment**: `GiftCardTopUpRequested` richiede pagamento

Nota: il `Payment BC` ha già un **event bus in-memory interno** (interfacce in `payment.application.events`, implementazione in `payment.infrastructure.events`). Questo ha risolto le storture interne (process manager che chiama use case, handler nascosto) ma non ha cambiato il cablaggio cross-BC.

### Strategie evolutive

| # | Strategia | Descrizione |
|---|---|---|
| 1 | **In-memory Event Bus cross-BC** | Componente `EventBus` condiviso su cui `Payment` pubblica e gli altri BC si sottoscrivono. |
| 2 | **Published Language in `common`** | Eventi Payment mappati in tipi condivisi in `common`; i BC downstream dipendono solo da `common`. |
| 3 | **Anti-Corruption Layer esplicito** | Ogni BC downstream ha un adapter che traduce eventi Payment in comandi interni. |
| 4 | **Message Broker** | Kafka/RabbitMQ; troppo pesante per il workshop. |
| 5 | **Direct Wiring** | Stato attuale: `Main.java` cabla esplicitamente i componenti. |

#### Versione 1 — In-memory Event Bus cross-BC

Introduce un componente `EventBus` in `common.application`. I use case `Payment` non conoscono gli handler; pubblicano sul bus. I subscriber di `Booking` e `GiftCard` si registrano nel `Composition Root`.

- **Vantaggi**: disaccoppia chi emette da chi ascolta; introduce event-driven integration; zero infrastruttura esterna.
- **Svantaggi**: non risolve le violazioni cross-BC (`Booking`/`GiftCard` continuano a importare tipi `Payment`).
- **Difficoltà stimata**: media.
- **File coinvolti**: use case `Payment`, `Main.java`, test.

#### Versione 3 — Anti-Corruption Layer esplicito

Ogni BC downstream ha un adapter dedicato (es. `PaymentResultAdapter` o `PaymentACL`) che traduce eventi `Payment` in comandi interni. Può combinarsi con il bus o con il Direct Wiring.

- **Vantaggi**: insegna il pattern ACL; evidenzia il confine tra "il loro linguaggio" e "il nostro linguaggio".
- **Svantaggi**: senza Published Language mantiene la dipendenza da `payment.domain.events`.
- **Difficoltà stimata**: media-alta.
- **File coinvolti**: package `Booking`/`GiftCard`, service cross-BC, test.

#### Versione 2 — Published Language in `common`

Sposta una rappresentazione opaca/minimale degli eventi `Payment` in `common`. I BC downstream dipendono solo da `common`; scompaiono gli import diretti da `payment`.

- **Vantaggi**: risolve effettivamente le violazioni cross-BC; insegna il pattern Published Language.
- **Svantaggi**: richiede nuovi tipi condivisi, mapper, refactor di policy e test.
- **Difficoltà stimata**: alta.
- **File coinvolti**: `common`, mapper `Payment`, policy `Booking`/`GiftCard`, test.

### Matrice comparativa dei gap

| Da Versione 5 a… | Difficoltà | File toccati | Concetti introdotti | Risolve violazioni cross-BC? |
|---|---|---|---|---|
| **1 — In-memory Event Bus** | Media | Use case Payment, `Main.java`, test | Event-driven dispatch, publish/subscribe | No |
| **3 — Anti-Corruption Layer** | Media-alta | Package Booking/GiftCard, service cross-BC, test | ACL, adapter | No (da sola) |
| **2 — Published Language** | Alta | `common`, Payment mapper, Booking/GiftCard policy, test | Published Language, contratto pubblico | Sì |

### Percorso didattico consigliato

Per mostrare l'evoluzione architetturale in modo graduale:

1. **Partenza — Versione 5 (Direct Wiring)**  
   Mostra il problema: "funziona ma è tutto cablato". Evidenzia l'accoppiamento in `Main.java`.

2. **Passo 1 — Versione 1 (In-memory Event Bus)**  
   Risolve il wiring cablato. Introduce publish/subscribe. Mantiene le violazioni come esercizio successivo.

3. **Passo 2 — Versione 3 (ACL esplicito)**  
   Mostra che il bus non basta: i BC downstream devono tradurre il linguaggio altrui. Rende esplicito l'adapter.

4. **Passo 3 — Versione 2 (Published Language)**  
   Soluzione completa: contratto condiviso, nessuna dipendenza da `payment.domain`.

Questo percorso costruisce la comprensione gradualmente e lascia spazio a più esercizi di Architectural Fitness Functions.

### Prossimo passo naturale

Il passo successivo è la **Versione 1 — In-memory Event Bus cross-BC**, che disaccoppia chi emette da chi ascolta mantenendo le violazioni cross-BC come esercizio successivo. Poiché il `Payment BC` ha già un bus interno, l'evoluzione consiste nell'estenderlo a tutti i BC o introdurre un bus condiviso in `common`.

### Riferimenti

- Analisi di design e integrazione cross-BC: `.ai/analyses/2026-06-13-bc-design-and-cross-bc-integration.md`
- Piano di alto livello e stato progetto: `.ai/analyses/2026-06-10-project-state.md`

---

## FAQ Architetturali per il Workshop
Questi punti emergono spesso durante la sessione di Hands-on 2 e 3:

### 1. Su cosa si basa la logica di una Policy?
- **Regola**: La Policy dovrebbe essere il più possibile "stateless".
- **Dati dell'Evento**: Spesso l'evento che la scatena ha già le informazioni necessarie (es. IDs).
- **Read Model**: Se la Policy deve prendere una decisione complessa basata su dati storici o cross-aggregate (es. "L'utente ha già usato 3 GiftCard oggi?"), deve consultare un **Read Model**.
- **Mai l'Aggregato**: Non si interroga lo stato interno di un aggregato da una Policy. L'aggregato riceve comandi e decide se eseguirli, non espone il suo stato per logiche reattive esterne.

### 2. Gli Aggregati possono ascoltare Eventi?
- **No**: Gli aggregati reagiscono solo a **Comandi**.
- **Flusso Temporale (Deadline)**: Per gestire le scadenze (es. pagamento entro 15 min). Questa catena specifica gestisce le scadenze temporali:
    1. **Trigger (Evento)**: `TimeReached` (o `PaymentDeadlineReached`).
    2. **Policy**: `PaymentExpiration`.
    3. **Comando**: `ExpirePayment(id)`.
- **Perché?**: Questo garantisce che l'aggregato rimanga un contesto isolato di logica di business pura, senza dipendere da infrastrutture come scheduler o code di messaggi.

### 3. Bounded Context Agnostici (es. Payment)
- **Regola**: Un BC generico o di supporto non deve mai conoscere il core business (Booking, GiftCard).
- **Ruolo**: Agisce come **Adapter** (per isolare i protocolli esterni) e **Dispatcher** (per instradare le richieste al provider corretto).
- **Inbound**: Viene attivato solo tramite **Comandi** espliciti (es. `InitializePayment`) inviati da Policy esterne. Non si sottoscrive a eventi di altri contesti.
- **Outbound**: Emette **Eventi di Integrazione** (es. `PaymentAccepted`, `PaymentRejected`) che altri contesti possono liberamente ascoltare.
- **Vantaggio Didattico**: Questo è il miglior esempio di **disaccoppiamento massimo**. Se domani volessi aggiungere un modulo "Store" per vendere gadget, il `Payment BC` funzionerebbe senza cambiare una riga di codice.

### 4. Strategic Design: Core vs Supporting vs Generic
Per il workshop, è utile classificare i BC per spiegare dove concentrare lo sforzo di design:
- **Booking BC (Core Domain)**: È il cuore del business. Qui si trova la complessità maggiore e il valore differenziante. Deve essere modellato con la massima cura.
- **GiftCard BC (Supporting Subdomain)**: Supporta il core business (incentiva le prenotazioni) ma non è il motivo principale per cui l'hotel esiste. Può essere meno complesso del Booking.
- **Payment BC (Generic Subdomain)**: Risolve un problema comune a molti business (pagare). Potrebbe essere sostituito da un servizio terzo (Stripe/PayPal).

### 5. Interazione Booking <-> Payment <-> GiftCard
Nel workshop, mostriamo l'interazione tramite **Choreography (Event-Driven)** con il `Payment BC` al centro:
1. `BookingPlaced` (Evento in Booking) -> `BookingPaymentRequestPolicy` (Policy in Booking) -> `RequestPayment` (Comando in Payment).
2. `PaymentAccepted` / `PaymentRejected` / `PaymentExpired` (Eventi in Payment) -> handler in Booking e GiftCard che aggiornano il proprio stato.
3. `BookingRefused` (Evento in Booking) -> `BookingRefundRequestPolicy` (Policy in Booking) -> `RefundTransaction` (Comando in Payment).
4. `GiftCardTopUpRequested` (Evento in GiftCard) -> `TopUpPaymentRequestPolicy` (Policy in GiftCard) -> `RequestPayment` (Comando in Payment).

Questo mantiene i BC disaccoppiati: Booking e GiftCard non si chiamano direttamente, ma reagiscono agli eventi emessi dal Payment BC (o inviano comandi verso di esso). Il Composition Root (`Main.java` / `Application`) cabla esplicitamente i cross-BC handler.

### 6. Cos'è la Coreografia Event-Driven?
Questa è la modalità di interazione principale che mostriamo nel workshop per disaccoppiare i Bounded Context:
- **Definizione**: In una coreografia, ogni modulo (Bounded Context) è autonomo e reagisce agli eventi pubblicati dagli altri. Non esiste un orchestratore centrale che conosce o comanda l'intero flusso.
- **In Event Storming**: Si rappresenta graficamente con la sequenza **Evento -> Policy -> Comando**. 
    - Un BC emette un **Evento di Integrazione** (es. `BookingPlaced`).
    - Una **Policy** (viola) "ascolta" l'evento e agisce come mediatore.
    - La Policy invia un **Comando** (azzurro) all'aggregato di un altro BC.
- **Logico vs Tecnico**: In questo flusso, l'evento emesso è un **Evento Logico** (un fatto di business). Il meccanismo tecnico di trasporto (bus, code, segnali) è l'**Evento Tecnico** sottostante che deve rimanere trasparente alla logica di business.
- **Vantaggi Didattici**:
    - **Disaccoppiamento**: Il modulo che emette l'evento non sa (e non deve sapere) chi lo userà.
    - **Estensibilità**: È facile aggiungere nuovi comportamenti (es. inviare una mail alla creazione della prenotazione) aggiungendo una nuova Policy senza modificare il codice del Booking.
    - **Resilienza**: Supporta la consistenza eventuale. Se un modulo è temporaneamente non disponibile, il sistema può continuare a funzionare per le altre parti.

### 7. Scelte Tecnologiche: Semplicità e Trasparenza
Per massimizzare l'efficacia didattica, abbiamo evitato framework "magici" per il REST e la DI:
- **Perché la DI Manuale?**: Obbliga i partecipanti a scrivere il codice di "wiring" (il Composition Root). Se un modulo `A` dipende da un modulo `B` in modo errato, questa dipendenza diventa visibile nel `Main` (Java) o in `Application` (TypeScript). Con Spring o NestJS, le dipendenze sono spesso nascoste dietro scansioni di componenti o decoratori, rendendo le violazioni più difficili da individuare a colpo d'occhio.
- **Perché Javalin ed Express?**: Entrambi permettono di definire le rotte in modo imperativo ed esplicito. Questo riduce il carico cognitivo dei partecipanti, che possono concentrarsi sulla logica di business e sui confini architetturali invece di combattere con configurazioni XML o annotazioni complesse.
- **Perché SQLite?**: Elimina il "collo di bottiglia" del setup infrastrutturale (Docker, database locali). Inoltre, permette di implementare fisicamente il principio del **Database per Bounded Context** creando file `.sqlite` separati (es. `booking.db`, `giftcard.db`). Se un partecipante prova a fare una JOIN tra tabelle di moduli diversi, il sistema fallirà fisicamente, rinforzando il concetto di isolamento.

### 8. Dove collocare Command e Policy rispettando l'architettura esagonale?
Quando si aggiungono le AFF interne a un BC, emerge spesso una violazione: le `Policy` in `domain.policies` generano oggetti `Command` in `application.commands`. Il dominio così dipende dall'applicazione.

Esistono due soluzioni principali:

- **Opzione 1 — Spostare i Command in `domain.commands`**
  - Risolve la violazione mantenendo le Policy nel dominio.
  - Tuttavia i Command sono DTO di ingresso degli use case, quindi concettualmente appartengono all'application layer. Spostarli nel dominio "sporca" il modello con oggetti che descrivono richieste esterne.

- **Opzione 2 — Spostare le Policy in `application.policies`**
  - Mantiene i Command nel loro layer naturale (application).
  - Le Policy diventano coordinatori applicativi che reagiscono a eventi di dominio e invocano use case tramite Command.
  - Rispetta meglio il principio DDD che il dominio è il cerchio più interno e non dipende dall'applicazione.

**Raccomandazione**: preferire l'**Opzione 2**. Preserva la collocazione canonica dei Command nell'application layer e lascia al dominio aggregati, eventi e regole pure. Le Policy reattive che generano Command sono un meccanismo di orchestrazione, non logica di business pura.

**Nota tecnica**: l'interfaccia `Policy<Event, Command>` in `common.domain.model` può rimanere dov'è. Il tipo `Command` è un parametro di tipo risolto dalle implementazioni applicative, quindi non introduce dipendenze dal dominio verso l'applicazione.

## Terminologia suggerita per il Workshop
Per descrivere il flusso **Evento -> Policy -> Comando**, ecco alcuni termini suggeriti da usare durante la spiegazione:

1. **Il Ciclo del "Whenever" (Raccomandato)**: Si lega direttamente alla frase usata per descrivere le Policy ("Ogni volta che accade X, allora fai Y"). È il modo più diretto per spiegare la reattività.
2. **Il Trigger di Business**: Definisce il flusso come l'unità minima di automatizzazione del business (Fatto -> Regola -> Azione).

[Link alla Board Miro WSC2026](https://miro.com/app/board/uXjVHOnchwg=/)

*Working Software 2026*
