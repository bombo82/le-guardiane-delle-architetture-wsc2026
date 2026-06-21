// Command per la conferma o il rifiuto di una prenotazione.


import type { Command } from '@/common/application/command.js';
import type { BookingId } from '../../domain/booking/bookingId.js';
import type { GiftCardReference } from '../../domain/primitive/giftCardReference.js';
import type { Money } from '@/common/domain/primitive/money.js';
import { requireArgument } from '@/common/utils/requireArgument.js';

export type ConfirmBooking = Command<'ConfirmBooking', BookingId> & {
  readonly giftCardReference: GiftCardReference;
  readonly amount: Money;
};

export type RejectBooking = Command<'RejectBooking', BookingId> & {
  readonly giftCardReference: GiftCardReference;
  readonly amount: Money;
};

export type BookingConfirmationCommand = ConfirmBooking | RejectBooking;

export function confirmBooking(aggregateId: BookingId, giftCardReference: GiftCardReference, amount: Money): ConfirmBooking {
  requireArgument(aggregateId, 'bookingId');
  requireArgument(giftCardReference, 'giftCardReference');
  requireArgument(amount, 'amount');
  return { kind: 'ConfirmBooking', aggregateId, giftCardReference, amount };
}

export function rejectBooking(aggregateId: BookingId, giftCardReference: GiftCardReference, amount: Money): RejectBooking {
  requireArgument(aggregateId, 'bookingId');
  requireArgument(giftCardReference, 'giftCardReference');
  requireArgument(amount, 'amount');
  return { kind: 'RejectBooking', aggregateId, giftCardReference, amount };
}
