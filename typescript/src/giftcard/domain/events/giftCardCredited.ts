// Evento emesso quando una gift card viene accreditata.

import type { Event } from '@/common/domain/model/event.js';
import type { Money } from '@/common/domain/primitive/money.js';
import type { GiftCardId } from '../giftcard/giftCardId.js';
import { requireArgument } from '@/common/utils/requireArgument.js';

export type GiftCardCredited = Event<'GiftCardCredited', GiftCardId> & {
  readonly creditedAmount: Money;
};

export function giftCardCredited(aggregateId: GiftCardId, creditedAmount: Money): GiftCardCredited {
  requireArgument(aggregateId, 'giftCardId');
  requireArgument(creditedAmount, 'creditedAmount');
  return { kind: 'GiftCardCredited', aggregateId, creditedAmount };
}
