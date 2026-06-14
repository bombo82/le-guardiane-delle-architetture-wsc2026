// Adapter repository per SQLite usando Drizzle ORM.

import Database from 'better-sqlite3';
import { drizzle } from 'drizzle-orm/better-sqlite3';
import type { BetterSQLite3Database } from 'drizzle-orm/better-sqlite3';
import { and, eq, inArray, lte } from 'drizzle-orm';

import { ClientReference } from '@/common/domain/primitive/clientReference.js';
import { Money } from '@/common/domain/primitive/money.js';
import { Timestamp } from '@/common/domain/primitive/timestamp.js';
import { Uuid } from '@/common/domain/primitive/uuid.js';
import { Payment } from '../domain/payment/payment.js';
import { PaymentId } from '../domain/payment/paymentId.js';
import { PaymentStatus, paymentStatusFromLabel } from '../domain/payment/paymentStatus.js';
import { providerFromLabel } from '../domain/payment/provider.js';
import { ProviderReference } from '../domain/payment/providerReference.js';
import { Transaction } from '../domain/payment/transaction.js';
import { TransactionId } from '../domain/payment/transactionId.js';
import { transactionStatusFromLabel } from '../domain/payment/transactionStatus.js';
import type { PaymentRepository } from '../domain/ports/paymentRepository.js';
import { paymentTransactions, payments } from './schema.js';
import { requireArgument } from '@/common/utils/requireArgument.js';
import { requireDependency } from '@/common/utils/requireDependency.js';

export class SqlitePaymentRepository implements PaymentRepository {
  private readonly _db: BetterSQLite3Database;

  constructor(database: Database.Database) {
    requireDependency(database, "database");
    this._db = drizzle(database);
  }

  save(payment: Payment): void {
    const paymentId = payment.id().value.value;

    this._db
      .insert(payments)
      .values({
        id: paymentId,
        clientReference: payment.clientReference().value,
        amount: payment.amount().value,
        status: payment.status(),
        requestedAt: payment.requestedAt().value.toISOString(),
      })
      .onConflictDoUpdate({
        target: payments.id,
        set: {
          clientReference: payment.clientReference().value,
          amount: payment.amount().value,
          status: payment.status(),
          requestedAt: payment.requestedAt().value.toISOString(),
        },
      })
      .run();

    this._db.delete(paymentTransactions).where(eq(paymentTransactions.paymentId, paymentId)).run();

    for (const transaction of payment.transactions()) {
      this.insertTransaction(paymentId, transaction);
    }
  }

  findById(id: PaymentId): Payment | null {
    const rows = this._db
      .select()
      .from(payments)
      .where(eq(payments.id, id.value.value))
      .all();

    if (rows.length === 0) {
      return null;
    }

    return this.toDomainPayment(rows[0]);
  }

  findByClientReference(clientReference: ClientReference): Payment | null {
    requireArgument(clientReference, 'clientReference');

    const rows = this._db
      .select()
      .from(payments)
      .where(eq(payments.clientReference, clientReference.value))
      .all();

    if (rows.length === 0) {
      return null;
    }

    return this.toDomainPayment(rows[0]);
  }

  findAllRequestedAndProcessingBefore(threshold: Timestamp): Payment[] {
    const rows = this._db
      .select()
      .from(payments)
      .where(
        and(
          inArray(payments.status, [PaymentStatus.REQUESTED, PaymentStatus.PROCESSING]),
          lte(payments.requestedAt, threshold.value.toISOString())
        )
      )
      .all();

    return rows.map((row) => this.toDomainPayment(row));
  }

  private loadTransactions(paymentId: string): Transaction[] {
    const txRows = this._db
      .select()
      .from(paymentTransactions)
      .where(eq(paymentTransactions.paymentId, paymentId))
      .all();

    return txRows.map((record) => this.toDomainTransaction(record));
  }

  private toDomainPayment(record: typeof payments.$inferSelect): Payment {
    const transactions = this.loadTransactions(record.id);
    return new Payment(
      new PaymentId(Uuid.fromString(record.id)),
      new ClientReference(record.clientReference),
      new Money(record.amount),
      paymentStatusFromLabel(record.status),
      new Timestamp(record.requestedAt === null ? new Date(0) : new Date(record.requestedAt)),
      transactions
    );
  }

  private toDomainTransaction(record: typeof paymentTransactions.$inferSelect): Transaction {
    return new Transaction(
      new TransactionId(Uuid.fromString(record.transactionId)),
      providerFromLabel(record.provider),
      record.providerReference === null ? null : new ProviderReference(Uuid.fromString(record.providerReference)),
      new Money(record.amount),
      transactionStatusFromLabel(record.status),
      new Timestamp(record.startedAt === null ? new Date(0) : new Date(record.startedAt)),
      record.providerCompletedAt === null ? null : new Timestamp(new Date(record.providerCompletedAt))
    );
  }

  private insertTransaction(paymentId: string, transaction: Transaction): void {
    this._db
      .insert(paymentTransactions)
      .values({
        paymentId,
        transactionId: transaction.id().value.value,
        provider: transaction.provider(),
        providerReference: this.toNullableReference(transaction.providerReference()),
        amount: transaction.amount().value,
        startedAt: transaction.startedAt().value.toISOString(),
        providerCompletedAt: this.toNullableISO(transaction.completedAt()),
        status: transaction.status(),
      })
      .run();
  }

  private toNullableReference(reference: ProviderReference | null): string | null {
    if (reference === null) return null;
    return reference.value.value;
  }

  private toNullableISO(timestamp: Timestamp | null): string | null {
    if (timestamp === null) return null;
    return timestamp.value.toISOString();
  }
}
