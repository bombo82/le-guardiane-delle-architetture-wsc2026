// Factory di aggregati per i test del GiftCard Bounded Context.

import { generateId } from '@/common/domain/identity/entityId.js';
import { Money } from '@/common/domain/primitive/money.js';
import { BookingId } from '@/booking/domain/booking/bookingId.js';
import { GiftCard } from '@/giftcard/domain/giftcard/giftCard.js';
import { GiftCardId } from '@/giftcard/domain/giftcard/giftCardId.js';
import type { GiftCardRepository } from '@/giftcard/domain/ports/giftCardRepository.js';

export class GiftCardAggregateFactory {
  static createGiftCard(balance: Money = Money.zero()): GiftCard {
    const cardId = generateId((value) => new GiftCardId(value));
    const giftCard = GiftCard.issue(cardId);
    if (balance.isPositive()) {
      giftCard.confirmTopUp(balance);
    }
    return giftCard;
  }

  static getSavedGiftCard(repository: GiftCardRepository, balance: Money = Money.zero()): GiftCard {
    const giftCard = GiftCardAggregateFactory.createGiftCard(balance);
    repository.save(giftCard);
    return giftCard;
  }

  static createBooking(): BookingId {
    return generateId((value) => new BookingId(value));
  }
}
