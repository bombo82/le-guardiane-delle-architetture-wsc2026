// Policy che traduce i risultati della prenotazione in accrediti sulla gift card.
// Violazione cross-BC didattica: GiftCard non dovrebbe importare tipi da Booking.

import type { BookingResultEvent } from '@/booking/domain/events/bookingResultEvents.js';
import type { Policy } from '@/common/application/policy.js';
import { match } from 'ts-pattern';
import { CreditGiftCard, creditGiftCard } from '../../application/commands/creditGiftCard.js';

export class CreditGiftCardPolicy implements Policy<BookingResultEvent, CreditGiftCard> {
  evaluate(event: BookingResultEvent): CreditGiftCard | null {
    return match(event)
      .with(
        { kind: 'BookingConfirmed' },
        (confirmed) => creditGiftCard(confirmed.giftCardId, confirmed.amount)
      )
      .with(
        { kind: 'BookingRefused' },
        (refused) => creditGiftCard(refused.giftCardId, refused.amount)
      )
      .with({ kind: 'BookingRejected' }, () => null)
      .exhaustive();
  }
}
