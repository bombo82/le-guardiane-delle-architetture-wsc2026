// Command per rimborsare una gift card.

import type { Command } from '@/common/application/command.js';
import type { Money } from '@/common/domain/primitive/money.js';
import type { GiftCardId } from '../../domain/giftcard/giftCardId.js';
import { requireArgument } from '@/common/utils/requireArgument.js';

export type RefundGiftCard = Command<'RefundGiftCard', GiftCardId> & {
  readonly amount: Money;
};

export function refundGiftCard(aggregateId: GiftCardId, amount: Money): RefundGiftCard {
  requireArgument(aggregateId, 'giftCardId');
  requireArgument(amount, 'amount');
  return { kind: 'RefundGiftCard', aggregateId, amount };
}
