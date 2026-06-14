// Schema Drizzle per il Payment Bounded Context.
// Replica lo schema finale delle migrazioni Flyway V1-V6.

import { index, integer, real, sqliteTable, text } from 'drizzle-orm/sqlite-core';

export const payments = sqliteTable('payment', {
  id: text('id').primaryKey(),
  clientReference: text('client_reference').notNull(),
  amount: real('amount').notNull(),
  status: text('status').notNull(),
  requestedAt: text('requested_at').notNull(),
});

export const paymentTransactions = sqliteTable(
  'payment_transaction',
  {
    id: integer('id').primaryKey({ autoIncrement: true }),
    paymentId: text('payment_id').notNull(),
    transactionId: text('transaction_id').notNull(),
    provider: text('provider').notNull(),
    providerReference: text('provider_reference'),
    amount: real('amount').notNull(),
    startedAt: text('started_at').notNull(),
    providerCompletedAt: text('provider_completed_at'),
    status: text('status').notNull(),
  },
  (table) => [index('idx_payment_transaction_payment_id').on(table.paymentId)]
);
