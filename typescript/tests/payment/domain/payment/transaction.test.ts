import { describe, expect, it } from 'vitest';
import { generateId } from '@/common/domain/identity/entityId.js';
import { Money } from '@/common/domain/primitive/money.js';
import { Timestamp } from '@/common/domain/primitive/timestamp.js';
import { Provider } from '@/payment/domain/payment/provider.js';
import { ProviderReference } from '@/payment/domain/payment/providerReference.js';
import { Transaction } from '@/payment/domain/payment/transaction.js';
import { Uuid } from '@/common/domain/primitive/uuid.js';
import { TransactionId } from '@/payment/domain/payment/transactionId.js';
import { TransactionStatus } from '@/payment/domain/payment/transactionStatus.js';

describe('Transaction', () => {
  describe('start', () => {
    it('should create started transaction', () => {
      const transactionId = generateId((value) => new TransactionId(value));
      const provider = Provider.GIFT_CARD;
      const providerReference = new ProviderReference(Uuid.generate());
      const amount = new Money(10);
      const startedAt = new Timestamp(new Date('2026-06-07T10:00:00.000Z'));

      const transaction = Transaction.start(transactionId, provider, providerReference, amount, startedAt);

      expect(transaction.id()).toEqual(transactionId);
      expect(transaction.provider()).toEqual(provider);
      expect(transaction.providerReference()).toEqual(providerReference);
      expect(transaction.amount()).toEqual(amount);
      expect(transaction.status()).toEqual(TransactionStatus.STARTED);
      expect(transaction.startedAt()).toEqual(startedAt);
      expect(transaction.completedAt()).toBeNull();
    });

    it('should fail if parameters are null', () => {
      const transactionId = generateId((value) => new TransactionId(value));
      const provider = Provider.GIFT_CARD;
      const providerReference = new ProviderReference(Uuid.generate());
      const amount = new Money(10);
      const startedAt = new Timestamp(new Date('2026-06-07T10:00:00.000Z'));

      expect(() => Transaction.start(null as unknown as TransactionId, provider, providerReference, amount, startedAt)).toThrow();
      expect(() => Transaction.start(transactionId, null as unknown as Provider, providerReference, amount, startedAt)).toThrow();
      expect(() => Transaction.start(transactionId, provider, providerReference, null as unknown as Money, startedAt)).toThrow();
      expect(() => Transaction.start(transactionId, provider, providerReference, amount, null as unknown as Timestamp)).toThrow();
    });
  });

  describe('accept', () => {
    it('should fail if completed at is null', () => {
      const transaction = createStartedTransaction();

      expect(() => transaction.accept(null as unknown as Timestamp)).toThrow();
    });

    it('should fail if transaction is not started', () => {
      const accepted = createStartedTransaction().accept(Timestamp.now());

      expect(() => accepted.accept(Timestamp.now())).toThrow();
    });
  });

  describe('reject', () => {
    it('should fail if completed at is null', () => {
      const transaction = createStartedTransaction();

      expect(() => transaction.reject(null as unknown as Timestamp)).toThrow();
    });

    it('should fail if transaction is not started', () => {
      const rejected = createStartedTransaction().reject(Timestamp.now());

      expect(() => rejected.reject(Timestamp.now())).toThrow();
    });
  });

  function createStartedTransaction(): Transaction {
    return Transaction.start(
      generateId((value) => new TransactionId(value)),
      Provider.GIFT_CARD,
      new ProviderReference(Uuid.generate()),
      new Money(10),
      new Timestamp(new Date('2026-06-07T10:00:00.000Z'))
    );
  }
});
