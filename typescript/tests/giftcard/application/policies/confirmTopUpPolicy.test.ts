import { describe, expect, it } from 'vitest';
import { generateId } from '@/common/domain/identity/entityId.js';
import { ClientReference } from '@/common/domain/primitive/clientReference.js';
import { Description } from '@/common/domain/primitive/description.js';
import { Money } from '@/common/domain/primitive/money.js';
import { Timestamp } from '@/common/domain/primitive/timestamp.js';
import { Payment } from '@/payment/domain/payment/payment.js';
import { PaymentId } from '@/payment/domain/payment/paymentId.js';
import {
  paymentAccepted,
  paymentRejected,
  paymentExpired,
} from '@/payment/domain/events/paymentResultEvents.js';
import { ConfirmTopUpPolicy } from '@/giftcard/application/policies/confirmTopUpPolicy.js';
import { GiftCardId } from '@/giftcard/domain/giftcard/giftCardId.js';
import { Uuid } from '@/common/domain/primitive/uuid.js';

describe('ConfirmTopUpPolicy', () => {
  const policy = new ConfirmTopUpPolicy();

  function createPayment(clientReference: string, amount: Money): Payment {
    return Payment.request(
      generateId((value) => new PaymentId(value)),
      new ClientReference(Uuid.fromString(clientReference)),
      amount,
      Timestamp.now()
    );
  }

  it('on PaymentAccepted with GiftCard provider returns ConfirmTopUp', () => {
    const giftCardId = generateId((value) => new GiftCardId(value));
    const amount = new Money(50);
    const payment = createPayment(giftCardId.value.value, amount);
    const event = paymentAccepted(
      payment.id(),
      payment.clientReference(),
      amount
    );

    const result = policy.evaluate(event);

    expect(result).not.toBeNull();
    expect(result!.aggregateId).toEqual(giftCardId);
    expect(result!.amount).toEqual(amount);
  });

  it('on PaymentAccepted with non-GiftCard provider returns ConfirmTopUp', () => {
    const giftCardId = generateId((value) => new GiftCardId(value));
    const amount = new Money(50);
    const payment = createPayment(giftCardId.value.value, amount);
    const event = paymentAccepted(
      payment.id(),
      payment.clientReference(),
      amount
    );

    const result = policy.evaluate(event);

    expect(result).not.toBeNull();
    expect(result!.aggregateId).toEqual(giftCardId);
    expect(result!.amount).toEqual(amount);
  });

  it('on PaymentRejected returns null', () => {
    const payment = createPayment(crypto.randomUUID(), new Money(50));
    const event = paymentRejected(
      payment.id(),
      payment.clientReference(),
      payment.amount(),
      new Description('declined')
    );

    const result = policy.evaluate(event);

    expect(result).toBeNull();
  });

  it('on PaymentExpired returns null', () => {
    const payment = createPayment(crypto.randomUUID(), new Money(50));
    const event = paymentExpired(
      payment.id(),
      payment.clientReference(),
      payment.amount()
    );

    const result = policy.evaluate(event);

    expect(result).toBeNull();
  });

  it('on null event throws', () => {
    expect(() => policy.evaluate(null as unknown as Parameters<typeof policy.evaluate>[0])).toThrow();
  });
});
