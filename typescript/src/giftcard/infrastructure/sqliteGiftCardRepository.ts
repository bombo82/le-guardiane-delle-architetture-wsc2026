// Adapter repository per SQLite usando Drizzle ORM.

import Database from 'better-sqlite3';
import { drizzle } from 'drizzle-orm/better-sqlite3';
import type { BetterSQLite3Database } from 'drizzle-orm/better-sqlite3';
import { eq } from 'drizzle-orm';

import { Money } from '@/common/domain/primitive/money.js';
import { Uuid } from '@/common/domain/primitive/uuid.js';
import { GiftCard } from '../domain/giftcard/giftCard.js';
import { GiftCardId } from '../domain/giftcard/giftCardId.js';
import type { GiftCardRepository } from '../domain/ports/giftCardRepository.js';
import { giftCards } from './schema.js';
import { requireDependency } from '@/common/utils/requireDependency.js';

export class SqliteGiftCardRepository implements GiftCardRepository {
  private readonly _db: BetterSQLite3Database;

  constructor(database: Database.Database) {
    requireDependency(database, "database");
    this._db = drizzle(database);
  }

  save(giftCard: GiftCard): void {
    const id = giftCard.id().value.value;
    const balance = giftCard.balance().value;

    this._db
      .insert(giftCards)
      .values({ id, balance })
      .onConflictDoUpdate({
        target: giftCards.id,
        set: { balance },
      })
      .run();
  }

  findById(id: GiftCardId): GiftCard | null {
    const rows = this._db
      .select()
      .from(giftCards)
      .where(eq(giftCards.id, id.value.value))
      .all();

    if (rows.length === 0) {
      return null;
    }

    const row = rows[0];
    return new GiftCard(new GiftCardId(Uuid.fromString(row.id)), new Money(row.balance));
  }
}
