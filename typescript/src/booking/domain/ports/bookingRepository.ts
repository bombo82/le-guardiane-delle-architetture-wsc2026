// Porta per la persistenza degli aggregati Booking.

import type { Booking } from '../booking/booking.js';
import type { BookingId } from '../booking/bookingId.js';

export interface BookingRepository {
  save(booking: Booking): void;

  findById(id: BookingId): Booking | null;
}
