import { describe, expect, it } from 'vitest';
import { generateId } from '@/common/domain/identity/entityId.js';
import { Money } from '@/common/domain/primitive/money.js';
import { giftCardTopUpRequested } from '@/giftcard/domain/events/giftCardTopUpRequested.js';
import { GiftCardId } from '@/giftcard/domain/giftcard/giftCardId.js';
import { paymentRequestFromTopUp } from '@/giftcard/application/integration/payment/adapter/paymentRequest.js';

describe('PaymentRequest (giftcard -> payment)', () => {
  it('adapts GiftCardTopUpRequested to PaymentRequestIntegrationCommand', () => {
    const giftCardId = generateId((value) => new GiftCardId(value));
    const requestedAmount = new Money(25);
    const currentBalance = new Money(10);
    const event = giftCardTopUpRequested(giftCardId, requestedAmount, currentBalance);

    const command = paymentRequestFromTopUp(event);

    expect(command.clientReference).toBe(giftCardId.value.value);
    expect(command.amount).toBe(requestedAmount);
  });

  it('throws on null event', () => {
    expect(() => paymentRequestFromTopUp(null as unknown as ReturnType<typeof giftCardTopUpRequested>)).toThrow();
  });
});
