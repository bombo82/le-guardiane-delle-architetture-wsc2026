// Caso d'uso per l'accredito di una gift card.

import type { UseCase } from '@/common/application/usecase.js';
import type { GiftCardCredited } from '../../domain/events/giftCardCredited.js';
import { GiftCardId } from '../../domain/giftcard/giftCardId.js';
import type { GiftCardRepository } from '../../domain/ports/giftCardRepository.js';
import { CreditGiftCard } from '../commands/creditGiftCard.js';
import { requireArgument } from '@/common/utils/requireArgument.js';
import { requireDependency } from '@/common/utils/requireDependency.js';

export class GiftCardCrediting implements UseCase<CreditGiftCard, GiftCardCredited> {
  private readonly _giftCardRepository: GiftCardRepository;

  constructor(giftCardRepository: GiftCardRepository) {
    requireDependency(giftCardRepository, "giftCardRepository");
    this._giftCardRepository = giftCardRepository;
  }

  invoke(command: CreditGiftCard): GiftCardCredited {
    requireArgument(command, 'command');

    const giftCardId: GiftCardId = command.aggregateId;
    const giftCard = this._giftCardRepository.findById(giftCardId);
    if (giftCard === null) {
      throw new Error(`GiftCard not found for credit: ${giftCardId.value}`);
    }

    const credited = giftCard.credit(command.amount);

    this._giftCardRepository.save(giftCard);
    return credited;
  }
}
