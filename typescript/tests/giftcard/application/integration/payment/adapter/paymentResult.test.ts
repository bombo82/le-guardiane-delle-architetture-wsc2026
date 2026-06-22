import { describe, expect, it } from 'vitest';
import { generateId } from '@/common/domain/identity/entityId.js';
import { Money } from '@/common/domain/primitive/money.js';
import {
  paymentAcceptedIntegrationEvent,
  paymentExpiredIntegrationEvent,
  paymentRejectedIntegrationEvent,
} from '@/payment/integration/paymentResultIntegrationEvent.js';
import { PaymentResult } from '@/giftcard/application/integration/payment/adapter/paymentResult.js';
import { GiftCardId } from '@/giftcard/domain/giftcard/giftCardId.js';

describe('PaymentResult', () => {
  const paymentResult = new PaymentResult();

  it('adapts PaymentAccepted to ConfirmTopUp', () => {
    const giftCardId = generateId((value) => new GiftCardId(value));
    const amount = new Money(50);
    const event = paymentAcceptedIntegrationEvent(giftCardId.value.value, amount);

    const result = paymentResult.adapt(event);

    expect(result).not.toBeNull();
    expect(result!.aggregateId).toEqual(giftCardId);
    expect(result!.amount).toEqual(amount);
  });

  it('adapts PaymentRejected to null', () => {
    const event = paymentRejectedIntegrationEvent(crypto.randomUUID(), new Money(50), 'declined');

    const result = paymentResult.adapt(event);

    expect(result).toBeNull();
  });

  it('adapts PaymentExpired to null', () => {
    const event = paymentExpiredIntegrationEvent(crypto.randomUUID(), new Money(50));

    const result = paymentResult.adapt(event);

    expect(result).toBeNull();
  });

  it('throws on null event', () => {
    expect(() => paymentResult.adapt(null as unknown as Parameters<typeof paymentResult.adapt>[0])).toThrow();
  });

  it('throws on null accepted event', () => {
    expect(() => paymentResult.adaptAccepted(null as unknown as Parameters<typeof paymentResult.adaptAccepted>[0])).toThrow();
  });
});
