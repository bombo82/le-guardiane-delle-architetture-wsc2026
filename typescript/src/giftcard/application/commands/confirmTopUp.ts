// Command per confermare una ricarica sulla gift card.

import type { Command } from '@/common/application/command.js';
import type { Money } from '@/common/domain/primitive/money.js';
import type { GiftCardId } from '../../domain/giftcard/giftCardId.js';
import { requireArgument } from '@/common/utils/requireArgument.js';

export type ConfirmTopUp = Command<'ConfirmTopUp', GiftCardId> & {
  readonly amount: Money;
};

export function confirmTopUp(aggregateId: GiftCardId, amount: Money): ConfirmTopUp {
  requireArgument(aggregateId, 'giftCardId');
  requireArgument(amount, 'amount');
  return { kind: 'ConfirmTopUp', aggregateId, amount };
}
