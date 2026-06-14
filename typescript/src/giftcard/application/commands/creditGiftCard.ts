// Command per accreditare una gift card.

import type { Command } from '@/common/application/command.js';
import type { Money } from '@/common/domain/primitive/money.js';
import type { GiftCardId } from '../../domain/giftcard/giftCardId.js';
import { requireArgument } from '@/common/utils/requireArgument.js';

export type CreditGiftCard = Command<'CreditGiftCard', GiftCardId> & {
  readonly amount: Money;
};

export function creditGiftCard(aggregateId: GiftCardId, amount: Money): CreditGiftCard {
  requireArgument(aggregateId, 'giftCardId');
  requireArgument(amount, 'amount');
  return { kind: 'CreditGiftCard', aggregateId, amount };
}
