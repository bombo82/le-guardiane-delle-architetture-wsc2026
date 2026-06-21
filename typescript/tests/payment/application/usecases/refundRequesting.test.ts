import { beforeEach, describe, expect, it } from 'vitest';
import { generateId } from '@/common/domain/identity/entityId.js';
import { ClientReference } from '@/common/domain/primitive/clientReference.js';
import { Money } from '@/common/domain/primitive/money.js';
import { Timestamp } from '@/common/domain/primitive/timestamp.js';
import { RefundTransaction, refundTransaction } from '@/payment/application/commands/refundTransaction.js';
import { RefundRequesting } from '@/payment/application/usecases/refundRequesting.js';
import { PaymentEvent } from '@/payment/domain/events/paymentEvent.js';

import { Payment } from '@/payment/domain/payment/payment.js';
import { PaymentId } from '@/payment/domain/payment/paymentId.js';
import { Provider } from '@/payment/domain/payment/provider.js';
import { ProviderReference } from '@/payment/domain/payment/providerReference.js';
import { TransactionId } from '@/payment/domain/payment/transactionId.js';
import { Uuid } from '@/common/domain/primitive/uuid.js';
import { SqlitePaymentRepository } from '@/payment/infrastructure/sqlitePaymentRepository.js';
import { CapturingEventPublisher } from '../../../testsupport/events/capturingEventPublisher.js';
import { DatabaseSetup } from '../../../testsupport/databaseSetup.js';

describe('RefundRequesting', () => {
  let repository: SqlitePaymentRepository;
  let publisher: CapturingEventPublisher<PaymentEvent>;
  let refundRequesting: RefundRequesting;

  beforeEach(() => {
    const database = DatabaseSetup.initializeInMemoryDb('payment');
    repository = new SqlitePaymentRepository(database);
    publisher = new CapturingEventPublisher();
    refundRequesting = new RefundRequesting(repository, publisher);
  });

  describe('construction', () => {
    it('rejects null repository', () => {
      expect(() => new RefundRequesting(null as unknown as SqlitePaymentRepository, publisher)).toThrow();
    });

    it('rejects null publisher', () => {
      expect(() => new RefundRequesting(repository, null as unknown as CapturingEventPublisher<PaymentEvent>)).toThrow();
    });
  });

  describe('execution', () => {
    it('rejects null command', () => {
      expect(() => refundRequesting.invoke(null as unknown as RefundTransaction)).toThrow();
    });

    it('should publish refund requested', () => {
      const paymentId = seedAcceptedPayment(50);
      const refundAmount = new Money(50);

      const event = refundRequesting.invoke(refundTransaction(paymentId, refundAmount));

      expect(event.aggregateId).toEqual(paymentId);
      expect(event.amount).toEqual(refundAmount);
      expect(publisher.events()).toHaveLength(1);
      expect(publisher.events()[0].kind).toBe('RefundRequested');
    });

    it('should fail if payment not found', () => {
      const paymentId = generateId((value) => new PaymentId(value));
      const refundAmount = new Money(10);

      expect(() => refundRequesting.invoke(refundTransaction(paymentId, refundAmount))).toThrow();
    });

    it('should fail if payment not accepted', () => {
      const paymentId = generateId((value) => new PaymentId(value));
      const payment = Payment.request(
        paymentId,
        new ClientReference(Uuid.fromString(crypto.randomUUID())),
        new Money(50),
        Timestamp.now()
      );
      repository.save(payment);

      const refundAmount = new Money(10);
      expect(() => refundRequesting.invoke(refundTransaction(paymentId, refundAmount))).toThrow();
    });
  });

  function seedAcceptedPayment(amount: number): PaymentId {
    const paymentId = generateId((value) => new PaymentId(value));
    const clientReference = crypto.randomUUID();
    const money = new Money(amount);
    const requestedAt = Timestamp.now();
    const payment = Payment.request(paymentId, new ClientReference(Uuid.fromString(clientReference)), money, requestedAt);
    const transactionId = generateId((value) => new TransactionId(value));
    payment.startTransaction(
      transactionId,
      Provider.PAYPAL,
      new ProviderReference(Uuid.generate()),
      money,
      new Timestamp(new Date(requestedAt.value.getTime() + 1000))
    );
    payment.acceptTransaction(transactionId, new Timestamp(new Date(requestedAt.value.getTime() + 60000)));
    repository.save(payment);
    return paymentId;
  }
});
