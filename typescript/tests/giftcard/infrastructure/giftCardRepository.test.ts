import { beforeAll, describe, expect, it } from 'vitest';
import { generateId } from '@/common/domain/identity/entityId.js';
import { Money } from '@/common/domain/primitive/money.js';
import { GiftCard } from '@/giftcard/domain/giftcard/giftCard.js';
import { GiftCardId } from '@/giftcard/domain/giftcard/giftCardId.js';
import type { GiftCardRepository } from '@/giftcard/domain/ports/giftCardRepository.js';
import { SqliteGiftCardRepository } from '@/giftcard/infrastructure/sqliteGiftCardRepository.js';
import { DatabaseSetup } from '../../testsupport/databaseSetup.js';

describe('GiftCardRepository', () => {
  let repository: GiftCardRepository;

  beforeAll(() => {
    const database = DatabaseSetup.initializeFileDb('giftcard', 'GiftCardRepositoryTest');
    repository = new SqliteGiftCardRepository(database);
  });

  describe('save', () => {
    it('should persist new card', () => {
      const cardId = generateId((value) => new GiftCardId(value));
      const original = GiftCard.issue(cardId);

      repository.save(original);
      const reloaded = repository.findById(cardId);

      expect(reloaded).not.toBeNull();
      expect(reloaded!.id()).toEqual(cardId);
      expect(reloaded!.balance()).toEqual(Money.zero());
    });

    it('should update balance', () => {
      const cardId = generateId((value) => new GiftCardId(value));
      const credited = new GiftCard(cardId, new Money(37.75));

      repository.save(credited);
      const reloaded = repository.findById(cardId);

      expect(reloaded).not.toBeNull();
      expect(reloaded!.balance()).toEqual(new Money(37.75));
    });
  });

  describe('findById', () => {
    it('should return empty when not found', () => {
      const nonExistentId = generateId((value) => new GiftCardId(value));

      const reloaded = repository.findById(nonExistentId);

      expect(reloaded).toBeNull();
    });
  });
});
