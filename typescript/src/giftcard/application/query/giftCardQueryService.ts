// Query service che espone i dati delle gift card all'API.

import { GiftCardId } from '../../domain/giftcard/giftCardId.js';
import type { GiftCardRepository } from '../../domain/ports/giftCardRepository.js';
import { type GiftCardDetails } from './giftCardDetails.js';

export class GiftCardQueryService {
  private readonly _repository: GiftCardRepository;

  constructor(repository: GiftCardRepository) {
    this._repository = repository;
  }

  findById(id: GiftCardId): GiftCardDetails | null {
    const card = this._repository.findById(id);
    if (card === null) {
      return null;
    }
    return {
      id: card.id().value.value,
      balance: card.balance(),
    };
  }
}
