import { beforeEach, describe, expect, it } from 'vitest';
import { generateId } from '@/common/domain/identity/entityId.js';
import { ClientReference } from '@/common/domain/primitive/clientReference.js';
import { Money } from '@/common/domain/primitive/money.js';
import { Timestamp } from '@/common/domain/primitive/timestamp.js';
import { PaymentRequesting } from '@/payment/application/usecases/paymentRequesting.js';
import { PaymentRequested } from '@/payment/domain/events/paymentRequested.js';
import { PaymentId } from '@/payment/domain/payment/paymentId.js';
import { SqlitePaymentRepository } from '@/payment/infrastructure/sqlitePaymentRepository.js';
import { CapturingEventPublisher } from '../../../testsupport/events/capturingEventPublisher.js';
import { DatabaseSetup } from '../../../testsupport/databaseSetup.js';
import { RequestPayment, requestPayment } from '@/payment/application/commands/requestPayment.js';
import { PaymentStatus } from '@/payment/domain/payment/paymentStatus.js';

describe('PaymentRequesting', () => {
  let repository: SqlitePaymentRepository;
  let publisher: CapturingEventPublisher<PaymentRequested>;
  let paymentRequesting: PaymentRequesting;

  beforeEach(() => {
    const database = DatabaseSetup.initializeInMemoryDb('payment');
    repository = new SqlitePaymentRepository(database);
    publisher = new CapturingEventPublisher();
    paymentRequesting = new PaymentRequesting(repository, publisher);
  });

  describe('construction', () => {
    it('rejects null repository', () => {
      expect(() => new PaymentRequesting(null as unknown as SqlitePaymentRepository, publisher)).toThrow();
    });

    it('rejects null publisher', () => {
      expect(() => new PaymentRequesting(repository, null as unknown as CapturingEventPublisher<never>)).toThrow();
    });
  });

  describe('execution', () => {
    it('rejects null command', () => {
      expect(() => paymentRequesting.invoke(null as unknown as RequestPayment)).toThrow();
    });

    it('should persist', () => {
      const paymentId = generateId((value) => new PaymentId(value));
      const clientReference = crypto.randomUUID();
      const amount = new Money(75);
      const requestedAt = new Timestamp(new Date('2026-06-07T10:00:00.000Z'));

      const event = paymentRequesting.invoke(
        requestPayment(paymentId, new ClientReference(clientReference), amount, requestedAt)
      );

      const loaded = repository.findById(paymentId);
      expect(loaded).not.toBeNull();
      expect(loaded!.id()).toEqual(paymentId);
      expect(loaded!.clientReference().value).toEqual(clientReference);
      expect(loaded!.amount()).toEqual(amount);
      expect(loaded!.status()).toEqual(PaymentStatus.REQUESTED);

      expect(event.aggregateId).toEqual(paymentId);
    });

    it('should publish requested event', () => {
      const paymentId = generateId((value) => new PaymentId(value));
      const clientReference = crypto.randomUUID();
      const amount = new Money(75);
      const requestedAt = new Timestamp(new Date('2026-06-07T10:00:00.000Z'));

      paymentRequesting.invoke(requestPayment(paymentId, new ClientReference(clientReference), amount, requestedAt));

      expect(publisher.events()).toHaveLength(1);
      expect(publisher.events()[0].kind).toBe('PaymentRequested');
    });
  });
});
