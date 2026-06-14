// DTO di risposta per una prenotazione.

import { Booking } from '../domain/booking/booking.js';

export type BookingResponse = {
  readonly id: string;
  readonly description: string;
  readonly giftCardId: string;
};

export function toBookingResponse(booking: Booking): BookingResponse {
  return {
    id: booking.id().value.value,
    description: booking.description().value,
    giftCardId: booking.giftCardId().value.value,
  };
}
