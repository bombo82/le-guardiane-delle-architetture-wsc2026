import { beforeAll, describe, expect, it } from 'vitest';
import { generateId } from '@/common/domain/identity/entityId.js';
import { Money } from '@/common/domain/primitive/money.js';
import { CreditGiftCard, creditGiftCard } from '@/giftcard/application/commands/creditGiftCard.js';
import { GiftCardCrediting } from '@/giftcard/application/usecases/giftCardCrediting.js';
import { GiftCardId } from '@/giftcard/domain/giftcard/giftCardId.js';
import { SqliteGiftCardRepository } from '@/giftcard/infrastructure/sqliteGiftCardRepository.js';
import { DatabaseSetup } from '../../../testsupport/databaseSetup.js';
import { GiftCardAggregateFactory } from '../../../testsupport/giftcard/aggregateFactory.js';

describe('GiftCardCrediting', () => {
  let repository: SqliteGiftCardRepository;
  let crediting: GiftCardCrediting;

  beforeAll(() => {
    const database = DatabaseSetup.initializeInMemoryDb('giftcard');
    repository = new SqliteGiftCardRepository(database);
    crediting = new GiftCardCrediting(repository);
  });

  describe('construction', () => {
    it('rejects null repository', () => {
      expect(() => new GiftCardCrediting(null as unknown as SqliteGiftCardRepository)).toThrow();
    });
  });

  describe('execution', () => {
    it('rejects null command', () => {
      expect(() => crediting.invoke(null as unknown as CreditGiftCard)).toThrow();
    });

    it('should persist credit', () => {
      const giftCard = GiftCardAggregateFactory.getSavedGiftCard(repository);
      const creditAmount = new Money(25);

      crediting.invoke(creditGiftCard(giftCard.id(), creditAmount));

      const persisted = repository.findById(giftCard.id());
      expect(persisted).not.toBeNull();
      expect(persisted!.balance()).toEqual(creditAmount);
    });

    it('should fail if card does not exist', () => {
      const nonExisting = generateId((value) => new GiftCardId(value));
      const amount = new Money(10);

      expect(() => crediting.invoke(creditGiftCard(nonExisting, amount))).toThrow();
    });
  });
});
