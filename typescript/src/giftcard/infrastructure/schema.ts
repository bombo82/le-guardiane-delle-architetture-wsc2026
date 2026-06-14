// Schema Drizzle per il GiftCard Bounded Context.

import { sqliteTable, text, real } from 'drizzle-orm/sqlite-core';

export const giftCards = sqliteTable('gift_card', {
  id: text('id').primaryKey(),
  balance: real('balance').notNull(),
});
