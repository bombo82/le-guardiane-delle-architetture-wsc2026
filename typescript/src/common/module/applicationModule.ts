import { mkdirSync } from 'node:fs';
import { dirname } from 'node:path';
import type { Express } from 'express';
import Database from 'better-sqlite3';
import { drizzle } from 'drizzle-orm/better-sqlite3';
import { migrate } from 'drizzle-orm/better-sqlite3/migrator';

export interface WebApi {
  configure(app: Express): void;
}

export abstract class ApplicationModule {
  abstract webApis(): WebApi[];

  static initializeDb(moduleName: string): Database.Database {
    const path = `data/${moduleName}/${moduleName}.db`;
    mkdirSync(dirname(path), { recursive: true });

    const database = new Database(path);
    const db = drizzle(database);
    migrate(db, { migrationsFolder: `./drizzle/${moduleName}` });

    return database;
  }

  stop(): void {
    // default: nessuna operazione
  }
}
