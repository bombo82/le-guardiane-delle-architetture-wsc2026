// Anti-Corruption Layer: isola `giftcard` dalle evoluzioni del modello interno di `booking`;
// l'unico contratto condiviso è la Published Language `BookingResultIntegrationEvent`.

import type {
  BookingRejectedIntegrationEvent,
  BookingResultIntegrationEvent,
} from '@/booking/integration/giftcard/bookingResultIntegrationEvent.js';
import type { CreditGiftCard } from '@/giftcard/application/commands/creditGiftCard.js';
import type { RefundGiftCard } from '@/giftcard/application/commands/refundGiftCard.js';
import { creditGiftCard } from '@/giftcard/application/commands/creditGiftCard.js';
import { refundGiftCard } from '@/giftcard/application/commands/refundGiftCard.js';
import { GiftCardId } from '@/giftcard/domain/giftcard/giftCardId.js';
import { Uuid } from '@/common/domain/primitive/uuid.js';
import { requireArgument } from '@/common/utils/requireArgument.js';

export class BookingResult {
  adaptCredit(event: BookingResultIntegrationEvent): CreditGiftCard | null {
    requireArgument(event, 'Booking result integration event');

    switch (event.kind) {
      case 'BookingCompletedIntegrationEvent':
      case 'BookingRefusedIntegrationEvent':
        return creditGiftCard(new GiftCardId(Uuid.fromString(event.giftCardReference)), event.amount);
      case 'BookingRejectedIntegrationEvent':
        return null;
    }
  }

  adaptRefund(event: BookingRejectedIntegrationEvent): RefundGiftCard {
    requireArgument(event, 'Booking rejected integration event');
    return refundGiftCard(new GiftCardId(Uuid.fromString(event.giftCardReference)), event.amount);
  }
}
