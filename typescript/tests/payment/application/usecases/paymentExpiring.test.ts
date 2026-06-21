import { beforeEach, describe, expect, it } from 'vitest';
import { generateId } from '@/common/domain/identity/entityId.js';
import { ClientReference } from '@/common/domain/primitive/clientReference.js';
import { Money } from '@/common/domain/primitive/money.js';
import { Timestamp } from '@/common/domain/primitive/timestamp.js';
import { PaymentExpiring } from '@/payment/application/usecases/paymentExpiring.js';
import {
  paymentDeadlineReached,
  type PaymentDeadlineReached,
} from '@/payment/domain/events/paymentDeadlineReached.js';
import { PaymentEvent } from '@/payment/domain/events/paymentEvent.js';
import type { PaymentExpired } from '@/payment/domain/events/paymentResultEvents.js';
import { Payment } from '@/payment/domain/payment/payment.js';
import { PaymentId } from '@/payment/domain/payment/paymentId.js';
import { PaymentStatus } from '@/payment/domain/payment/paymentStatus.js';
import { PaymentExpiration } from '@/payment/application/policies/paymentExpiration.js';
import { SqlitePaymentRepository } from '@/payment/infrastructure/sqlitePaymentRepository.js';
import { Uuid } from '@/common/domain/primitive/uuid.js';
import { CapturingEventPublisher } from '../../../testsupport/events/capturingEventPublisher.js';
import { DatabaseSetup } from '../../../testsupport/databaseSetup.js';

describe('PaymentExpiring', () => {
  let repository: SqlitePaymentRepository;
  let publisher: CapturingEventPublisher<PaymentEvent>;
  let paymentExpiring: PaymentExpiring;

  beforeEach(() => {
    const database = DatabaseSetup.initializeInMemoryDb('payment');
    repository = new SqlitePaymentRepository(database);
    publisher = new CapturingEventPublisher();
    paymentExpiring = new PaymentExpiring(repository, publisher, new PaymentExpiration());
  });

  describe('construction', () => {
    it('rejects null repository', () => {
      expect(() => new PaymentExpiring(null as unknown as SqlitePaymentRepository, publisher, new PaymentExpiration())).toThrow();
    });

    it('rejects null publisher', () => {
      expect(() => new PaymentExpiring(repository, null as unknown as CapturingEventPublisher<PaymentEvent>, new PaymentExpiration())).toThrow();
    });

    it('rejects null policy', () => {
      expect(() => new PaymentExpiring(repository, publisher, null as unknown as PaymentExpiration)).toThrow();
    });
  });

  describe('execution', () => {
    it('rejects null event', () => {
      expect(() => paymentExpiring.on(null as unknown as PaymentDeadlineReached)).toThrow();
    });

    it('should expire payment', () => {
      const paymentId = seedPayment();

      paymentExpiring.on(paymentDeadlineReached(paymentId));

      const loaded = repository.findById(paymentId);
      expect(loaded).not.toBeNull();
      expect(loaded!.status()).toEqual(PaymentStatus.EXPIRED);
    });

    it('should publish expired event', () => {
      const paymentId = seedPayment();

      paymentExpiring.on(paymentDeadlineReached(paymentId));

      expect(publisher.events()).toHaveLength(1);
      expect(publisher.events()[0].kind).toBe('PaymentExpired');
      const expired = publisher.events()[0] as PaymentExpired;
      expect(expired.aggregateId).toEqual(paymentId);
    });

    it('should fail if payment not found', () => {
      const paymentId = generateId((value) => new PaymentId(value));

      expect(() => paymentExpiring.on(paymentDeadlineReached(paymentId))).toThrow();
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
});
