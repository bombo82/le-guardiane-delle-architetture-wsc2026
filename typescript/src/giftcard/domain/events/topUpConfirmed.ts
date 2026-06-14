// Evento emesso quando una ricarica viene confermata.

import type { Event } from '@/common/domain/model/event.js';
import type { Money } from '@/common/domain/primitive/money.js';
import type { GiftCardId } from '../giftcard/giftCardId.js';
import { requireArgument } from '@/common/utils/requireArgument.js';

export type TopUpConfirmed = Event<'TopUpConfirmed', GiftCardId> & {
  readonly confirmedAmount: Money;
};

export function topUpConfirmed(aggregateId: GiftCardId, confirmedAmount: Money): TopUpConfirmed {
  requireArgument(aggregateId, 'giftCardId');
  requireArgument(confirmedAmount, 'confirmedAmount');
  return { kind: 'TopUpConfirmed', aggregateId, confirmedAmount };
}
