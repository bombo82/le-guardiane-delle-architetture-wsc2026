// Schema Drizzle per il Booking Bounded Context.

import { sqliteTable, text } from 'drizzle-orm/sqlite-core';

export const bookings = sqliteTable('booking', {
  id: text('id').primaryKey(),
  description: text('description').notNull(),
  giftCardId: text('gift_card_id').notNull(),
  status: text('status').notNull(),
});
