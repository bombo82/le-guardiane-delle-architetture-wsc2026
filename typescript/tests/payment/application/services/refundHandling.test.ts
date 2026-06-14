import { beforeEach, describe, expect, it } from 'vitest';
import { generateId } from '@/common/domain/identity/entityId.js';
import { ClientReference } from '@/common/domain/primitive/clientReference.js';
import { Description } from '@/common/domain/primitive/description.js';
import { Money } from '@/common/domain/primitive/money.js';
import { Timestamp } from '@/common/domain/primitive/timestamp.js';
import { RefundHandling } from '@/payment/application/services/refundHandling.js';
import { PaymentEvent } from '@/payment/domain/events/paymentEvent.js';
import {
  refundRequested,
  type RefundRequested,
} from '@/payment/domain/events/refundRequested.js';
import type { TransactionRefunded } from '@/payment/domain/events/refundResultEvents.js';
import { Payment } from '@/payment/domain/payment/payment.js';
import { PaymentId } from '@/payment/domain/payment/paymentId.js';
import { PaymentStatus } from '@/payment/domain/payment/paymentStatus.js';
import { providerFromLabel } from '@/payment/domain/payment/provider.js';
import { ProviderReference } from '@/payment/domain/payment/providerReference.js';
import { TransactionId } from '@/payment/domain/payment/transactionId.js';
import { TransactionStatus } from '@/payment/domain/payment/transactionStatus.js';
import { TransactionRefund } from '@/payment/domain/policies/transactionRefund.js';
import type { PaymentProvider } from '@/payment/domain/ports/paymentProvider.js';
import { Uuid } from '@/common/domain/primitive/uuid.js';
import {
  paymentProviderFailure,
  paymentProviderSuccess,
} from '@/payment/domain/ports/paymentProviderResult.js';
import { SqlitePaymentRepository } from '@/payment/infrastructure/sqlitePaymentRepository.js';
import { CapturingEventPublisher } from '../../../testsupport/events/capturingEventPublisher.js';
import { DatabaseSetup } from '../../../testsupport/databaseSetup.js';

