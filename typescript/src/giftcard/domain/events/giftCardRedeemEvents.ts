// Eventi emessi quando un riscatto della gift card riesce o fallisce.

import { Description } from '@/common/domain/primitive/description.js';
import type { Event } from '@/common/domain/model/event.js';
import type { Money } from '@/common/domain/primitive/money.js';
import type { GiftCardId } from '../giftcard/giftCardId.js';
import { requireArgument } from '@/common/utils/requireArgument.js';

export type GiftCardRedeemed = Event<'GiftCardRedeemed', GiftCardId> & {
  readonly redeemedAmount: Money;
};

export function giftCardRedeemed(aggregateId: GiftCardId, redeemedAmount: Money): GiftCardRedeemed {
  requireArgument(aggregateId, 'giftCardId');
  requireArgument(redeemedAmount, 'redeemedAmount');
  return { kind: 'GiftCardRedeemed', aggregateId, redeemedAmount };
}

export type GiftCardNotRedeemed = Event<'GiftCardNotRedeemed', GiftCardId> & {
  readonly attemptedAmount: Money;
  readonly reason: Description;
};

export function giftCardNotRedeemed(
  aggregateId: GiftCardId,
  attemptedAmount: Money,
  reason: Description
): GiftCardNotRedeemed {
  requireArgument(aggregateId, 'giftCardId');
  requireArgument(attemptedAmount, 'attemptedAmount');
  requireArgument(reason, 'reason');
  return { kind: 'GiftCardNotRedeemed', aggregateId, attemptedAmount, reason };
}

export type GiftCardRedeemEvent = GiftCardRedeemed | GiftCardNotRedeemed;
