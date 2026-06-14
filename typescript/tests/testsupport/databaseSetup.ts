// Utility per creare database SQLite isolati per i test.

import { mkdirSync } from 'node:fs';
import Database from 'better-sqlite3';
import { drizzle } from 'drizzle-orm/better-sqlite3';
import { migrate } from 'drizzle-orm/better-sqlite3/migrator';

export class DatabaseSetup {
  static initializeInMemoryDb(moduleName: string): Database.Database {
    const database = new Database(':memory:');
    const db = drizzle(database);
    migrate(db, { migrationsFolder: `./drizzle/${moduleName}` });
    return database;
  }

  static initializeFileDb(moduleName: string, suiteName: string): Database.Database {
    const dir = `data/${moduleName}`;
    mkdirSync(dir, { recursive: true });
    const path = `${dir}/${suiteName}.db`;

    try {
      const existing = new Database(path);
      // Ricrea il file per partire da uno schema pulito.
      existing.close();
      // eslint-disable-next-line no-empty
    } catch (_error) {}

    const database = new Database(path);
    const db = drizzle(database);
    migrate(db, { migrationsFolder: `./drizzle/${moduleName}` });
    return database;
  }
}
