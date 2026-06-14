// Command per richiedere una ricarica della gift card.

import type { Command } from '@/common/application/command.js';
import type { Money } from '@/common/domain/primitive/money.js';
import type { GiftCardId } from '../../domain/giftcard/giftCardId.js';
import { requireArgument } from '@/common/utils/requireArgument.js';

export type RequestGiftCardTopUp = Command<'RequestGiftCardTopUp', GiftCardId> & {
  readonly amount: Money;
};

export function requestGiftCardTopUp(aggregateId: GiftCardId, amount: Money): RequestGiftCardTopUp {
  requireArgument(aggregateId, 'giftCardId');
  requireArgument(amount, 'amount');
  return { kind: 'RequestGiftCardTopUp', aggregateId, amount };
}
