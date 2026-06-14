// Policy che traduce i risultati della prenotazione in rimborsi sulla gift card.
// Violazione cross-BC didattica: GiftCard non dovrebbe importare tipi da Booking.

import type { BookingResultEvent } from '@/booking/domain/events/bookingResultEvents.js';
import type { Policy } from '@/common/domain/model/policy.js';
import { match } from 'ts-pattern';
import { RefundGiftCard, refundGiftCard } from '../../application/commands/refundGiftCard.js';

export class RefundGiftCardPolicy implements Policy<BookingResultEvent, RefundGiftCard> {
  evaluate(event: BookingResultEvent): RefundGiftCard | null {
    return match(event)
      .with(
        { kind: 'BookingRejected' },
        (rejected) => refundGiftCard(rejected.giftCardId, rejected.amount)
      )
      .with({ kind: 'BookingConfirmed' }, () => null)
      .with({ kind: 'BookingRefused' }, () => null)
      .exhaustive();
  }
}
