# Esercizio Pratico (TypeScript)

Benvenuti nell'implementazione TypeScript dell'esercizio "Le Guardiane delle Architetture".

## 🛠️ Stack Tecnologico

L'esercizio utilizza strumenti leggeri che privilegiano la trasparenza e il controllo esplicito:

- **Linguaggio**: TypeScript 5+.
- **Architectural Fitness Functions**: [**ArchUnitTS**](https://archunits.github.io/ArchUnitTS/) per la verifica automatizzata delle regole di design tramite i test.
- **Testing**: **Vitest** e [**Supertest**](https://github.com/ladjs/supertest) per un approccio al testing fluido e dichiarativo.
- **Database**: **SQLite** (file-based per l'applicazione, in-memory per i test).
- **Accesso al DB**: [**Drizzle ORM**](https://orm.drizzle.team/). La definizione esplicita dello schema rende gli import tra moduli visibili e rilevabili dalle AFF.
- **Migrazioni**: [**Drizzle-kit**](https://orm.drizzle.team/kit-docs/overview) per la gestione automatizzata dello schema.
- **REST Server**: **Express** (gestione rotte semplice e senza astrazioni complesse).
- **Dependency Injection**: **Manuale** tramite costruttori o factory functions nel punto di ingresso dell'applicazione.
- **Framework**: **Plain TypeScript**, evitando framework che nascondano le dipendenze strutturali.
- **Architettura Interna**: **Esagonale (Ports & Adapters)** organizzata in directory `domain`, `application`, `infrastructure` e `api`.

### 🏗️ Struttura dei Layer

L'implementazione segue una rigorosa separazione delle responsabilità:

- **`domain`**: Contiene le interfacce delle Porte e la logica di business pura.
- **`application`**: Ospita i Casi d'Uso e le Policy reattive.
- **`infrastructure`**: Contiene gli schemi Drizzle e gli Adattatori per il database.
- **`api`**: Gestisce le rotte Express e i controller.

### 🗄️ Strategia di Persistenza
Ogni Bounded Context (Booking, GiftCard, Payment) gestisce i propri dati in modo isolato tramite file `.sqlite` separati. Questo garantisce l'assenza di join illegali a livello di database e facilita un'eventuale futura estrazione in microservizi.

### 🔄 Alternativa AFF
Per chi preferisce un approccio basato su analisi statica e feedback immediato nell'IDE, è possibile utilizzare:
- [**Dependency-cruiser**](https://github.com/sverweij/dependency-cruiser): Per la validazione e visualizzazione delle dipendenze a livello di file system.
- [**eslint-plugin-boundaries**](https://github.com/javierbrea/eslint-plugin-boundaries): Per ricevere avvisi in tempo reale direttamente durante la scrittura del codice.

### Branch del Progetto
- `main`: il branch di partenza con le violazioni intenzionali da identificare e risolvere.
- `solution`: contiene una possibile implementazione delle soluzioni e dei fix (da consultare post-esercizio).

## 🚀 Come Iniziare

1. **Requisiti**: Assicurati di avere installato Node.js (versione 18+) e `npm`.
2. **Installazione**:
   ```bash
   npm install
   ```
3. **Controllo Architetturale**:
   Esegui il check delle dipendenze con:
   ```bash
   npm run architecture:check
   ```
4. **Esecuzione Test**:
   ```bash
   npm test
   ```

---
[⬅️ Torna al README principale](../README.md)
