# Esercizio Pratico (Java)

Benvenuti nell'implementazione Java dell'esercizio "Le Guardiane delle Architetture".

## 🛠️ Stack Tecnologico

L'esercizio utilizza strumenti leggeri che privilegiano la trasparenza e il controllo esplicito:

- **Linguaggio**: Java 21+ (con ampio uso di Record e Sealed Classes).
- **Architectural Fitness Functions**: [**ArchUnit**](https://www.archunit.org/) per la verifica automatizzata delle regole architetturali tramite test unitari.
- **Testing**: **JUnit 5**, **AssertJ** e **Javalin Test** per un linguaggio di asserzione coerente tra logica di dominio e API.
- **Database**: **SQLite** (file-based per l'applicazione, in-memory per i test).
- **Accesso al DB**: [**jOOQ**](https://www.jooq.org/) (Type-safe DSL). L'uso dei metadata generati rende gli import cross-modulo espliciti e rilevabili dalle AFF.
- **Migrazioni**: [**Flyway**](https://flywaydb.org/) per la gestione dello schema SQL.
- **REST Server**: **Javalin** (configurazione rotte esplicita, senza annotazioni).
- **Dependency Injection**: **Manuale (Composition Root)** nel `Main`, per rendere palesi le dipendenze e l'accoppiamento tra i moduli.
- **Build Tool**: **Gradle**.
- **Framework**: **Plain Java** per il dominio, evitando astrazioni che possano nascondere violazioni architetturali.
- **Architettura Interna**: **Esagonale (Ports & Adapters)** organizzata in package `domain`, `application`, `infrastructure` e `api`.

### 📖 Documentazione API (OpenAPI & Swagger UI)
Il modulo API espone automaticamente una specifica **OpenAPI 3.1** e una GUI **Swagger UI** tramite i plugin ufficiali di Javalin:
- Specifica JSON disponibile all'endpoint: `/openapi`
- Interfaccia Swagger UI navigabile su: `/swagger`
- La documentazione è generata a compile-time tramite annotazioni `@OpenApi` sugli handler, garantendo zero overhead a runtime.

### 🏗️ Struttura dei Layer

L'implementazione segue una rigorosa separazione delle responsabilità:

- **`domain`**: Contiene le interfacce delle Porte, gli aggregati, i value object e gli eventi.
- **`application`**: Ospita l'orchestrazione dei flussi e i comandi.
- **`infrastructure`**: Contiene gli schemi Drizzle e gli adattatori per il database.
- **`api`**: Gestisce le rotte Express e i DTO di request/response.

### 🗄️ Strategia di Persistenza
Ogni Bounded Context (Booking, GiftCard, Payment) gestisce i propri dati in modo isolato tramite file `.sqlite` separati dentro la cartella `data/<bc>/`:

- `data/giftcard/giftcard.db` — GiftCard BC (file reale, creato al primo `./gradlew run`)
- `build/test-db/giftcard-test-*.db` — database di test isolati (mai committati)

Questo garantisce l'assenza di join illegali a livello di database e facilita un'eventuale futura estrazione in microservizi.

### 🔄 Alternativa AFF e Visualizzazione
Per chi desidera strumenti di visualizzazione o feedback in tempo reale avanzati:
- [**jQAssistant**](https://jqassistant.org/): Permette di analizzare, visualizzare (tramite Neo4j) e validare l'architettura usando query Cypher.
- [**Degraph**](https://github.com/sonegy/degraph): Focalizzato sulla visualizzazione delle dipendenze e l'identificazione di cicli.

### 💡 Suggerimento per l'IDE (Feedback in tempo reale)
Se usi **IntelliJ IDEA**, puoi ottenere un feedback immediato attivando le **Dependency Rules**:
1. Vai in `Settings` -> `Editor` -> `Inspections`.
2. Cerca `Java` -> `Dependency issues` -> `Illegal package dependencies`.
3. Qui puoi definire regole (es. `booking` non può dipendere da `giftcard`).
4. L'IDE sottolineerà in rosso gli import vietati **mentre scrivi il codice**.

### Branch del Progetto
- `main`: il branch di partenza con le violazioni intenzionali da identificare e risolvere.
- `solution`: contiene una possibile implementazione delle soluzioni e dei fix (da consultare post-esercizio).

## 🚀 Come Iniziare

1. **Requisiti**: Assicurati di avere installato un JDK 21 o superiore.
2. **Esecuzione Test**:
   ```bash
   ./gradlew test
   ```
3. **Controllo Architetturale**:
   Le regole si trovano nel pacchetto `architecture`. Puoi eseguire solo i test architetturali con:
   ```bash
   ./gradlew test --tests "*ArchitectureRulesTest"
   ```

---
[⬅️ Torna al README principale](../README.md)
