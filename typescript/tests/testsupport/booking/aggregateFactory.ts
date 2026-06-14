// Factory di aggregati per i test del Booking Bounded Context.

import { generateId } from '@/common/domain/identity/entityId.js';
import { Description } from '@/common/domain/primitive/description.js';
import { Booking } from '@/booking/domain/booking/booking.js';
import { BookingId } from '@/booking/domain/booking/bookingId.js';
import { GiftCardId } from '@/giftcard/domain/giftcard/giftCardId.js';

export class BookingAggregateFactory {
  static createBooking(): Booking {
    const bookingId = generateId((value) => new BookingId(value));
    const giftCardId = generateId((value) => new GiftCardId(value));
    return Booking.place(bookingId, new Description('Test booking'), giftCardId);
  }
}
