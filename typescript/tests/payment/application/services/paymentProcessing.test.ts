import { beforeEach, describe, expect, it } from 'vitest';
import { generateId } from '@/common/domain/identity/entityId.js';
import { ClientReference } from '@/common/domain/primitive/clientReference.js';
import { Description } from '@/common/domain/primitive/description.js';
import { Money } from '@/common/domain/primitive/money.js';
import { Timestamp } from '@/common/domain/primitive/timestamp.js';
import { StartTransaction, startTransaction } from '@/payment/application/commands/startTransaction.js';
import { TransactionAccepting } from '@/payment/application/usecases/transactionAccepting.js';
import { TransactionRejecting } from '@/payment/application/usecases/transactionRejecting.js';
import type { PaymentAccepted, PaymentRejected } from '@/payment/domain/events/paymentResultEvents.js';
import { Payment } from '@/payment/domain/payment/payment.js';
import { PaymentId } from '@/payment/domain/payment/paymentId.js';
import { PaymentStatus } from '@/payment/domain/payment/paymentStatus.js';
import { Provider, providerFromLabel } from '@/payment/domain/payment/provider.js';
import { ProviderReference } from '@/payment/domain/payment/providerReference.js';
import { PaymentCompletion } from '@/payment/domain/policies/paymentCompletion.js';
import { PaymentRejection } from '@/payment/domain/policies/paymentRejection.js';
import type { PaymentProvider } from '@/payment/domain/ports/paymentProvider.js';
import {
  paymentProviderFailure,
  paymentProviderSuccess,
} from '@/payment/domain/ports/paymentProviderResult.js';
import { SqlitePaymentRepository } from '@/payment/infrastructure/sqlitePaymentRepository.js';
import { InMemoryPaymentEventBus } from '@/payment/infrastructure/inMemoryPaymentEventBus.js';
import { PaymentProcessing } from '@/payment/application/services/paymentProcessing.js';
import { Uuid } from '@/common/domain/primitive/uuid.js';
import { CapturingSubscriber } from '../../../testsupport/events/capturingSubscriber.js';
import { DatabaseSetup } from '../../../testsupport/databaseSetup.js';

describe('PaymentProcessing', () => {
  let repository: SqlitePaymentRepository;
  let eventBus: InMemoryPaymentEventBus;
  let paymentProcessing: PaymentProcessing;

  beforeEach(() => {
    const database = DatabaseSetup.initializeInMemoryDb('payment');
    repository = new SqlitePaymentRepository(database);
    eventBus = new InMemoryPaymentEventBus((task) => task());
    const accepting = new TransactionAccepting(repository, eventBus, new PaymentCompletion());
    const rejecting = new TransactionRejecting(repository, eventBus, new PaymentRejection());
    eventBus.subscribe('TransactionAccepted', accepting);
    eventBus.subscribe('TransactionRejected', rejecting);

    const providerAt = new Timestamp(new Date('2026-06-07T10:30:00.000Z'));
    const alwaysSuccess: PaymentProvider = {
      process: () => paymentProviderSuccess(Uuid.generate(), providerAt),
      refund: () => paymentProviderSuccess(Uuid.generate(), Timestamp.now()),
    };
    const alwaysFailure: PaymentProvider = {
      process: () => paymentProviderFailure(new Description('declined')),
      refund: () => paymentProviderFailure(new Description('declined')),
    };

    const providers: Record<string, PaymentProvider> = {
      PayPal: alwaysSuccess,
      Klarna: alwaysFailure,
    };

    paymentProcessing = new PaymentProcessing(repository, providers, eventBus);
  });

  describe('construction', () => {
    it('rejects null parameters', () => {
      const providers: Record<string, PaymentProvider> = {};

      expect(() => new PaymentProcessing(null as unknown as SqlitePaymentRepository, providers, eventBus)).toThrow();
      expect(() => new PaymentProcessing(repository, null as unknown as Record<string, PaymentProvider>, eventBus)).toThrow();
      expect(() => new PaymentProcessing(repository, providers, null as unknown as InMemoryPaymentEventBus)).toThrow();
    });
  });

  describe('execution', () => {
    it('rejects null command', () => {
      expect(() => paymentProcessing.invoke(null as unknown as StartTransaction)).toThrow();
    });

    it('should accept when provider succeeds', () => {
      const paymentId = seedPayment(50);

      const acceptedCaptor = new CapturingSubscriber<PaymentAccepted>();
      eventBus.subscribe('PaymentAccepted', acceptedCaptor);

      paymentProcessing.invoke(
        startTransaction(
          paymentId,
          Provider.PAYPAL,
          new ProviderReference(Uuid.generate()),
          new Money(50),
          Timestamp.now()
        )
      );

      const loaded = repository.findById(paymentId);
      expect(loaded).not.toBeNull();
      expect(loaded!.status()).toEqual(PaymentStatus.ACCEPTED);
      expect(acceptedCaptor.events()).toHaveLength(1);
    });

    it('should reject when provider fails', () => {
      const paymentId = seedPayment(50);

      const rejectedCaptor = new CapturingSubscriber<PaymentRejected>();
      eventBus.subscribe('PaymentRejected', rejectedCaptor);

      paymentProcessing.invoke(
        startTransaction(
          paymentId,
          Provider.KLARNA,
          new ProviderReference(Uuid.generate()),
          new Money(50),
          Timestamp.now()
        )
      );

      const loaded = repository.findById(paymentId);
      expect(loaded).not.toBeNull();
      expect(loaded!.status()).toEqual(PaymentStatus.REJECTED);
      expect(rejectedCaptor.events()).toHaveLength(1);
    });

    it('should fail if payment not found', () => {
      const paymentId = generateId((value) => new PaymentId(value));

      expect(() =>
        paymentProcessing.invoke(
          startTransaction(
            paymentId,
            Provider.PAYPAL,
            new ProviderReference(Uuid.generate()),
            new Money(50),
            Timestamp.now()
          )
        )
      ).toThrow();
    });

    it('should fail if provider unknown', () => {
      const paymentId = seedPayment(50);

      expect(() =>
        paymentProcessing.invoke(
          startTransaction(
            paymentId,
            providerFromLabel('UnknownProvider'),
            new ProviderReference(Uuid.generate()),
            new Money(50),
            Timestamp.now()
          )
        )
      ).toThrow();
    });
  });

  function seedPayment(amount: number): PaymentId {
    const paymentId = generateId((value) => new PaymentId(value));
    const payment = Payment.request(
      paymentId,
      new ClientReference(Uuid.fromString(crypto.randomUUID())),
      new Money(amount),
      new Timestamp(new Date('2026-06-07T10:00:00.000Z'))
    );
    repository.save(payment);
    return paymentId;
  }
});
