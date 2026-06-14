// Caso d'uso per il rimborso di una gift card.

import type { UseCase } from '@/common/application/usecase.js';
import type { GiftCardRefunded } from '../../domain/events/giftCardRefunded.js';
import { GiftCardId } from '../../domain/giftcard/giftCardId.js';
import type { GiftCardRepository } from '../../domain/ports/giftCardRepository.js';
import { RefundGiftCard } from '../commands/refundGiftCard.js';
import { requireArgument } from '@/common/utils/requireArgument.js';
import { requireDependency } from '@/common/utils/requireDependency.js';

export class GiftCardRefunding implements UseCase<RefundGiftCard, GiftCardRefunded> {
  private readonly _giftCardRepository: GiftCardRepository;

  constructor(giftCardRepository: GiftCardRepository) {
    requireDependency(giftCardRepository, "giftCardRepository");
    this._giftCardRepository = giftCardRepository;
  }

  invoke(command: RefundGiftCard): GiftCardRefunded {
    requireArgument(command, 'command');

    const giftCardId: GiftCardId = command.aggregateId;
    const giftCard = this._giftCardRepository.findById(giftCardId);
    if (giftCard === null) {
      throw new Error(`GiftCard not found for refund: ${giftCardId.value}`);
    }

    const refunded = giftCard.refund(command.amount);

    this._giftCardRepository.save(giftCard);
    return refunded;
  }
}
