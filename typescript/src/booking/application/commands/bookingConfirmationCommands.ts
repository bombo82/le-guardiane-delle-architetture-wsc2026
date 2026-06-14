// Command per la conferma o il rifiuto di una prenotazione.


import type { Command } from '@/common/application/command.js';
import type { BookingId } from '../../domain/booking/bookingId.js';
import type { GiftCardId } from '@/giftcard/domain/giftcard/giftCardId.js';
import type { Money } from '@/common/domain/primitive/money.js';
import { requireArgument } from '@/common/utils/requireArgument.js';

export type ConfirmBooking = Command<'ConfirmBooking', BookingId> & {
  readonly giftCardId: GiftCardId;
  readonly amount: Money;
};

export type RejectBooking = Command<'RejectBooking', BookingId> & {
  readonly giftCardId: GiftCardId;
  readonly amount: Money;
};

export type BookingConfirmationCommand = ConfirmBooking | RejectBooking;

export function confirmBooking(aggregateId: BookingId, giftCardId: GiftCardId, amount: Money): ConfirmBooking {
  requireArgument(aggregateId, 'bookingId');
  requireArgument(giftCardId, 'giftCardId');
  requireArgument(amount, 'amount');
  return { kind: 'ConfirmBooking', aggregateId, giftCardId, amount };
}

export function rejectBooking(aggregateId: BookingId, giftCardId: GiftCardId, amount: Money): RejectBooking {
  requireArgument(aggregateId, 'bookingId');
  requireArgument(giftCardId, 'giftCardId');
  requireArgument(amount, 'amount');
  return { kind: 'RejectBooking', aggregateId, giftCardId, amount };
}
