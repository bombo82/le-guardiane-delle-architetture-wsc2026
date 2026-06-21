// Command per inserire una nuova prenotazione.


import type { Command } from '@/common/application/command.js';
import type { Description } from '@/common/domain/primitive/description.js';
import type { Money } from '@/common/domain/primitive/money.js';
import type { GiftCardReference } from '../../domain/primitive/giftCardReference.js';
import type { BookingId } from '../../domain/booking/bookingId.js';
import { requireArgument } from '@/common/utils/requireArgument.js';

export type PlaceBooking = Command<'PlaceBooking', BookingId> & {
  readonly amount: Money;
  readonly description: Description;
  readonly giftCardReference: GiftCardReference;
};

export function placeBooking(
  aggregateId: BookingId,
  amount: Money,
  description: Description,
  giftCardReference: GiftCardReference
): PlaceBooking {
  requireArgument(aggregateId, 'bookingId');
  requireArgument(amount, 'amount');
  requireArgument(description, 'description');
  requireArgument(giftCardReference, 'giftCardReference');
  return { kind: 'PlaceBooking', aggregateId, amount, description, giftCardReference };
}
