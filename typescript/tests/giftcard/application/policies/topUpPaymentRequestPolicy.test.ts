import { describe, expect, it } from 'vitest';
import { Money } from '@/common/domain/primitive/money.js';
import { PaymentId } from '@/payment/domain/payment/paymentId.js';
import { TopUpPaymentRequestPolicy } from '@/giftcard/application/policies/topUpPaymentRequestPolicy.js';
import { GiftCardAggregateFactory } from '../../../testsupport/giftcard/aggregateFactory.js';

describe('TopUpPaymentRequestPolicy', () => {
  it('evaluate with null event throws', () => {
    const policy = new TopUpPaymentRequestPolicy();

    expect(() => policy.evaluate(null as unknown as Parameters<typeof policy.evaluate>[0])).toThrow();
  });

  it('evaluate returns RequestPayment', () => {
    const card = GiftCardAggregateFactory.createGiftCard();
    const event = card.requestTopUp(new Money(25.5));
    const policy = new TopUpPaymentRequestPolicy();

    const cmd = policy.evaluate(event);

    expect(cmd.clientReference.toString()).toEqual(event.aggregateId.value.value);
    expect(cmd.amount).toEqual(event.requestedAmount);
    expect(cmd.aggregateId).toBeInstanceOf(PaymentId);
  });
});
