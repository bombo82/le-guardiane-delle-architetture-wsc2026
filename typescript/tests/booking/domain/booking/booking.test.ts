import { describe, expect, it } from 'vitest';
import { generateId } from '@/common/domain/identity/entityId.js';
import { Description } from '@/common/domain/primitive/description.js';
import { Money } from '@/common/domain/primitive/money.js';
import { Uuid } from '@/common/domain/primitive/uuid.js';
import { Booking } from '@/booking/domain/booking/booking.js';
import { BookingId } from '@/booking/domain/booking/bookingId.js';
import { BookingStatus } from '@/booking/domain/booking/bookingStatus.js';
import type { BookingConfirmed } from '@/booking/domain/events/bookingResultEvents.js';
import { GiftCardReference } from '@/booking/domain/primitive/giftCardReference.js';
import { BookingAggregateFactory } from '../../../testsupport/booking/aggregateFactory.js';

describe('Booking', () => {
  describe('place', () => {
    it('should create booking in placed status', () => {
      const id = generateId((value) => new BookingId(value));
      const description = new Description('Some description');
      const giftCardReference = new GiftCardReference(Uuid.generate());

      const booking = Booking.place(id, description, giftCardReference);

      expect(booking.id()).toEqual(id);
      expect(booking.description()).toEqual(description);
      expect(booking.giftCardReference()).toEqual(giftCardReference);
      expect(booking.status()).toEqual(BookingStatus.PLACED);
    });

    it('should fail if parameters are invalid', () => {
      const id = generateId((value) => new BookingId(value));
      const description = new Description('Some description');
      const giftCardReference = new GiftCardReference(Uuid.generate());

      expect(() => Booking.place(null as unknown as BookingId, description, giftCardReference)).toThrow();
      expect(() => Booking.place(id, null as unknown as Description, giftCardReference)).toThrow();
      expect(() => Booking.place(id, description, null as unknown as GiftCardReference)).toThrow();
    });
  });

  describe('confirm', () => {
    it('should emit booking confirmed', () => {
      const giftCardReference = new GiftCardReference(Uuid.generate());
      const booking = Booking.place(
        generateId((value) => new BookingId(value)),
        new Description('Test booking'),
        giftCardReference
      );
      const amount = new Money(75);

      const event = booking.confirm(amount);

      expect(event.kind).toBe('BookingConfirmed');
      const confirmed = event as BookingConfirmed;
      expect(confirmed.aggregateId).toEqual(booking.id());
      expect(confirmed.giftCardReference).toEqual(giftCardReference.value.value);
      expect(confirmed.amount).toEqual(amount);
    });

    it('should fail if parameters are invalid', () => {
      const booking = BookingAggregateFactory.createBooking();

      expect(() => booking.confirm(null as unknown as Money)).toThrow();
    });
  });

  describe('reject', () => {
    it('should emit booking rejected', () => {
      const giftCardReference = new GiftCardReference(Uuid.generate());
      const booking = Booking.place(
        generateId((value) => new BookingId(value)),
        new Description('Test booking'),
        giftCardReference
      );
      const amount = new Money(25);

      const event = booking.reject(amount);

      expect(event.aggregateId).toEqual(booking.id());
      expect(event.giftCardReference).toEqual(giftCardReference.value.value);
      expect(event.amount).toEqual(amount);
    });

    it('should fail if parameters are invalid', () => {
      const booking = BookingAggregateFactory.createBooking();

      expect(() => booking.reject(null as unknown as Money)).toThrow();
    });
  });
});
