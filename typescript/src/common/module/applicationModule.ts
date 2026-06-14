// Modulo applicativo di base: gestisce l'inizializzazione del database SQLite e le migrazioni Drizzle.

import { mkdirSync } from 'node:fs';
import { dirname } from 'node:path';
import type { Express } from 'express';
import Database from 'better-sqlite3';
import { drizzle } from 'drizzle-orm/better-sqlite3';
import { migrate } from 'drizzle-orm/better-sqlite3/migrator';

export abstract class ApplicationModule {
  initializeDb(moduleName: string): Database.Database {
    const path = `data/${moduleName}/${moduleName}.db`;
    mkdirSync(dirname(path), { recursive: true });

    const database = new Database(path);
    const db = drizzle(database);
    migrate(db, { migrationsFolder: `./drizzle/${moduleName}` });

    return database;
  }

  abstract configure(app: Express): void;

  /**
   * Override in caso di clean-up necessario durante lo shutdown dell'applicazione,
   * ad esempio per fermare executor schedulati o chiudere risorse.
   */
  stop(): void {
    // default: nessuna operazione
  }
}
