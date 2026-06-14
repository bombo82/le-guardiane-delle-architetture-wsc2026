// Command per emettere una nuova gift card.

import type { Command } from '@/common/application/command.js';
import type { GiftCardId } from '../../domain/giftcard/giftCardId.js';
import { requireArgument } from '@/common/utils/requireArgument.js';

export type IssueGiftCard = Command<'IssueGiftCard', GiftCardId>;

export function issueGiftCard(aggregateId: GiftCardId): IssueGiftCard {
  requireArgument(aggregateId, 'giftCardId');
  return { kind: 'IssueGiftCard', aggregateId };
}
