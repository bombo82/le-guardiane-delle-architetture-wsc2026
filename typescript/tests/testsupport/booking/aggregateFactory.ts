// Factory di aggregati per i test del Booking Bounded Context.

import { generateId } from '@/common/domain/identity/entityId.js';
import { Description } from '@/common/domain/primitive/description.js';
import { Uuid } from '@/common/domain/primitive/uuid.js';
import { Booking } from '@/booking/domain/booking/booking.js';
import { BookingId } from '@/booking/domain/booking/bookingId.js';
import { GiftCardReference } from '@/booking/domain/primitive/giftCardReference.js';

export class BookingAggregateFactory {
  static createBooking(): Booking {
    const bookingId = generateId((value) => new BookingId(value));
    const giftCardReference = new GiftCardReference(Uuid.generate());
    return Booking.place(bookingId, new Description('Test booking'), giftCardReference);
  }
}
