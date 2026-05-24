# Le Guardiane delle Architetture

Workshop da tenere alla conferenza [Working Software 2026](https://www.agilemovement.it/workingsoftware/)

## Proposta

### Description

La complessità di un monolite che ci sfugge di mano ci spinge verso le lussuose promesse di indipendenza, alta coesione e basso accoppiamento dei microservizi. Eppure, troppo spesso, il percorso verso i microservizi ci porta a replicare e persino ad accentuare gli stessi problemi architetturali che cercavamo di lasciarci alle spalle.

Questo workshop è un invito a guardare oltre il paradigma architetturale scelto e le mode del momento, per concentrarci sulla qualità intrinseca del design. Andremo a toccare con mano come costruire sistemi software con architetture semplici e robuste che rimangono resilienti e manutenibili nel tempo.

L'ingrediente segreto per prevenire il degrado architetturale in un monolite o nei microservizi, risiede nella capacità di verificare attivamente la salute della nostra modularità. Vi presenteremo i migliori guardiani delle architetture disponibili sul mercato: le Architectural Fitness Functions (AFF). Proveremo insieme a scriverle e a integrarle nel vostro flusso di lavoro, trasformando le regole architetturali da astratte raccomandazioni a rigidi requisiti verificabili.

Preparatevi a scrivere codice per costruire un monolite o un sistema di microservizi (chi lo può dire!), che sarà in grado di evolvere nel tempo, rimanendo resiliente e manutenibile a lungo, con la certezza che la vostra architettura rimanga sana.

### If I participate in your session, what do I learn?

Come provare a scrivere "Sustainably" Working Software...
- non essere focalizzati sul "come scrivere codice", ma sul "scrivere codice bene" facendo in modo che l'archietettura possa essere un alleato nel tempo e non un nemico contro con cui combattere (e perdere)
- un'introduzione alle Architectural Fitness Functions, che possono essere utilizzate anche per altri aspetti del design del codice

### Do attendees need something?

PC con un IDE installato e configurato per sviluppare software in TypeScript o Java. Non è necessario che tutti abbiano un PC, i partecipanti lavoreranno in coppie o piccoli gruppi, basta che abbiano 1 PC per gruppo.

## Scaletta per il Workshop

Scaletta per il workshop di **180 minuti** "Le Guardiane delle Architetture", pensata per bilanciare teoria, dimostrazioni e **lavoro di gruppo** (90 minuti totali di attività hands-on collaborative), con un'enfasi particolare sulla **modularità** attraverso i concetti di Bounded Context e Monolite Modulare, utilizzando le AFF come garanzia ed enforcing delle regole architetturali e delle buone pratiche. La codebase utilizzata sarà un monolite già strutturato in moduli (Bounded Context), contenente alcune violazioni intenzionali dei confini.

L'idea è di far lavorare i partecipanti in **coppie o piccoli gruppi**, massimo 4 persone (1 singolo PC per gruppo), con momenti di confronto e condivisione.

### Struttura Generale
- **Durata totale**: 180 minuti
- **Focus**: 6% introduzione, 19% teoria minimale (incluso live-coding), 50% esercizi di gruppo, 14% share back/condivisione, 11% conclusioni e prossimi passi
- **Attività di gruppo**: due blocchi integrati (teoria minima + esercizio) + hands-on principale. Totale hands-on collaborativo: 90 minuti (50%)

### Calcolo dei tempi e distribuzione percentuale per tipologia di attività

Considerando il live-coding come parte della spiegazione teorica:

- **Introduzione e benvenuto**: 10 minuti (5.6%)
- **Teoria integrata nei blocchi + Live-coding**: 10 min (teoria architettura interna) + 15 min (live-coding) + 10 min (teoria Bounded Context) = 35 minuti (19.4%)
- **Esercizi di gruppo (Hands-on)**: 30 min + 30 min + 30 min = 90 minuti (50.0%)
- **Share Back e momenti di condivisione**: 5 min + 5 min + 15 min = 25 minuti (13.9%)
- **Prossimi passi e Conclusioni/Q&A**: 5 min + 15 min = 20 minuti (11.1%)
- **Totale**: 180 minuti

**Distribuzione riassuntiva**:
- Introduzione + Teoria/Spiegazione (incl. live-coding): 25%
- Hands-on di gruppo: 50%
- Condivisione + Conclusioni: 25%

### Scaletta Dettagliata

**1. Benvenuto e introduzione (10 min)**
- Accoglienza e presentazione del facilitatore
- Icebreaker rapido: "Qual è il vostro peggior incubo architetturale o di design?" (condivisione in cerchio o post-it)
- Panoramica del workshop e obiettivi di apprendimento:
  - parleremo di accoppiamento e coesione che ci porta alla modularità, passando da concetti di dominio/business (Bounded Context)
  - usiamo un Monolite Modulare come esercizio ed esempio e alla fine del workshop saremo in grado di estrarre microservizi in modo semplice, rapido e naturale, ma non lo faremo!!
  - Architectural Fitness Functions (AFF) come filo conduttore
- Formazione dei gruppi (da coppie a 4 persone, 1 PC per gruppo)
- *Obiettivo*: creare engagement e preparare al lavoro collaborativo

**2. Hands-on 1: Architettura Interna dei Moduli (60 min)**
- **Teoria minima necessaria (10 min)**: Cos'è l'architettura interna di un modulo (Layered Architecture, Hexagonal/Ports-and-Adapters Architecture, etc.). Perché è importante definirla e proteggerla con AFF per mantenere l'ordine all'interno di ogni Bounded Context.
- **Live-coding dal facilitatore (15 min)**: Scrittura della prima Architectural Fitness Function per verificare il rispetto dell'architettura e del design interno di uno dei moduli di esempio (es. regole che impediscono l'accesso diretto dal layer di presentazione al database, o dependency rules tra porte e adapter in hexagonal).
- **Esercizio di gruppo (30 min)**: Ogni gruppo esplora il monolite pre-strutturato in moduli, identifica l'architettura interna di almeno un modulo e scrive AFF per proteggerla.
- **Share Back (5 min)**: ogni gruppo racconta brevemente, in 1 min, problemi e impressioni a seguito della scrittura della prima AFF.
- *Obiettivo*: far toccare con mano subito il concetto di enforcement dell'architettura interna dopo aver visto il live-coding.
- *Supporto*: facilitatore gira tra i gruppi per aiutare e discutere le scelte architetturali o di design.

