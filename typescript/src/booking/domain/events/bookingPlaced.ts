// Evento emesso quando una prenotazione viene inserita.


import type { Event } from '@/common/domain/model/event.js';
import type { Description } from '@/common/domain/primitive/description.js';
import type { Money } from '@/common/domain/primitive/money.js';
import type { BookingId } from '../booking/bookingId.js';
import { requireArgument } from '@/common/utils/requireArgument.js';

export type BookingPlaced = Event<'BookingPlaced', BookingId> & {
  readonly amount: Money;
  readonly description: Description;
  readonly giftCardReference: string;
};

export function bookingPlaced(
  aggregateId: BookingId,
  amount: Money,
  description: Description,
  giftCardReference: string
): BookingPlaced {
  requireArgument(aggregateId, 'bookingId');
  requireArgument(amount, 'amount');
  requireArgument(description, 'description');
  requireArgument(giftCardReference, 'giftCardReference');
  return { kind: 'BookingPlaced', aggregateId, amount, description, giftCardReference };
}
