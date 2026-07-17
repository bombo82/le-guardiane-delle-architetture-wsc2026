import type { BookingPlaced } from './bookingPlaced.js';
import type { BookingResultEvent } from './bookingResultEvents.js';

export type BookingEvent = BookingPlaced | BookingResultEvent;
