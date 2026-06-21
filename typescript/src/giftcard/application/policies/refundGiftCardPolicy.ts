// Policy che traduce i risultati della prenotazione in rimborsi sulla gift card.

import type { BookingResultEvent } from '@/booking/domain/events/bookingResultEvents.js';
import type { Policy } from '@/common/application/policy.js';
import { match } from 'ts-pattern';
import { RefundGiftCard, refundGiftCard } from '../../application/commands/refundGiftCard.js';
import { GiftCardId } from '../../domain/giftcard/giftCardId.js';
import { Uuid } from '@/common/domain/primitive/uuid.js';

export class RefundGiftCardPolicy implements Policy<BookingResultEvent, RefundGiftCard> {
  evaluate(event: BookingResultEvent): RefundGiftCard | null {
    return match(event)
      .with(
        { kind: 'BookingRejected' },
        (rejected) => refundGiftCard(new GiftCardId(Uuid.fromString(rejected.giftCardReference)), rejected.amount)
      )
      .with({ kind: 'BookingConfirmed' }, () => null)
      .with({ kind: 'BookingRefused' }, () => null)
      .exhaustive();
  }
}
