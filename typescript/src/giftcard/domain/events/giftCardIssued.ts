// Evento emesso quando una gift card viene emessa.

import type { Event } from '@/common/domain/model/event.js';
import type { Money } from '@/common/domain/primitive/money.js';
import type { GiftCardId } from '../giftcard/giftCardId.js';
import { requireArgument } from '@/common/utils/requireArgument.js';

export type GiftCardIssued = Event<'GiftCardIssued', GiftCardId> & {
  readonly balance: Money;
};

export function giftCardIssued(aggregateId: GiftCardId, balance: Money): GiftCardIssued {
  requireArgument(aggregateId, 'giftCardId');
  requireArgument(balance, 'balance');
  return { kind: 'GiftCardIssued', aggregateId, balance };
}
