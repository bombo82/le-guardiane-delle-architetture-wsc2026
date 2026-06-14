// Evento emesso quando una gift card viene rimborsata.

import type { Event } from '@/common/domain/model/event.js';
import type { Money } from '@/common/domain/primitive/money.js';
import type { GiftCardId } from '../giftcard/giftCardId.js';
import { requireArgument } from '@/common/utils/requireArgument.js';

export type GiftCardRefunded = Event<'GiftCardRefunded', GiftCardId> & {
  readonly refundedAmount: Money;
};

export function giftCardRefunded(aggregateId: GiftCardId, refundedAmount: Money): GiftCardRefunded {
  requireArgument(aggregateId, 'giftCardId');
  requireArgument(refundedAmount, 'refundedAmount');
  return { kind: 'GiftCardRefunded', aggregateId, refundedAmount };
}
