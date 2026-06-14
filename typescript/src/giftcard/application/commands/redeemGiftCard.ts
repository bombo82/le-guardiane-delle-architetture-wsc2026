// Command per riscattare una gift card.

import type { Command } from '@/common/application/command.js';
import type { Money } from '@/common/domain/primitive/money.js';
import type { GiftCardId } from '../../domain/giftcard/giftCardId.js';
import { requireArgument } from '@/common/utils/requireArgument.js';

export type RedeemGiftCard = Command<'RedeemGiftCard', GiftCardId> & {
  readonly amount: Money;
};

export function redeemGiftCard(aggregateId: GiftCardId, amount: Money): RedeemGiftCard {
  requireArgument(aggregateId, 'giftCardId');
  requireArgument(amount, 'amount');
  return { kind: 'RedeemGiftCard', aggregateId, amount };
}
