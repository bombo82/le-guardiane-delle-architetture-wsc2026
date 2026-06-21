import { describe, expect, it } from 'vitest';
import { generateId } from '@/common/domain/identity/entityId.js';
import { ClientReference } from '@/common/domain/primitive/clientReference.js';
import { Description } from '@/common/domain/primitive/description.js';
import { Money } from '@/common/domain/primitive/money.js';
import { Timestamp } from '@/common/domain/primitive/timestamp.js';
import { acceptTransaction } from '@/payment/application/commands/acceptTransaction.js';
import { expirePayment } from '@/payment/application/commands/expirePayment.js';
import { refundTransaction } from '@/payment/application/commands/refundTransaction.js';
import { rejectTransaction } from '@/payment/application/commands/rejectTransaction.js';
import { requestPayment } from '@/payment/application/commands/requestPayment.js';
import { startTransaction } from '@/payment/application/commands/startTransaction.js';
import { PaymentId } from '@/payment/domain/payment/paymentId.js';
import { Provider } from '@/payment/domain/payment/provider.js';
import { ProviderReference } from '@/payment/domain/payment/providerReference.js';
import { TransactionId } from '@/payment/domain/payment/transactionId.js';
import { Uuid } from '@/common/domain/primitive/uuid.js';

const A_CLIENT_REFERENCE = new ClientReference(Uuid.fromString('00000000-0000-0000-0000-000000000001'));

const REQUESTED_AT = new Timestamp(new Date('2026-06-07T10:00:00.000Z'));

describe('PaymentCommands', () => {
  describe('RequestPayment validation', () => {
    it('should fail if parameters are null', () => {
      const paymentId = generateId((value) => new PaymentId(value));
      const amount = new Money(10);

      expect(() => requestPayment(null as unknown as PaymentId, A_CLIENT_REFERENCE, amount, REQUESTED_AT)).toThrow();
      expect(() => requestPayment(paymentId, null as unknown as ClientReference, amount, REQUESTED_AT)).toThrow();
      expect(() => requestPayment(paymentId, A_CLIENT_REFERENCE, null as unknown as Money, REQUESTED_AT)).toThrow();
      expect(() => requestPayment(paymentId, A_CLIENT_REFERENCE, amount, null as unknown as Timestamp)).toThrow();
    });
  });

  describe('StartTransaction validation', () => {
    it('should fail if parameters are null', () => {
      const paymentId = generateId((value) => new PaymentId(value));
      const providerReference = new ProviderReference(Uuid.generate());
      const amount = new Money(10);
      const startedAt = REQUESTED_AT;

      expect(() => startTransaction(null as unknown as PaymentId, Provider.GIFT_CARD, providerReference, amount, startedAt)).toThrow();
      expect(() => startTransaction(paymentId, null as unknown as Provider, providerReference, amount, startedAt)).toThrow();
      expect(() => startTransaction(paymentId, Provider.GIFT_CARD, providerReference, null as unknown as Money, startedAt)).toThrow();
      expect(() => startTransaction(paymentId, Provider.GIFT_CARD, providerReference, amount, null as unknown as Timestamp)).toThrow();
    });
  });

  describe('RefundTransaction validation', () => {
    it('should fail if parameters are null', () => {
      const paymentId = generateId((value) => new PaymentId(value));
      const amount = new Money(10);

      expect(() => refundTransaction(null as unknown as PaymentId, amount)).toThrow();
      expect(() => refundTransaction(paymentId, null as unknown as Money)).toThrow();
    });
  });

  describe('RejectTransaction validation', () => {
    it('should fail if parameters are null or blank', () => {
      const paymentId = generateId((value) => new PaymentId(value));
      const transactionId = generateId((value) => new TransactionId(value));

      expect(() => rejectTransaction(null as unknown as PaymentId, transactionId, new Description('reason'))).toThrow();
      expect(() => rejectTransaction(paymentId, null as unknown as TransactionId, new Description('reason'))).toThrow();
      expect(() => rejectTransaction(paymentId, transactionId, null as unknown as Description)).toThrow();
      expect(() => rejectTransaction(paymentId, transactionId, new Description(''))).toThrow();
    });
  });

  describe('AcceptTransaction validation', () => {
    it('should fail if parameters are null', () => {
      const paymentId = generateId((value) => new PaymentId(value));
      const transactionId = generateId((value) => new TransactionId(value));
      const providerCompletedAt = REQUESTED_AT;

      expect(() => acceptTransaction(null as unknown as PaymentId, transactionId, providerCompletedAt)).toThrow();
      expect(() => acceptTransaction(paymentId, null as unknown as TransactionId, providerCompletedAt)).toThrow();
      expect(() => acceptTransaction(paymentId, transactionId, null as unknown as Timestamp)).toThrow();
    });
  });

  describe('ExpirePayment validation', () => {
    it('should fail if aggregate id is null', () => {
      expect(() => expirePayment(null as unknown as PaymentId)).toThrow();
    });
  });
});
