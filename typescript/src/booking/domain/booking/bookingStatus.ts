// Stati del ciclo di vita di una prenotazione.

export const BookingStatus = {
  PLACED: 'PLACED',
  CONFIRMED: 'CONFIRMED',
  REFUSED: 'REFUSED',
  REJECTED: 'REJECTED',
} as const;

export type BookingStatus = (typeof BookingStatus)[keyof typeof BookingStatus];