describe('RefundHandling', () => {
  let repository: SqlitePaymentRepository;
  let publisher: CapturingEventPublisher<PaymentEvent>;
  let refundHandling: RefundHandling;

  beforeEach(() => {
    const database = DatabaseSetup.initializeInMemoryDb('payment');
    repository = new SqlitePaymentRepository(database);
    publisher = new CapturingEventPublisher();

    const alwaysSuccess: PaymentProvider = {
      process: () => paymentProviderSuccess(Uuid.generate(), Timestamp.now()),
      refund: () => paymentProviderSuccess(Uuid.generate(), Timestamp.now()),
    };
    const alwaysFailure: PaymentProvider = {
      process: () => paymentProviderSuccess(Uuid.generate(), Timestamp.now()),
      refund: () => paymentProviderFailure(new Description('refund declined')),
    };

    const providers: Record<string, PaymentProvider> = {
      PayPal: alwaysSuccess,
      Klarna: alwaysFailure,
      GiftCard: alwaysSuccess,
    };

    refundHandling = new RefundHandling(repository, providers, new TransactionRefund(), publisher);
  });

  describe('construction', () => {
    it('rejects null repository', () => {
      expect(() => new RefundHandling(null as unknown as SqlitePaymentRepository, {}, new TransactionRefund(), publisher)).toThrow();
    });

    it('rejects null providers', () => {
      expect(() => new RefundHandling(repository, null as unknown as Record<string, PaymentProvider>, new TransactionRefund(), publisher)).toThrow();
    });

    it('rejects null policy', () => {
      expect(() => new RefundHandling(repository, {}, null as unknown as TransactionRefund, publisher)).toThrow();
    });

    it('rejects null publisher', () => {
      expect(() => new RefundHandling(repository, {}, new TransactionRefund(), null as unknown as CapturingEventPublisher<PaymentEvent>)).toThrow();
    });
  });

  describe('execution', () => {
    it('rejects null event', () => {
      expect(() => refundHandling.on(null as unknown as RefundRequested)).toThrow();
    });

    it('should refund when provider succeeds', () => {
      const paymentId = seedAcceptedPayment('PayPal', 50);

      refundHandling.on(refundRequested(paymentId, new ClientReference(crypto.randomUUID()), new Money(50)));

      const loaded = repository.findById(paymentId);
      expect(loaded).not.toBeNull();
      expect(loaded!.status()).toEqual(PaymentStatus.REFUNDED);
      expect(loaded!.transactions().every((t) => t.status() === TransactionStatus.REFUNDED)).toBe(true);
      expect(publisher.events()).toHaveLength(1);
      expect(publisher.events()[0].kind).toBe('TransactionRefunded');
      const refunded = publisher.events()[0] as TransactionRefunded;
      expect(refunded.amount).toEqual(new Money(50));
    });

    it('should notify failure when provider rejects', () => {
      const paymentId = seedAcceptedPayment('Klarna', 50);

      refundHandling.on(refundRequested(paymentId, new ClientReference(crypto.randomUUID()), new Money(50)));

      const loaded = repository.findById(paymentId);
      expect(loaded).not.toBeNull();
      expect(loaded!.status()).toEqual(PaymentStatus.ACCEPTED);
      expect(publisher.events()).toHaveLength(1);
      expect(publisher.events()[0].kind).toBe('TransactionNotRefunded');
    });

    it('should refund all accepted transactions', () => {
      const paymentId = seedSplitPayment('PayPal', 60, 'GiftCard', 40);

      refundHandling.on(refundRequested(paymentId, new ClientReference(crypto.randomUUID()), new Money(100)));

      const loaded = repository.findById(paymentId);
      expect(loaded).not.toBeNull();
      expect(loaded!.status()).toEqual(PaymentStatus.REFUNDED);
      expect(loaded!.transactions().every((t) => t.status() === TransactionStatus.REFUNDED)).toBe(true);
      expect(publisher.events()).toHaveLength(1);
      expect(publisher.events()[0].kind).toBe('TransactionRefunded');
      const refunded = publisher.events()[0] as TransactionRefunded;
      expect(refunded.amount).toEqual(new Money(100));
    });

    it('should mark successful transactions as refunded even if one fails', () => {
      const paymentId = seedSplitPayment('PayPal', 60, 'Klarna', 40);

      refundHandling.on(refundRequested(paymentId, new ClientReference(crypto.randomUUID()), new Money(100)));

      const loaded = repository.findById(paymentId);
      expect(loaded).not.toBeNull();
      expect(loaded!.status()).toEqual(PaymentStatus.ACCEPTED);
      const refundedCount = loaded!.transactions().filter((t) => t.status() === TransactionStatus.REFUNDED).length;
      expect(refundedCount).toEqual(1);
      expect(publisher.events()).toHaveLength(1);
      expect(publisher.events()[0].kind).toBe('TransactionNotRefunded');
    });

    it('should fail if payment not found', () => {
      const paymentId = generateId((value) => new PaymentId(value));

      expect(() => refundHandling.on(refundRequested(paymentId, new ClientReference(crypto.randomUUID()), new Money(10)))).toThrow();
    });
  });

  function seedAcceptedPayment(provider: string, amount: number): PaymentId {
    const paymentId = generateId((value) => new PaymentId(value));
    const clientReference = crypto.randomUUID();
    const money = new Money(amount);
    const requestedAt = Timestamp.now();
    const payment = Payment.request(paymentId, new ClientReference(clientReference), money, requestedAt);
    const transactionId = generateId((value) => new TransactionId(value));
    payment.startTransaction(
      transactionId,
      providerFromLabel(provider),
      new ProviderReference(Uuid.generate()),
      money,
      new Timestamp(new Date(requestedAt.value.getTime() + 1000))
    );
    payment.acceptTransaction(transactionId, new Timestamp(new Date(requestedAt.value.getTime() + 60000)));
    repository.save(payment);
    return paymentId;
  }

  function seedSplitPayment(firstProvider: string, firstAmount: number, secondProvider: string, secondAmount: number): PaymentId {
    const paymentId = generateId((value) => new PaymentId(value));
    const clientReference = crypto.randomUUID();
    const total = new Money(firstAmount + secondAmount);
    const requestedAt = Timestamp.now();
    const payment = Payment.request(paymentId, new ClientReference(clientReference), total, requestedAt);

    const firstTx = generateId((value) => new TransactionId(value));
    payment.startTransaction(
      firstTx,
      providerFromLabel(firstProvider),
      new ProviderReference(Uuid.generate()),
      new Money(firstAmount),
      new Timestamp(new Date(requestedAt.value.getTime() + 1000))
    );
    payment.acceptTransaction(firstTx, new Timestamp(new Date(requestedAt.value.getTime() + 60000)));

    const secondTx = generateId((value) => new TransactionId(value));
    payment.startTransaction(
      secondTx,
      providerFromLabel(secondProvider),
      new ProviderReference(Uuid.generate()),
      new Money(secondAmount),
      new Timestamp(new Date(requestedAt.value.getTime() + 2000))
    );
    payment.acceptTransaction(secondTx, new Timestamp(new Date(requestedAt.value.getTime() + 120000)));

    repository.save(payment);
    return paymentId;
  }
});