**3. Hands-on 2: Bounded Context e Confini Modulari (45 min)**
- **Teoria minima necessaria (10 min)**: Definizione e identificazione dei **Bounded Context** come unità coese di business e codice. Monolite Modulare vs Big Ball of Mud: come i confini espliciti permettono un'evoluzione controllata e la futura estrazione di servizi. Ruolo delle AFF nel proteggere i boundary tra context (zero dipendenze crossing-boundary, package/module boundary).
- **Esercizio di gruppo (30 min)**: I gruppi identificano tutti i Bounded Context nella codebase, mappano le violazioni intenzionali dei confini scrivendo AFF specifiche per "nessuna dipendenza tra Bounded Context diversi" e "rispetto dei package/module boundary".
- **Share Back (5 min)**: ogni gruppo racconta brevemente, in 1 min, le violazioni dei boundaries che ha identificato e reso evidenti con le AFF.
- *Obiettivo*: consolidare la comprensione della modularità e boundary enforcement attraverso la pratica.
- *Supporto*: facilitatore gira tra i gruppi per aiutare e discutere sui trade-off tra coesione interna e boundary.

**4. Hands-on 3: Fix delle violazioni dei boundaries (45 min) – Esercizio di gruppo principale**
- **Esercizio di gruppo (30 min)**: I gruppi ragionano come risolvere i problemi di dipendenza tra boundaries differenti, **ma devono rispettare tutte le AFF** definite (sia per architettura interna dei moduli che per boundary modulari) o scriverne di nuove.
- Sfida: trovare soluzioni semplici, chiare ed eleganti per risolvere i problemi di dipendenze cross-boundaries.
- **Share Back (15 min)**: "Quali regole ci hanno salvato i confini? Quali boundary erano violati? Il Monolite Modulare ha aiutato? L'architettura interna è rimasta integra?"
- *Obiettivo*: esperienza pratica di "Sustainably Working Software" con focus su modularità evolutiva
- *Supporto*: facilitatore fornisce hint e facilita discussioni all'interno dei gruppi sui trade-off

**5. Prossimi passi (5 min)**
Idee e spunti su come continuare l'esercizio a casa:
- nuove funzionalità da implementare
- estrarre un boundary in un servizio separato
- esplorare gli altri strumenti che offrono le librerie di AFF
- quanto interagiscono tra loro i boundaries? come visualizzarlo e misurarlo?

**6. Conclusioni, risorse e Q&A (15 min)**
- Riepilogo dei concetti chiave: Bounded Context, Monolite Modulare, AFF come guardiani della modularità (inclusa architettura interna dei moduli)
- **Riflessione**: in che modo le AFF e i confini ben definiti renderebbero l'estrazione di un microservizio successiva un'operazione a basso rischio?"
- Risorse per approfondire (libri come "Domain-Driven Design", tool, repository di esempi modulari)
- Domande aperte
- Feedback rapido dal pubblico (es. "una cosa che porto a casa" sul tema modularità)

#### Note e Suggerimenti per l'Implementazione
- **Materiali necessari**: codebase di esempio pronti con chiara struttura modulare (uno per Java, uno per TypeScript, pre-strutturati con Bounded Context e violazioni intenzionali dei confini; esempio per AFF su architettura interna layered/hexagonal), slide, template per le AFF su boundary, lista di tool
- **Adattamenti**: i tempi sono indicativi e flessibili; la teoria sulla modularità è prioritaria e non va ridotta
- **Coinvolgimento continuo**: domande durante la teoria e fare in modo che i gruppi condividano progressi a metà della sessione hands-on principale, focalizzandosi sui boundary
