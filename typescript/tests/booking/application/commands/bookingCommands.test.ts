import { describe, expect, it } from 'vitest';
import { generateId } from '@/common/domain/identity/entityId.js';
import { Description } from '@/common/domain/primitive/description.js';
import { Money } from '@/common/domain/primitive/money.js';
import { BookingId } from '@/booking/domain/booking/bookingId.js';
import { placeBooking } from '@/booking/application/commands/placeBooking.js';
import {
  confirmBooking,
  rejectBooking,
} from '@/booking/application/commands/bookingConfirmationCommands.js';
import { GiftCardId } from '@/giftcard/domain/giftcard/giftCardId.js';

describe('BookingCommands', () => {
  describe('PlaceBooking validation', () => {
    it('should fail if parameters are null', () => {
      const bookingId = generateId((value) => new BookingId(value));
      const amount = new Money(10);
      const giftCardId = generateId((value) => new GiftCardId(value));

      expect(() => placeBooking(null as unknown as BookingId, amount, new Description('desc'), giftCardId)).toThrow();
      expect(() => placeBooking(bookingId, null as unknown as Money, new Description('desc'), giftCardId)).toThrow();
      expect(() => placeBooking(bookingId, amount, null as unknown as Description, giftCardId)).toThrow();
      expect(() => placeBooking(bookingId, amount, new Description('desc'), null as unknown as GiftCardId)).toThrow();
    });
  });

  describe('ConfirmBooking validation', () => {
    it('should fail if parameters are null', () => {
      const bookingId = generateId((value) => new BookingId(value));
      const giftCardId = generateId((value) => new GiftCardId(value));
      const amount = new Money(10);

      expect(() => confirmBooking(null as unknown as BookingId, giftCardId, amount)).toThrow();
      expect(() => confirmBooking(bookingId, null as unknown as GiftCardId, amount)).toThrow();
      expect(() => confirmBooking(bookingId, giftCardId, null as unknown as Money)).toThrow();
    });
  });

  describe('RejectBooking validation', () => {
    it('should fail if parameters are null', () => {
      const bookingId = generateId((value) => new BookingId(value));
      const giftCardId = generateId((value) => new GiftCardId(value));
      const amount = new Money(10);

      expect(() => rejectBooking(null as unknown as BookingId, giftCardId, amount)).toThrow();
      expect(() => rejectBooking(bookingId, null as unknown as GiftCardId, amount)).toThrow();
      expect(() => rejectBooking(bookingId, giftCardId, null as unknown as Money)).toThrow();
    });
  });
});
