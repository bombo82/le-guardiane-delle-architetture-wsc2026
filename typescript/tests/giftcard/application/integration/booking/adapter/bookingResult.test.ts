import { describe, expect, it } from 'vitest';
import { generateId } from '@/common/domain/identity/entityId.js';
import { Money } from '@/common/domain/primitive/money.js';
import {
  bookingCompletedIntegrationEvent,
  bookingRefusedIntegrationEvent,
  bookingRejectedIntegrationEvent,
} from '@/booking/integration/giftcard/bookingResultIntegrationEvent.js';
import { BookingResult } from '@/giftcard/application/integration/booking/adapter/bookingResult.js';
import { GiftCardId } from '@/giftcard/domain/giftcard/giftCardId.js';

describe('BookingResult', () => {
  const bookingResult = new BookingResult();

  it('adapts BookingCompleted to CreditGiftCard', () => {
    const giftCardId = generateId((value) => new GiftCardId(value));
    const amount = new Money(50);
    const event = bookingCompletedIntegrationEvent(giftCardId.value.value, amount);

    const result = bookingResult.adapt(event);

    expect(result).not.toBeNull();
    expect(result!.aggregateId).toEqual(giftCardId);
    expect(result!.amount).toEqual(amount);
  });

  it('adapts BookingRefused to CreditGiftCard', () => {
    const giftCardId = generateId((value) => new GiftCardId(value));
    const amount = new Money(75.5);
    const event = bookingRefusedIntegrationEvent(giftCardId.value.value, amount);

    const result = bookingResult.adapt(event);

    expect(result).not.toBeNull();
    expect(result!.aggregateId).toEqual(giftCardId);
    expect(result!.amount).toEqual(amount);
  });

  it('adapts BookingRejected to null for credit', () => {
    const giftCardId = generateId((value) => new GiftCardId(value));
    const amount = new Money(100);
    const event = bookingRejectedIntegrationEvent(giftCardId.value.value, amount);

    const result = bookingResult.adapt(event);

    expect(result).toBeNull();
  });

  it('adapts BookingRejected to RefundGiftCard', () => {
    const giftCardId = generateId((value) => new GiftCardId(value));
    const amount = new Money(50);
    const event = bookingRejectedIntegrationEvent(giftCardId.value.value, amount);

    const result = bookingResult.adaptRejected(event);

    expect(result).not.toBeNull();
    expect(result.aggregateId).toEqual(giftCardId);
    expect(result.amount).toEqual(amount);
  });

  it('throws on null credit event', () => {
    expect(() => bookingResult.adapt(null as unknown as Parameters<typeof bookingResult.adapt>[0])).toThrow();
  });

  it('throws on null refund event', () => {
    expect(() => bookingResult.adaptRejected(null as unknown as Parameters<typeof bookingResult.adaptRejected>[0])).toThrow();
  });
});
