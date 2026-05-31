# Esercizio Pratico: Le Guardiane delle Architetture

**Workshop "Le Guardiane delle Architetture"**  
*Working Software 2026*

## 🎯 Obiettivo dell'Esercizio

Imparare a proteggere la **modularità** di un sistema software attraverso le **Architectural Fitness Functions (AFF)**.  
L'esercizio simula un Monolite Modulare che sta degenerando a causa di violazioni dei confini tra Bounded Context e dell'architettura interna dei moduli. I partecipanti useranno le AFF come "guardiane" per identificare, misurare e risolvere questi problemi, mantenendo l'architettura sana e pronta per un'evoluzione futura (inclusa l'eventuale estrazione di microservizi).

## 🗺️ Supporto Visivo: Board Miro

Per tutta la durata del workshop, utilizzeremo una **board Miro dedicata** dove troverete:
- **Event Storming**: La board contiene diversi livelli di analisi:
    - **Specifiche Carta Regalo**: descrizione ad alto livello ed elenco di specifiche iniziali fornite.
    - **Bounded Context Map**: Mappa concettuale che rappresenta i 3 bounded context e la tipologia delle loro iterazioni.
    - **Payment BC - Visione Agnostica**: Contiene delle note concettuali e tecniche su come è stato implementato il `Payment BC` al fine di renderlo agnostico rispetto agli altri bounded context e riuatilizzbile.
    - **Software Modelling (Design Level)**: Flussi dettagliati per ogni Bounded Context (Booking, Payment, GiftCard) con Aggregati, Comandi, Policy e Sistemi Esterni. Qui gli eventi logici vengono mappati nel codice, distinguendoli dalle necessità puramente tecniche dell'architettura software.
- **Bounded Context Map**: La mappa strategica che formalizza le relazioni tra i moduli (`Booking`, `GiftCard`, `Payment`), specificando i ruoli Upstream (U) / Downstream (D) e i pattern di integrazione (ACL, OHS, PL).
- **Architettura Interna**: Schemi dei pattern (Esagonale/Ports & Adapters) usati nei moduli per garantirne la qualità interna.

[Link alla Board Miro WSC2026](https://miro.com/app/board/uXjVHOnchwg=/)

## 🛠️ Il Problema: Un Monolite Modulare che Perde Controllo

Un residence offre ai propri clienti la possibilità di creare una "carta regalo" per premiare la loro fedeltà e disincentivare l'uso dei contanti all'interno del residence. Tramite la "carta regalo" è possibile pagare ai bar e ai ristoranti interni al residence e pagare servizi accessori a prenotazione come escursioni, o liberi (senza prenotazione) ad esempio palestra, estetista, barbiere.

- **L'architettura interna** di ogni modulo (es. accesso diretto dal layer di presentazione ai repository, dipendenze errate tra porte e adapter).
- **I confini tra Bounded Context** (es. import diretti tra moduli diversi, dipendenze cicliche, accoppiamento alto).

Senza enforcement automatico, queste violazioni si accumulano, rendendo il sistema difficile da mantenere, testare ed evolvere. Le AFF trasformano le regole architetturali in test automatizzati che falliscono in caso di violazione, fornendo feedback immediato in CI.

### Dominio: Carta Regalo e Booking (Gift Card)

La codebase è strutturata in **3 Bounded Context** (moduli), con l'implementazione focalizzata sui flussi core della Carta Regalo:

| Bounded Context | Tipo di Sottodominio | Responsabilità Principali | Stato Implementazione |
|-----------------|----------------------|---------------------------|------------------------|
| **Booking** | **Core Domain** | Prenotazioni, Check-in, Check-out, Stato | **Implementato**: Creazione prenotazione e conferma. <br> *Non implementate*: Check-in, Check-out. |
| **GiftCard** | **Supporting** | Emissione, ricarica e riscatto credito e gestione punti fedeltà | **Implementato**: Emissione, Ricarica, Riscatto, Accredito e Rimborso punti fedeltà. |
| **Payment** | **Generic** | Pagamenti elettronici e scadenze, Fatturazione | **Implementato**: Richiesta pagamento, gestione scadenza (48h), transazioni. <br> *Non implementate*: Fatturazione. |

### 🏗️ Architettura Interna: Ports & Adapters (Esagonale)

Ogni modulo segue un'**Architettura Esagonale** per garantire l'isolamento della logica di business e facilitare l'enforcement dei confini tramite le AFF. La struttura è suddivisa in 4 layer concentrici:

1.  **`domain` (Il Cuore)**: Contiene Aggregati, Entity e Value Objects. Definisce le **Porte** (interfacce) per la persistenza e i servizi esterni. Non ha dipendenze.
2.  **`application` (I Casi d'Uso)**: Coordina il flusso di business interni all'applicazione. Dipende solo dal `domain`.
3.  **`infrastructure` (I Dettagli Tecnici)**: Implementa le porte tramite **Adattatori** (jOOQ/Drizzle, client esterni). Dipende da `domain` e `application`.
4.  **`api` (L'Ingresso)**: Espone le funzionalità tramite endpoint REST (Javalin/Express). Dipende dall' layer `application`.

Questa simmetria tra l'implementazione Java e TypeScript permette di focalizzarsi sui principi architetturali indipendentemente dal linguaggio scelto.

#### Focus Architetturale: Payment BC come Adapter & Dispatcher
Il modulo **Payment** è modellato come un *Generic Subdomain* che agisce da ponte verso il mondo esterno:
- **Adapter (Gateway Pattern)**: Protegge il core business dalla complessità dei protocolli dei provider (PayPal, Stripe, ecc.). Traduce i comandi di dominio in chiamate specifiche e unifica le risposte in eventi di integrazione standardizzati (`PaymentAccepted`, `PaymentRejected`).
- **Dispatcher**: Smista le richieste verso il provider corretto in base alla scelta dell'utente e gestisce l'orchestrazione tecnica del pagamento, inclusa la gestione temporale delle scadenze (es. il timeout delle 48 ore).
- **GiftCard come Provider**: In un'architettura avanzata, il `Payment BC` può trattare il modulo `GiftCard` come un ulteriore "payment provider". Questo permette al `Booking BC` di ignorare totalmente l'esistenza delle gift card, delegando al modulo pagamenti il compito di scalare il credito prima di richiedere l'eventuale residuo a provider esterni.

> **NOTE - Stato dell'implementazione**: Nella codebase attuale, il `GiftCard BC` non è ancora integrato come provider all'interno del `Payment BC`. Sebbene le funzionalità di "riscatto" (redeem) e "rimborso" (refund) siano tecnicamente implementate nel modulo `GiftCard`, esse non sono ancora collegate al flusso di pagamento e attualmente l'applicazione non permette di pagare una prenotazione tramite `GiftCard`.

### Specifiche di Business: Sistema Gift Card

Gli esperti del business vi hanno inviato le seguenti specifiche:

1. Un utente registrato può richiedere la carta regalo per la raccolta dei punti, su cui è anche possibile caricare del credito.
2. A ogni prenotazione confermata vengono accreditati i punti.
3. A ogni prenotazione rifiutata vengono accreditati i punti (per rifiutata [**Refused**] si intende una prenotazione pagata correttamente, ma che non è stata confermata per motivi non imputabili all'utente).
4. I punti accreditati sono crediti spendibili per le future prenotazioni dell'utente.
5. L'utente può caricare dei soldi sulla propria carta regalo, tramite il portale di pagamento elettronico.
6. L'utente durante la procedura di prenotazione può pagare sfruttando interamente il credito presente sulla propria carta regalo.
7. L'utente durante la procedura di prenotazione può pagare utilizzando i metodi di pagamento elettronico supportati dal portale.
8. L'utente durante la procedura di prenotazione può pagare sfruttando parzialmente il credito presente sulla propria carta regalo e integrando la restante parte utilizzando i metodi di pagamento elettronico supportati.
9. Le richieste di pagamento vengono inserite sul portale e l'utente può decidere se pagare subito oppure in un secondo momento.
10. L'utente ha tempo 48 ore per terminare il pagamento.
11. Le richieste di pagamento possono essere autorizzate o meno [**Rejected**] dall'istituto di credito.
12. I crediti scalati dalla carta regalo devono essere rimborsati se la prenotazione fallisce per qualsiasi motivo.

**IMPORTANTE:** nessuna speculazione! Tutto quello che non è scritto non esiste e nel dubbio applicate la soluzione più semplice.

### Esempi di possibili "Violazioni Intenzionali" (da scoprire e fixare)

- Un service di un Bounded Context accede direttamente a repository o classi di un altro Bounded Context (violazione boundary).
- Componenti di un modulo accedono a classi di un altro modulo per calcolare logiche di business (accoppiamento cross-BC).
- Il layer di Api accede direttamente a repository, bypassando i service applicativi.
- Dipendenze cicliche tra moduli.
- Violazioni delle convenzioni di naming per porte e adapter in architettura hexagonale.
- Accessi errati tra layer interni (es. Infrastructure verso Domain).
- Costrutti di dominio/applicazione che non rispettano i contratti previsti dall'architettura (es. un Use Case che non implementa l'interfaccia `UseCase`, un Command che non estende/implementa `Command`, una Policy che non implementa `Policy`).

Alcune di queste violazioni sono presenti nella repository di esempio.

## 📋 Gli Esercizi Hands-on (90 minuti totali)

Lavorerete in **coppie o gruppi di max 4 persone** (1 PC per gruppo).  
Il lavoro di gruppo è suddiviso in **3 blocchi integrati** di 30 minuti ciascuno + share back.

### Hands-on 1: Proteggi l'Architettura Interna dei Moduli (30 min)

**Teoria minima + Live-coding** (già vista nella scaletta).

**Task per il gruppo**:
1. Esplora la codebase e identifica l'architettura interna di **almeno un modulo** (es. il modulo di prenotazione o delle gift card).
2. Scrivi **2-3 Architectural Fitness Functions** per proteggerla:
   - Nessun accesso diretto da Presentation a Infrastructure.
   - Regole di dipendenza (Domain non dipende da Infrastructure).
   - Naming convention per porte/adapter (se hexagonal).
3. Esegui le AFF e verifica che falliscano sulle violazioni esistenti.

**Obiettivo**: Toccare con mano l'enforcement dell'architettura interna subito dopo il live-coding.

### Hands-on 2: Bounded Context e Confini Modulari (30 min)

**Task per il gruppo**:
1. Identifica **tutti i Bounded Context** nella codebase.
2. Mappa le violazioni dei confini (usa le AFF per renderle evidenti).
3. Scrivi AFF globali per:
   - `no cross-boundary dependencies` (zero import tra package/moduli diversi).
   - Rispetto package/module boundary.
   - Misura dell'accoppiamento (es. conta dipendenze crossing).

**Obiettivo**: Consolidare la comprensione della modularità e boundary enforcement.

### Hands-on 3: Fix delle Violazioni (Esercizio Principale - 30 min)

**Sfida principale**:
Risolvere le violazioni **senza rompere le AFF** già scritte (o scrivendone di nuove se necessario).

**Possibili soluzioni eleganti** (da discutere in gruppo):
- Introduzione di un **Anti-Corruption Layer** o Shared Kernel minimo.
- Uso di **Domain Events** per decoupling tra BC.
- Refactoring verso una **Context Map** più chiara.
- Introduzione di un nuovo Bounded Context intermedio (es. `pricing` o `checkout`).
- Adattamento dell'architettura interna (es. porte/adapter) per rispettare le regole.

**Criteri di successo**:
- Tutte le AFF passano (sia interne che di boundary).
- Il codice rimane semplice, coeso e testabile.
- Le dipendenze cross-boundary sono eliminate o mediate correttamente.

**Share Back (15 min)**:
Ogni gruppo presenta (2-3 min):
- Quali boundary erano violati?
- Quali regole (AFF) vi hanno "salvato" i confini?
- Il Monolite Modulare ha aiutato o ostacolato?
- Come sarebbe ora l'estrazione di un microservizio?

## 💻 Implementazioni Disponibili

Scegli l'implementazione tecnologica per il tuo gruppo:

- [☕ Implementazione Java](./java/README.md)
- [📘 Implementazione TypeScript](./typescript/README.md)

## 🚀 Come Iniziare (Istruzioni per i Partecipanti)

1. Clona la repository del workshop (link fornito durante la sessione o nella pagina dell'evento).

2. Apri il progetto nel tuo IDE (supporto per Java o TypeScript).

3. Segui le istruzioni nel README specifico del progetto (Java o TypeScript) per eseguire i test iniziali delle AFF e verificare le violazioni.

4. Lavora in gruppo seguendo i task dei 3 Hands-on in sequenza.

## ❓ Domande Guida per la Riflessione

- In che modo le AFF e i confini ben definiti renderebbero l'estrazione di un microservizio un'operazione a basso rischio?
- Quali trade-off avete incontrato tra coesione interna e enforcement dei boundary?
- Come integrereste queste AFF nel vostro flusso di lavoro quotidiano (pre-commit hook, CI pipeline)?

## 📚 Risorse

- Libro: *Domain-Driven Design* di Eric Evans
- *Building Evolutionary Architectures* di Neal Ford, Rebecca Parsons, Patrick Kua
- Repository esempi: (link al repo del workshop)
- Tool: [ArchUnit](https://www.archunit.org/), [ArchUnitTS](https://archunits.github.io/ArchUnitTS/), [dependency-cruiser](https://github.com/sverweij/dependency-cruiser), Structurizr (per visualizzazione Context Map)

---
**Buon lavoro!** Le guardiane sono pronte a proteggervi. 🛡️

*Working Software 2026*
