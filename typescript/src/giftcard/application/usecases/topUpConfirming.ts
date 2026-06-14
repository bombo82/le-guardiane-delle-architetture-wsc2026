// Caso d'uso per la conferma della ricarica di una gift card.

import type { UseCase } from '@/common/application/usecase.js';
import type { TopUpConfirmed } from '../../domain/events/topUpConfirmed.js';
import { GiftCardId } from '../../domain/giftcard/giftCardId.js';
import type { GiftCardRepository } from '../../domain/ports/giftCardRepository.js';
import { ConfirmTopUp } from '../commands/confirmTopUp.js';
import { requireArgument } from '@/common/utils/requireArgument.js';
import { requireDependency } from '@/common/utils/requireDependency.js';

export class TopUpConfirming implements UseCase<ConfirmTopUp, TopUpConfirmed> {
  private readonly _giftCardRepository: GiftCardRepository;

  constructor(giftCardRepository: GiftCardRepository) {
    requireDependency(giftCardRepository, "giftCardRepository");
    this._giftCardRepository = giftCardRepository;
  }

  invoke(command: ConfirmTopUp): TopUpConfirmed {
    requireArgument(command, 'command');

    const giftCardId: GiftCardId = command.aggregateId;
    const giftCard = this._giftCardRepository.findById(giftCardId);
    if (giftCard === null) {
      throw new Error(`GiftCard not found for top-up confirmation: ${giftCardId.value}`);
    }

    const confirmed = giftCard.confirmTopUp(command.amount);

    this._giftCardRepository.save(giftCard);
    return confirmed;
  }
}
