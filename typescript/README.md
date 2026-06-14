# Esercizio Pratico (TypeScript)

Benvenuti nell'implementazione TypeScript dell'esercizio "Le Guardiane delle Architetture".

## 🛠️ Stack Tecnologico

L'esercizio utilizza strumenti leggeri che privilegiano la trasparenza e il controllo esplicito:

- **Linguaggio**: TypeScript 5.8+.
- **Architectural Fitness Functions**: [**ArchUnitTS**](https://archunits.github.io/ArchUnitTS/) (`archunit` 2.3.0) per la verifica automatizzata delle regole di design tramite i test.
- **Testing**: **Vitest** 3.2 e [**Supertest**](https://github.com/ladjs/supertest) 7.1 per un approccio al testing fluido e dichiarativo.
- **Database**: **SQLite** (file-based per l'applicazione, in-memory per i test).
- **Accesso al DB**: [**Drizzle ORM**](https://orm.drizzle.team/). La definizione esplicita dello schema rende gli import tra moduli visibili e rilevabili dalle AFF.
- **Migrazioni**: [**Drizzle-kit**](https://orm.drizzle.team/kit-docs/overview) per la gestione automatizzata dello schema.
- **REST Server**: **Express** 5.1 (gestione rotte semplice e senza astrazioni complesse).
- **Dependency Injection**: **Manuale** tramite costruttori nel punto di ingresso dell'applicazione (`Application`).
- **Framework**: **Plain TypeScript**, evitando framework che nascondano le dipendenze strutturali.
- **Architettura Interna**: **Esagonale (Ports & Adapters)** organizzata in directory `domain`, `application`, `infrastructure` e `api`.

### 📖 Documentazione API (OpenAPI & Swagger UI)
Il modulo API espone una specifica **OpenAPI 3.0** e una GUI **Swagger UI** tramite `swagger-jsdoc` e `swagger-ui-express`:
- Specifica JSON disponibile all'endpoint: `/openapi`
- Interfaccia Swagger UI navigabile su: `/swagger`
- La documentazione è generata tramite annotazioni JSDoc `@swagger` vicino agli handler, garantendo che la spec evolva insieme al codice.

### 🏗️ Struttura dei Layer

L'implementazione segue una rigorosa separazione delle responsabilità:

- **`domain`**: Contiene le interfacce delle Porte, gli aggregati, i value object e gli eventi.
- **`application`**: Ospita l'orchestrazione dei flussi e i comandi.
- **`infrastructure`**: Contiene gli schemi Drizzle e gli adattatori per il database.
- **`api`**: Gestisce le rotte Express e i DTO di request/response.

### 🗄️ Strategia di Persistenza

Ogni Bounded Context (Booking, GiftCard, Payment) gestisce i propri dati in modo isolato tramite file SQLite separati (`data/<bc>/<bc>.db`). Questo garantisce l'assenza di join illegali a livello di database e facilita un'eventuale futura estrazione in microservizi.

Le migrazioni sono separate per modulo:
- `drizzle/booking/`
- `drizzle/giftcard/`
- `drizzle/payment/`

### 🔄 Alternativa AFF

Per chi preferisce un approccio basato su analisi statica e feedback immediato nell'IDE, è possibile utilizzare:

- [**Dependency-cruiser**](https://github.com/sverweij/dependency-cruiser): Per la validazione e visualizzazione delle dipendenze a livello di file system.
- [**eslint-plugin-boundaries**](https://github.com/javierbrea/eslint-plugin-boundaries): Per ricevere avvisi in tempo reale direttamente durante la scrittura del codice.

### Branch del Progetto

- `main`: il branch di partenza con le violazioni intenzionali da identificare e risolvere.
- `solution`: contiene una possibile implementazione delle soluzioni e dei fix (da consultare post-esercizio).

## 🚀 Come Iniziare

1. **Requisiti**: Assicurati di avere installato Node.js (versione 22+) e `pnpm`.
2. **Installazione**:
   ```bash
   cd typescript
   pnpm install
   ```
3. **Controllo tipi**:
   ```bash
   pnpm run typecheck
   ```
4. **Esecuzione Test**:
   ```bash
   pnpm test
   ```
   > Attenzione: i test attualmente terminano con **8 fallimenti voluti** nelle regole architetturali (AFF). Questi fallimenti sono parte integrante dell'esercizio didattico.
5. **Avvio dell'applicazione**:
   ```bash
   pnpm start
   ```
   L'applicazione è disponibile su `http://localhost:7070`.

---
[⬅️ Torna al README principale](../README.md)
