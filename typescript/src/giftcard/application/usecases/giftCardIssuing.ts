// Caso d'uso per l'emissione di una gift card.

import type { UseCase } from '@/common/application/usecase.js';
import { GiftCard } from '../../domain/giftcard/giftCard.js';
import { GiftCardIssued, giftCardIssued } from '../../domain/events/giftCardIssued.js';
import type { GiftCardRepository } from '../../domain/ports/giftCardRepository.js';
import { IssueGiftCard } from '../commands/issueGiftCard.js';
import { requireArgument } from '@/common/utils/requireArgument.js';
import { requireDependency } from '@/common/utils/requireDependency.js';

export class GiftCardIssuing implements UseCase<IssueGiftCard, GiftCardIssued> {
  private readonly _giftCardRepository: GiftCardRepository;

  constructor(giftCardRepository: GiftCardRepository) {
    requireDependency(giftCardRepository, "giftCardRepository");
    this._giftCardRepository = giftCardRepository;
  }

  invoke(command: IssueGiftCard): GiftCardIssued {
    requireArgument(command, 'command');

    const card = GiftCard.issue(command.aggregateId);
    this._giftCardRepository.save(card);

    return giftCardIssued(card.id(), card.balance());
  }
}
