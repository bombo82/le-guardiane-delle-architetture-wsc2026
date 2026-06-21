// Eventi di risultato del ciclo di vita di una prenotazione.


import type { Event } from '@/common/domain/model/event.js';
import type { Money } from '@/common/domain/primitive/money.js';
import type { BookingId } from '../booking/bookingId.js';
import { requireArgument } from '@/common/utils/requireArgument.js';

export type BookingConfirmed = Event<'BookingConfirmed', BookingId> & {
  readonly giftCardReference: string;
  readonly amount: Money;
};

export function bookingConfirmed(aggregateId: BookingId, giftCardReference: string, amount: Money): BookingConfirmed {
  requireArgument(aggregateId, 'bookingId');
  requireArgument(giftCardReference, 'giftCardReference');
  requireArgument(amount, 'amount');
  return { kind: 'BookingConfirmed', aggregateId, giftCardReference, amount };
}

export type BookingRefused = Event<'BookingRefused', BookingId> & {
  readonly giftCardReference: string;
  readonly amount: Money;
};

export function bookingRefused(aggregateId: BookingId, giftCardReference: string, amount: Money): BookingRefused {
  requireArgument(aggregateId, 'bookingId');
  requireArgument(giftCardReference, 'giftCardReference');
  requireArgument(amount, 'amount');
  return { kind: 'BookingRefused', aggregateId, giftCardReference, amount };
}

export type BookingRejected = Event<'BookingRejected', BookingId> & {
  readonly giftCardReference: string;
  readonly amount: Money;
};

export function bookingRejected(aggregateId: BookingId, giftCardReference: string, amount: Money): BookingRejected {
  requireArgument(aggregateId, 'bookingId');
  requireArgument(giftCardReference, 'giftCardReference');
  requireArgument(amount, 'amount');
  return { kind: 'BookingRejected', aggregateId, giftCardReference, amount };
}

export type BookingResultEvent = BookingConfirmed | BookingRefused | BookingRejected;
