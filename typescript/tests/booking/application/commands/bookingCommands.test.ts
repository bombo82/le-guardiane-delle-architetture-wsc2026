import { describe, expect, it } from 'vitest';
import { generateId } from '@/common/domain/identity/entityId.js';
import { Description } from '@/common/domain/primitive/description.js';
import { Money } from '@/common/domain/primitive/money.js';
import { Uuid } from '@/common/domain/primitive/uuid.js';
import { BookingId } from '@/booking/domain/booking/bookingId.js';
import { placeBooking } from '@/booking/application/commands/placeBooking.js';
import {
  confirmBooking,
  rejectBooking,
} from '@/booking/application/commands/bookingConfirmationCommands.js';
import { GiftCardReference } from '@/booking/domain/primitive/giftCardReference.js';

describe('BookingCommands', () => {
  describe('PlaceBooking validation', () => {
    it('should fail if parameters are null', () => {
      const bookingId = generateId((value) => new BookingId(value));
      const amount = new Money(10);
      const giftCardReference = new GiftCardReference(Uuid.generate());

      expect(() => placeBooking(null as unknown as BookingId, amount, new Description('desc'), giftCardReference)).toThrow();
      expect(() => placeBooking(bookingId, null as unknown as Money, new Description('desc'), giftCardReference)).toThrow();
      expect(() => placeBooking(bookingId, amount, null as unknown as Description, giftCardReference)).toThrow();
      expect(() => placeBooking(bookingId, amount, new Description('desc'), null as unknown as GiftCardReference)).toThrow();
    });
  });

  describe('ConfirmBooking validation', () => {
    it('should fail if parameters are null', () => {
      const bookingId = generateId((value) => new BookingId(value));
      const giftCardReference = new GiftCardReference(Uuid.generate());
      const amount = new Money(10);

      expect(() => confirmBooking(null as unknown as BookingId, giftCardReference, amount)).toThrow();
      expect(() => confirmBooking(bookingId, null as unknown as GiftCardReference, amount)).toThrow();
      expect(() => confirmBooking(bookingId, giftCardReference, null as unknown as Money)).toThrow();
    });
  });

  describe('RejectBooking validation', () => {
    it('should fail if parameters are null', () => {
      const bookingId = generateId((value) => new BookingId(value));
      const giftCardReference = new GiftCardReference(Uuid.generate());
      const amount = new Money(10);

      expect(() => rejectBooking(null as unknown as BookingId, giftCardReference, amount)).toThrow();
      expect(() => rejectBooking(bookingId, null as unknown as GiftCardReference, amount)).toThrow();
      expect(() => rejectBooking(bookingId, giftCardReference, null as unknown as Money)).toThrow();
    });
  });
});
