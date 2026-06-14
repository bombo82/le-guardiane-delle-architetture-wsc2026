// Caso d'uso per la richiesta di ricarica di una gift card.

import type { EventPublisher } from '@/common/application/events/eventPublisher.js';
import type { UseCase } from '@/common/application/usecase.js';
import { GiftCardEvent } from '../../domain/events/giftCardEvent.js';
import { GiftCardTopUpRequested } from '../../domain/events/giftCardTopUpRequested.js';
import { GiftCardId } from '../../domain/giftcard/giftCardId.js';
import type { GiftCardRepository } from '../../domain/ports/giftCardRepository.js';
import { RequestGiftCardTopUp } from '../commands/requestGiftCardTopUp.js';
import { requireArgument } from '@/common/utils/requireArgument.js';
import { requireDependency } from '@/common/utils/requireDependency.js';

export class TopUpRequesting implements UseCase<RequestGiftCardTopUp, GiftCardTopUpRequested> {
  private readonly _giftCardRepository: GiftCardRepository;
  private readonly _eventPublisher: EventPublisher<GiftCardEvent>;

  constructor(giftCardRepository: GiftCardRepository, eventPublisher: EventPublisher<GiftCardEvent>) {
    requireDependency(giftCardRepository, "giftCardRepository");
    requireDependency(eventPublisher, "eventPublisher");
    this._giftCardRepository = giftCardRepository;
    this._eventPublisher = eventPublisher;
  }

  invoke(command: RequestGiftCardTopUp): GiftCardTopUpRequested {
    requireArgument(command, 'command');

    const giftCardId: GiftCardId = command.aggregateId;
    const giftCard = this._giftCardRepository.findById(giftCardId);
    if (giftCard === null) {
      throw new Error(`Gift card not found: ${giftCardId.value}`);
    }

    const topUpRequested = giftCard.requestTopUp(command.amount);

    this._giftCardRepository.save(giftCard);
    this._eventPublisher.publish(topUpRequested);

    return topUpRequested;
  }
}
