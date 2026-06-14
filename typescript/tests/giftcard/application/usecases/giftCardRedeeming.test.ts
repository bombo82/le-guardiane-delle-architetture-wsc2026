import { beforeAll, describe, expect, it } from 'vitest';
import { Money } from '@/common/domain/primitive/money.js';
import { RedeemGiftCard, redeemGiftCard } from '@/giftcard/application/commands/redeemGiftCard.js';
import { GiftCardRedeeming } from '@/giftcard/application/usecases/giftCardRedeeming.js';
import { SqliteGiftCardRepository } from '@/giftcard/infrastructure/sqliteGiftCardRepository.js';
import { DatabaseSetup } from '../../../testsupport/databaseSetup.js';
import { GiftCardAggregateFactory } from '../../../testsupport/giftcard/aggregateFactory.js';

describe('GiftCardRedeeming', () => {
  let repository: SqliteGiftCardRepository;
  let redemption: GiftCardRedeeming;

  beforeAll(() => {
    const database = DatabaseSetup.initializeInMemoryDb('giftcard');
    repository = new SqliteGiftCardRepository(database);
    redemption = new GiftCardRedeeming(repository);
  });

  describe('construction', () => {
    it('rejects null repository', () => {
      expect(() => new GiftCardRedeeming(null as unknown as SqliteGiftCardRepository)).toThrow();
    });
  });

  describe('execution', () => {
    it('rejects null command', () => {
      expect(() => redemption.invoke(null as unknown as RedeemGiftCard)).toThrow();
    });

    it('should persist with sufficient balance', () => {
      const giftCard = GiftCardAggregateFactory.getSavedGiftCard(repository, new Money(100));
      redemption.invoke(redeemGiftCard(giftCard.id(), new Money(42)));

      const persisted = repository.findById(giftCard.id());
      expect(persisted).not.toBeNull();
      expect(persisted!.balance()).toEqual(new Money(58));
    });

    it('should not persist with insufficient balance', () => {
      const original = GiftCardAggregateFactory.getSavedGiftCard(repository, new Money(20));

      redemption.invoke(redeemGiftCard(original.id(), new Money(50)));

      const persisted = repository.findById(original.id());
      expect(persisted).not.toBeNull();
      expect(persisted!.balance()).toEqual(new Money(20));
    });
  });
});
