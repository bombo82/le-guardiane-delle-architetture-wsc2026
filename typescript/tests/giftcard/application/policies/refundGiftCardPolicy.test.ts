import { describe, expect, it } from 'vitest';
import { generateId } from '@/common/domain/identity/entityId.js';
import { Money } from '@/common/domain/primitive/money.js';
import {
  bookingConfirmed,
  bookingRejected,
  bookingRefused,
} from '@/booking/domain/events/bookingResultEvents.js';
import { RefundGiftCardPolicy } from '@/giftcard/application/policies/refundGiftCardPolicy.js';
import { GiftCardId } from '@/giftcard/domain/giftcard/giftCardId.js';
import { GiftCardAggregateFactory } from '../../../testsupport/giftcard/aggregateFactory.js';

describe('RefundGiftCardPolicy', () => {
  const policy = new RefundGiftCardPolicy();

  it('on BookingRejected returns RefundGiftCard', () => {
    const giftCardId = generateId((value) => new GiftCardId(value));
    const amount = new Money(50);
    const bookingId = GiftCardAggregateFactory.createBooking();
    const event = bookingRejected(bookingId, giftCardId, amount);

    const result = policy.evaluate(event);

    expect(result).not.toBeNull();
    expect(result!.aggregateId).toEqual(giftCardId);
    expect(result!.amount).toEqual(amount);
  });

  it('on BookingConfirmed returns null', () => {
    const giftCardId = generateId((value) => new GiftCardId(value));
    const amount = new Money(100);
    const bookingId = GiftCardAggregateFactory.createBooking();
    const event = bookingConfirmed(bookingId, giftCardId, amount);

    const result = policy.evaluate(event);

    expect(result).toBeNull();
  });

  it('on BookingRefused returns null', () => {
    const giftCardId = generateId((value) => new GiftCardId(value));
    const amount = new Money(75.5);
    const bookingId = GiftCardAggregateFactory.createBooking();
    const event = bookingRefused(bookingId, giftCardId, amount);

    const result = policy.evaluate(event);

    expect(result).toBeNull();
  });

  it('on null event throws', () => {
    expect(() => policy.evaluate(null as unknown as Parameters<typeof policy.evaluate>[0])).toThrow();
  });
});
