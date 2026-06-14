// Caso d'uso per il riscatto di una gift card.

import type { UseCase } from '@/common/application/usecase.js';
import type { GiftCardRedeemEvent } from '../../domain/events/giftCardRedeemEvents.js';
import { GiftCardId } from '../../domain/giftcard/giftCardId.js';
import type { GiftCardRepository } from '../../domain/ports/giftCardRepository.js';
import { RedeemGiftCard } from '../commands/redeemGiftCard.js';
import { requireArgument } from '@/common/utils/requireArgument.js';
import { requireDependency } from '@/common/utils/requireDependency.js';

export class GiftCardRedeeming implements UseCase<RedeemGiftCard, GiftCardRedeemEvent> {
  private readonly _giftCardRepository: GiftCardRepository;

  constructor(giftCardRepository: GiftCardRepository) {
    requireDependency(giftCardRepository, "giftCardRepository");
    this._giftCardRepository = giftCardRepository;
  }

  invoke(command: RedeemGiftCard): GiftCardRedeemEvent {
    requireArgument(command, 'command');

    const giftCardId: GiftCardId = command.aggregateId;
    const giftCard = this._giftCardRepository.findById(giftCardId);
    if (giftCard === null) {
      throw new Error(`GiftCard not found for redeem: ${giftCardId.value}`);
    }

    const redeemResult = giftCard.redeem(command.amount);

    this._giftCardRepository.save(giftCard);
    return redeemResult;
  }
}
