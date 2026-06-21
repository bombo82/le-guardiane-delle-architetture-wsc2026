import { beforeEach, describe, expect, it } from 'vitest';
import { generateId } from '@/common/domain/identity/entityId.js';
import { ClientReference } from '@/common/domain/primitive/clientReference.js';
import { Money } from '@/common/domain/primitive/money.js';
import { Timestamp } from '@/common/domain/primitive/timestamp.js';
import { TransactionAccepting } from '@/payment/application/usecases/transactionAccepting.js';
import { PaymentEvent } from '@/payment/domain/events/paymentEvent.js';
import type { PaymentAccepted } from '@/payment/domain/events/paymentResultEvents.js';
import {
  transactionAccepted,
  type TransactionAccepted,
} from '@/payment/domain/events/transactionAccepted.js';
import { Payment } from '@/payment/domain/payment/payment.js';
import { PaymentId } from '@/payment/domain/payment/paymentId.js';
import { PaymentStatus } from '@/payment/domain/payment/paymentStatus.js';
import { Provider } from '@/payment/domain/payment/provider.js';
import { ProviderReference } from '@/payment/domain/payment/providerReference.js';
import { TransactionId } from '@/payment/domain/payment/transactionId.js';
import { PaymentCompletion } from '@/payment/domain/policies/paymentCompletion.js';
import { SqlitePaymentRepository } from '@/payment/infrastructure/sqlitePaymentRepository.js';
import { Uuid } from '@/common/domain/primitive/uuid.js';
import { CapturingEventPublisher } from '../../../testsupport/events/capturingEventPublisher.js';
import { DatabaseSetup } from '../../../testsupport/databaseSetup.js';

describe('TransactionAccepting', () => {
  let repository: SqlitePaymentRepository;
  let publisher: CapturingEventPublisher<PaymentEvent>;
  let transactionAccepting: TransactionAccepting;

  beforeEach(() => {
    const database = DatabaseSetup.initializeInMemoryDb('payment');
    repository = new SqlitePaymentRepository(database);
    publisher = new CapturingEventPublisher();
    transactionAccepting = new TransactionAccepting(repository, publisher, new PaymentCompletion());
  });

  describe('construction', () => {
    it('rejects null repository', () => {
      expect(() => new TransactionAccepting(null as unknown as SqlitePaymentRepository, publisher, new PaymentCompletion())).toThrow();
    });

    it('rejects null publisher', () => {
      expect(() => new TransactionAccepting(repository, null as unknown as CapturingEventPublisher<PaymentEvent>, new PaymentCompletion())).toThrow();
    });

    it('rejects null policy', () => {
      expect(() => new TransactionAccepting(repository, publisher, null as unknown as PaymentCompletion)).toThrow();
    });
  });

  describe('execution', () => {
    it('rejects null event', () => {
      expect(() => transactionAccepting.on(null as unknown as TransactionAccepted)).toThrow();
    });

    it('should accept transaction', () => {
      const paymentId = seedPayment();
      const transactionId = startTransaction(paymentId, new Money(50));
      const providerAt = new Timestamp(new Date('2026-06-07T10:30:00.000Z'));

      transactionAccepting.on(transactionAccepted(paymentId, Provider.PAYPAL, transactionId, new Money(50), providerAt));

      const loaded = repository.findById(paymentId);
      expect(loaded).not.toBeNull();
      expect(loaded!.status()).toEqual(PaymentStatus.ACCEPTED);
    });

    it('should publish accepted event', () => {
      const paymentId = seedPayment();
      const transactionId = startTransaction(paymentId, new Money(50));
      const providerAt = new Timestamp(new Date('2026-06-07T10:30:00.000Z'));

      transactionAccepting.on(transactionAccepted(paymentId, Provider.PAYPAL, transactionId, new Money(50), providerAt));

      expect(publisher.events()).toHaveLength(1);
      expect(publisher.events()[0].kind).toBe('PaymentAccepted');
      const accepted = publisher.events()[0] as PaymentAccepted;
      expect(accepted.aggregateId).toEqual(paymentId);
    });

    it('should fail if payment not found', () => {
      const paymentId = new PaymentId(Uuid.generate());
      const transactionId = new TransactionId(Uuid.generate());
      const providerAt = new Timestamp(new Date('2026-06-07T10:30:00.000Z'));

      expect(() =>
        transactionAccepting.on(transactionAccepted(paymentId, Provider.PAYPAL, transactionId, new Money(10), providerAt))
      ).toThrow();
    });

    it('should fail if already accepted', () => {
      const paymentId = seedPayment();
      const payment = repository.findById(paymentId)!;
      const acceptedTransactionId = generateId((value) => new TransactionId(value));
      const startedAt = new Timestamp(new Date('2026-06-07T10:01:00.000Z'));
      const acceptedAt = new Timestamp(new Date('2026-06-07T10:30:00.000Z'));
      payment.startTransaction(
        acceptedTransactionId,
        Provider.PAYPAL,
        new ProviderReference(Uuid.generate()),
        new Money(50),
        startedAt
      );
      payment.acceptTransaction(acceptedTransactionId, acceptedAt);
      repository.save(payment);

      const transactionId = new TransactionId(Uuid.generate());
      const providerAt = new Timestamp(new Date('2026-06-07T10:30:00.000Z'));
      expect(() =>
        transactionAccepting.on(transactionAccepted(paymentId, Provider.PAYPAL, transactionId, new Money(50), providerAt))
      ).toThrow();
    });
  });

  function seedPayment(): PaymentId {
    const paymentId = generateId((value) => new PaymentId(value));
    const payment = Payment.request(
      paymentId,
      new ClientReference(Uuid.fromString(crypto.randomUUID())),
      new Money(50),
      new Timestamp(new Date('2026-06-07T10:00:00.000Z'))
    );
    repository.save(payment);
    return paymentId;
  }

  function startTransaction(paymentId: PaymentId, amount: Money): TransactionId {
    const payment = repository.findById(paymentId)!;
    const transactionId = generateId((value) => new TransactionId(value));
    const started = payment.startTransaction(
      transactionId,
      Provider.PAYPAL,
      new ProviderReference(Uuid.generate()),
      amount,
      Timestamp.now()
    );
    repository.save(payment);
    return started.transactionId;
  }
});
