import { describe, expect, it } from 'vitest';
import { GiftCardQueryService } from '@/giftcard/application/query/giftCardQueryService.js';
import { GiftCard } from '@/giftcard/domain/giftcard/giftCard.js';
import { GiftCardId } from '@/giftcard/domain/giftcard/giftCardId.js';
import { GiftCardRepository } from '@/giftcard/domain/ports/giftCardRepository.js';
import { Money } from '@/common/domain/primitive/money.js';
import { generateId } from '@/common/domain/identity/entityId.js';

class InMemoryGiftCardRepository implements GiftCardRepository {
    private readonly cards = new Map<string, GiftCard>();

    save(card: GiftCard): void {
        this.cards.set(card.id().value.value, card);
    }

    findById(id: GiftCardId): GiftCard | null {
        return this.cards.get(id.value.value) ?? null;
    }
}

describe('GiftCardQueryService', () => {
    it('should return gift card details when gift card exists', () => {
        const repository = new InMemoryGiftCardRepository();
        const queryService = new GiftCardQueryService(repository);
        const cardId = generateId((value) => new GiftCardId(value));
        const balance = new Money(42.5);
        const card = new GiftCard(cardId, balance);
        repository.save(card);

        const result = queryService.findById(cardId);

        expect(result).not.toBeNull();
        expect(result!.id).toBe(cardId.value.value);
        expect(result!.balance).toBe(balance);
    });

    it('should return null when gift card does not exist', () => {
        const repository = new InMemoryGiftCardRepository();
        const queryService = new GiftCardQueryService(repository);
        const cardId = generateId((value) => new GiftCardId(value));

        const result = queryService.findById(cardId);

        expect(result).toBeNull();
    });
});
