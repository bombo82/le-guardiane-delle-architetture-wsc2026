import { describe, expect, it } from 'vitest';
import { generateId } from '@/common/domain/identity/entityId.js';
import { Description } from '@/common/domain/primitive/description.js';
import { Money } from '@/common/domain/primitive/money.js';
import { Booking } from '@/booking/domain/booking/booking.js';
import { BookingId } from '@/booking/domain/booking/bookingId.js';
import { BookingStatus } from '@/booking/domain/booking/bookingStatus.js';
import type { BookingConfirmed } from '@/booking/domain/events/bookingResultEvents.js';
import { GiftCardId } from '@/giftcard/domain/giftcard/giftCardId.js';
import { BookingAggregateFactory } from '../../../testsupport/booking/aggregateFactory.js';

describe('Booking', () => {
  describe('place', () => {
    it('should create booking in placed status', () => {
      const id = generateId((value) => new BookingId(value));
      const description = new Description('Some description');
      const giftCardId = generateId((value) => new GiftCardId(value));

      const booking = Booking.place(id, description, giftCardId);

      expect(booking.id()).toEqual(id);
      expect(booking.description()).toEqual(description);
      expect(booking.giftCardId()).toEqual(giftCardId);
      expect(booking.status()).toEqual(BookingStatus.PLACED);
    });

    it('should fail if parameters are invalid', () => {
      const id = generateId((value) => new BookingId(value));
      const description = new Description('Some description');
      const giftCardId = generateId((value) => new GiftCardId(value));

      expect(() => Booking.place(null as unknown as BookingId, description, giftCardId)).toThrow();
      expect(() => Booking.place(id, null as unknown as Description, giftCardId)).toThrow();
      expect(() => Booking.place(id, description, null as unknown as GiftCardId)).toThrow();
    });
  });

  describe('confirm', () => {
    it('should emit booking confirmed', () => {
      const giftCardId = generateId((value) => new GiftCardId(value));
      const booking = Booking.place(
        generateId((value) => new BookingId(value)),
        new Description('Test booking'),
        giftCardId
      );
      const amount = new Money(75);

      const event = booking.confirm(giftCardId, amount);

      expect(event.kind).toBe('BookingConfirmed');
      const confirmed = event as BookingConfirmed;
      expect(confirmed.aggregateId).toEqual(booking.id());
      expect(confirmed.giftCardId).toEqual(giftCardId);
      expect(confirmed.amount).toEqual(amount);
    });

    it('should fail if parameters are invalid', () => {
      const validGiftCardId = generateId((value) => new GiftCardId(value));
      const booking = BookingAggregateFactory.createBooking();

      expect(() => booking.confirm(null as unknown as GiftCardId, new Money(10))).toThrow();
      expect(() => booking.confirm(validGiftCardId, null as unknown as Money)).toThrow();
    });
  });

  describe('reject', () => {
    it('should emit booking rejected', () => {
      const giftCardId = generateId((value) => new GiftCardId(value));
      const booking = Booking.place(
        generateId((value) => new BookingId(value)),
        new Description('Test booking'),
        giftCardId
      );
      const amount = new Money(25);

      const event = booking.reject(giftCardId, amount);

      expect(event.aggregateId).toEqual(booking.id());
      expect(event.giftCardId).toEqual(giftCardId);
      expect(event.amount).toEqual(amount);
    });

    it('should fail if parameters are invalid', () => {
      const validGiftCardId = generateId((value) => new GiftCardId(value));
      const booking = BookingAggregateFactory.createBooking();

      expect(() => booking.reject(null as unknown as GiftCardId, new Money(10))).toThrow();
      expect(() => booking.reject(validGiftCardId, null as unknown as Money)).toThrow();
    });
  });
});
