// Policy che traduce i risultati della prenotazione in accrediti sulla gift card.

import type { BookingResultEvent } from '@/booking/domain/events/bookingResultEvents.js';
import type { Policy } from '@/common/application/policy.js';
import { match } from 'ts-pattern';
import { CreditGiftCard, creditGiftCard } from '../../application/commands/creditGiftCard.js';
import { GiftCardId } from '../../domain/giftcard/giftCardId.js';
import { Uuid } from '@/common/domain/primitive/uuid.js';

export class CreditGiftCardPolicy implements Policy<BookingResultEvent, CreditGiftCard> {
  evaluate(event: BookingResultEvent): CreditGiftCard | null {
    return match(event)
      .with(
        { kind: 'BookingConfirmed' },
        (confirmed) => creditGiftCard(new GiftCardId(Uuid.fromString(confirmed.giftCardReference)), confirmed.amount)
      )
      .with(
        { kind: 'BookingRefused' },
        (refused) => creditGiftCard(new GiftCardId(Uuid.fromString(refused.giftCardReference)), refused.amount)
      )
      .with({ kind: 'BookingRejected' }, () => null)
      .exhaustive();
  }
}
