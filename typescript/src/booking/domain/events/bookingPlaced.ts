// Evento emesso quando una prenotazione viene inserita.


import type { Event } from '@/common/domain/model/event.js';
import type { Description } from '@/common/domain/primitive/description.js';
import type { Money } from '@/common/domain/primitive/money.js';
import type { GiftCardId } from '@/giftcard/domain/giftcard/giftCardId.js';
import type { BookingId } from '../booking/bookingId.js';
import { requireArgument } from '@/common/utils/requireArgument.js';

export type BookingPlaced = Event<'BookingPlaced', BookingId> & {
  readonly amount: Money;
  readonly description: Description;
  readonly giftCardId: GiftCardId;
};

export function bookingPlaced(
  aggregateId: BookingId,
  amount: Money,
  description: Description,
  giftCardId: GiftCardId
): BookingPlaced {
  requireArgument(aggregateId, 'bookingId');
  requireArgument(amount, 'amount');
  requireArgument(description, 'description');
  requireArgument(giftCardId, 'giftCardId');
  return { kind: 'BookingPlaced', aggregateId, amount, description, giftCardId };
}
