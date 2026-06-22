// Published Language esposta dal BC booking verso il BC giftcard.
// Rappresenta il risultato di una prenotazione senza esporre gli eventi di dominio interni di booking.

import type { Money } from '@/common/domain/primitive/money.js';

export type BookingCompletedIntegrationEvent = {
  readonly kind: 'BookingCompletedIntegrationEvent';
  readonly giftCardReference: string;
  readonly amount: Money;
};

export type BookingRefusedIntegrationEvent = {
  readonly kind: 'BookingRefusedIntegrationEvent';
  readonly giftCardReference: string;
  readonly amount: Money;
};

export type BookingRejectedIntegrationEvent = {
  readonly kind: 'BookingRejectedIntegrationEvent';
  readonly giftCardReference: string;
  readonly amount: Money;
};

export type BookingResultIntegrationEvent =
  | BookingCompletedIntegrationEvent
  | BookingRefusedIntegrationEvent
  | BookingRejectedIntegrationEvent;

export function bookingCompletedIntegrationEvent(
  giftCardReference: string,
  amount: Money
): BookingCompletedIntegrationEvent {
  return { kind: 'BookingCompletedIntegrationEvent', giftCardReference, amount };
}

export function bookingRefusedIntegrationEvent(
  giftCardReference: string,
  amount: Money
): BookingRefusedIntegrationEvent {
  return { kind: 'BookingRefusedIntegrationEvent', giftCardReference, amount };
}

export function bookingRejectedIntegrationEvent(
  giftCardReference: string,
  amount: Money
): BookingRejectedIntegrationEvent {
  return { kind: 'BookingRejectedIntegrationEvent', giftCardReference, amount };
}
