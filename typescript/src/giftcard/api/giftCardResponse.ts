// DTO di risposta per una gift card.

import { GiftCard } from '../domain/giftcard/giftCard.js';

export type GiftCardResponse = {
  readonly id: string;
  readonly balance: number;
};

export function toGiftCardResponse(card: GiftCard): GiftCardResponse {
  return {
    id: card.id().value.value,
    balance: card.balance().value,
  };
}
