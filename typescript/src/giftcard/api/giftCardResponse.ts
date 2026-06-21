// DTO di risposta per una gift card.

import { type GiftCardDetails } from '../application/query/giftCardDetails.js';

export type GiftCardResponse = {
  readonly id: string;
  readonly balance: number;
};

export function toGiftCardResponse(card: GiftCardDetails): GiftCardResponse {
  return {
    id: card.id,
    balance: card.balance.value,
  };
}
