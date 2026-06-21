import { describe, expect, it } from 'vitest';
import { generateId } from '@/common/domain/identity/entityId.js';
import { ClientReference } from '@/common/domain/primitive/clientReference.js';
import { Description } from '@/common/domain/primitive/description.js';
import { Money } from '@/common/domain/primitive/money.js';
import { Timestamp } from '@/common/domain/primitive/timestamp.js';
import { Payment } from '@/payment/domain/payment/payment.js';
import { PaymentId } from '@/payment/domain/payment/paymentId.js';
import { PaymentStatus } from '@/payment/domain/payment/paymentStatus.js';
import { Uuid } from '@/common/domain/primitive/uuid.js';
import { Provider } from '@/payment/domain/payment/provider.js';
import { ProviderReference } from '@/payment/domain/payment/providerReference.js';
import { TransactionId } from '@/payment/domain/payment/transactionId.js';


const REQUESTED_AT = new Timestamp(new Date('2026-06-07T10:00:00.000Z'));

function createPayment(): Payment {
  return Payment.request(
    generateId((value) => new PaymentId(value)),
    new ClientReference(Uuid.fromString(crypto.randomUUID())),
    new Money(50),
    REQUESTED_AT
  );
}

describe('Payment', () => {
  describe('request', () => {
    it('should create payment in requested status', () => {
      const paymentId = generateId((value) => new PaymentId(value));
      const clientReference = crypto.randomUUID();
      const amount = new Money(50);

      const payment = Payment.request(paymentId, new ClientReference(Uuid.fromString(clientReference)), amount, REQUESTED_AT);

      expect(payment.id()).toEqual(paymentId);
      expect(payment.clientReference().toString()).toEqual(clientReference);
      expect(payment.amount()).toEqual(amount);
      expect(payment.status()).toEqual(PaymentStatus.REQUESTED);
    });

    it('should fail if parameters are null', () => {
      const paymentId = generateId((value) => new PaymentId(value));
      const clientReference = new ClientReference(Uuid.fromString(crypto.randomUUID()));
      const amount = new Money(50);

      expect(() => Payment.request(null as unknown as PaymentId, clientReference, amount, REQUESTED_AT)).toThrow();
      expect(() => Payment.request(paymentId, null as unknown as ClientReference, amount, REQUESTED_AT)).toThrow();
      expect(() => Payment.request(paymentId, clientReference, null as unknown as Money, REQUESTED_AT)).toThrow();
      expect(() => Payment.request(paymentId, clientReference, amount, null as unknown as Timestamp)).toThrow();
    });
  });

  describe('startTransaction', () => {
    it('should fail if parameters are null', () => {
      const payment = createPayment();
      const transactionId = generateId((value) => new TransactionId(value));
      const provider = Provider.GIFT_CARD;
      const amount = new Money(50);
      const startedAt = REQUESTED_AT.plusSeconds(1);

      expect(() => payment.startTransaction(null as unknown as TransactionId, provider, null, amount, startedAt)).toThrow();
      expect(() => payment.startTransaction(transactionId, null as unknown as Provider, null, amount, startedAt)).toThrow();
      expect(() => payment.startTransaction(transactionId, provider, null, null as unknown as Money, startedAt)).toThrow();
      expect(() => payment.startTransaction(transactionId, provider, null, amount, null as unknown as Timestamp)).toThrow();
    });
  });

  describe('acceptTransaction', () => {
    it('should produce payment accepted event', () => {
      const payment = createPayment();
      const transactionId = generateId((value) => new TransactionId(value));
      payment.startTransaction(
        transactionId,
        Provider.GIFT_CARD,
        new ProviderReference(Uuid.generate()),
        new Money(50),
        REQUESTED_AT.plusSeconds(1)
      );

      const event = payment.acceptTransaction(transactionId, REQUESTED_AT.plusSeconds(60));

      expect(event!.kind).toBe('PaymentAccepted');
      expect(event!.aggregateId).toEqual(payment.id());
      expect(event!.amount).toEqual(new Money(50));
    });

    it('should fail if parameters are null', () => {
      const payment = createPayment();

      expect(() => payment.acceptTransaction(null as unknown as TransactionId, null as unknown as Timestamp)).toThrow();
    });
  });

  describe('rejectTransaction', () => {
    it('should fail if parameters are null', () => {
      const payment = createPayment();
      const transactionId = generateId((value) => new TransactionId(value));
      payment.startTransaction(
        transactionId,
        Provider.GIFT_CARD,
        new ProviderReference(Uuid.generate()),
        new Money(50),
        REQUESTED_AT.plusSeconds(1)
      );

      expect(() => payment.rejectTransaction(null as unknown as TransactionId, new Description('reason'))).toThrow();
      expect(() => payment.rejectTransaction(transactionId, null as unknown as Description)).toThrow();
    });
  });

  describe('requestRefund', () => {
    it('should fail if amount is null', () => {
      const payment = createPayment();
      const transactionId = generateId((value) => new TransactionId(value));
      payment.startTransaction(
        transactionId,
        Provider.GIFT_CARD,
        new ProviderReference(Uuid.generate()),
        new Money(50),
        REQUESTED_AT.plusSeconds(1)
      );
      payment.acceptTransaction(transactionId, REQUESTED_AT.plusSeconds(60));

      expect(() => payment.requestRefund(null as unknown as Money)).toThrow();
    });
  });

  describe('rejectRefund', () => {
    it('should fail if parameters are null', () => {
      const payment = createPayment();
      const transactionId = generateId((value) => new TransactionId(value));
      payment.startTransaction(
        transactionId,
        Provider.GIFT_CARD,
        new ProviderReference(Uuid.generate()),
        new Money(50),
        REQUESTED_AT.plusSeconds(1)
      );
      payment.acceptTransaction(transactionId, REQUESTED_AT.plusSeconds(60));

      const provider = Provider.GIFT_CARD;
      const providerReference = new ProviderReference(Uuid.generate());

      expect(() => payment.rejectRefund(null as unknown as Provider, providerReference, new Description('reason'))).toThrow();
      expect(() => payment.rejectRefund(provider, null as unknown as ProviderReference, new Description('reason'))).toThrow();
      expect(() => payment.rejectRefund(provider, providerReference, null as unknown as Description)).toThrow();
    });
  });
});
