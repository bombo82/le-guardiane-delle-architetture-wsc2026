// Adapter repository per SQLite usando Drizzle ORM.


import Database from 'better-sqlite3';
import { drizzle } from 'drizzle-orm/better-sqlite3';
import type { BetterSQLite3Database } from 'drizzle-orm/better-sqlite3';
import { eq } from 'drizzle-orm';

import { Description } from '@/common/domain/primitive/description.js';
import { GiftCardReference } from '../domain/primitive/giftCardReference.js';
import { Uuid } from '@/common/domain/primitive/uuid.js';
import { Booking } from '../domain/booking/booking.js';
import { BookingId } from '../domain/booking/bookingId.js';
import { BookingStatus } from '../domain/booking/bookingStatus.js';
import type { BookingRepository } from '../domain/ports/bookingRepository.js';
import { bookings } from './schema.js';
import { requireDependency } from '@/common/utils/requireDependency.js';

export class SqliteBookingRepository implements BookingRepository {
  private readonly _db: BetterSQLite3Database;

  constructor(database: Database.Database) {
    requireDependency(database, "database");
    this._db = drizzle(database);
  }

  save(booking: Booking): void {
    const id = booking.id().value.value;
    const status = booking.status();

    this._db
      .insert(bookings)
      .values({
        id,
        description: booking.description().value,
        giftCardId: booking.giftCardReference().value.value,
        status,
      })
      .onConflictDoUpdate({
        target: bookings.id,
        set: {
          description: booking.description().value,
          giftCardId: booking.giftCardReference().value.value,
          status,
        },
      })
      .run();
  }

  findById(id: BookingId): Booking | null {
    const rows = this._db
      .select()
      .from(bookings)
      .where(eq(bookings.id, id.value.value))
      .all();

    if (rows.length === 0) {
      return null;
    }

    const row = rows[0];
    return new Booking(
      new BookingId(Uuid.fromString(row.id)),
      new Description(row.description),
      new GiftCardReference(Uuid.fromString(row.giftCardId)),
      BookingStatus[row.status as keyof typeof BookingStatus]
    );
  }
}
