// DTO di risposta per una prenotazione.

import { type BookingDetails } from '../application/query/bookingDetails.js';

export type BookingResponse = {
  readonly id: string;
  readonly description: string;
  readonly giftCardId: string;
};

export function toBookingResponse(booking: BookingDetails): BookingResponse {
  return {
    id: booking.id,
    description: booking.description.value,
    giftCardId: booking.giftCardId,
  };
}
