// Evento emesso quando viene richiesta una ricarica della gift card.

import type { Event } from '@/common/domain/model/event.js';
import type { Money } from '@/common/domain/primitive/money.js';
import type { GiftCardId } from '../giftcard/giftCardId.js';
import { requireArgument } from '@/common/utils/requireArgument.js';

export type GiftCardTopUpRequested = Event<'GiftCardTopUpRequested', GiftCardId> & {
  readonly requestedAmount: Money;
  readonly currentBalance: Money;
};

export function giftCardTopUpRequested(
  aggregateId: GiftCardId,
  requestedAmount: Money,
  currentBalance: Money
): GiftCardTopUpRequested {
  requireArgument(aggregateId, 'giftCardId');
  requireArgument(requestedAmount, 'requestedAmount');
  requireArgument(currentBalance, 'currentBalance');
  return { kind: 'GiftCardTopUpRequested', aggregateId, requestedAmount, currentBalance };
}
