// Porta del repository per l'aggregato GiftCard.

import type { GiftCard } from '../giftcard/giftCard.js';
import type { GiftCardId } from '../giftcard/giftCardId.js';

export interface GiftCardRepository {
  save(giftCard: GiftCard): void;
  findById(id: GiftCardId): GiftCard | null;
}
