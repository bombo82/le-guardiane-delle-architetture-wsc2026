// Eventi di risultato del ciclo di vita di una prenotazione.


import type { Event } from '@/common/domain/model/event.js';
import type { Money } from '@/common/domain/primitive/money.js';
import type { GiftCardId } from '@/giftcard/domain/giftcard/giftCardId.js';
import type { BookingId } from '../booking/bookingId.js';
import { requireArgument } from '@/common/utils/requireArgument.js';

export type BookingConfirmed = Event<'BookingConfirmed', BookingId> & {
  readonly giftCardId: GiftCardId;
  readonly amount: Money;
};

export function bookingConfirmed(aggregateId: BookingId, giftCardId: GiftCardId, amount: Money): BookingConfirmed {
  requireArgument(aggregateId, 'bookingId');
  requireArgument(giftCardId, 'giftCardId');
  requireArgument(amount, 'amount');
  return { kind: 'BookingConfirmed', aggregateId, giftCardId, amount };
}

export type BookingRefused = Event<'BookingRefused', BookingId> & {
  readonly giftCardId: GiftCardId;
  readonly amount: Money;
};

export function bookingRefused(aggregateId: BookingId, giftCardId: GiftCardId, amount: Money): BookingRefused {
  requireArgument(aggregateId, 'bookingId');
  requireArgument(giftCardId, 'giftCardId');
  requireArgument(amount, 'amount');
  return { kind: 'BookingRefused', aggregateId, giftCardId, amount };
}

export type BookingRejected = Event<'BookingRejected', BookingId> & {
  readonly giftCardId: GiftCardId;
  readonly amount: Money;
};

export function bookingRejected(aggregateId: BookingId, giftCardId: GiftCardId, amount: Money): BookingRejected {
  requireArgument(aggregateId, 'bookingId');
  requireArgument(giftCardId, 'giftCardId');
  requireArgument(amount, 'amount');
  return { kind: 'BookingRejected', aggregateId, giftCardId, amount };
}

export type BookingResultEvent = BookingConfirmed | BookingRefused | BookingRejected;
