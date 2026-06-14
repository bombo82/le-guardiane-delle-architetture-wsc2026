import { beforeAll, describe, expect, it } from 'vitest';
import { generateId } from '@/common/domain/identity/entityId.js';
import { Money } from '@/common/domain/primitive/money.js';
import { RefundGiftCard, refundGiftCard } from '@/giftcard/application/commands/refundGiftCard.js';
import { GiftCardRefunding } from '@/giftcard/application/usecases/giftCardRefunding.js';
import { GiftCardId } from '@/giftcard/domain/giftcard/giftCardId.js';
import { SqliteGiftCardRepository } from '@/giftcard/infrastructure/sqliteGiftCardRepository.js';
import { DatabaseSetup } from '../../../testsupport/databaseSetup.js';
import { GiftCardAggregateFactory } from '../../../testsupport/giftcard/aggregateFactory.js';

describe('GiftCardRefunding', () => {
  let repository: SqliteGiftCardRepository;
  let refunding: GiftCardRefunding;

  beforeAll(() => {
    const database = DatabaseSetup.initializeInMemoryDb('giftcard');
    repository = new SqliteGiftCardRepository(database);
    refunding = new GiftCardRefunding(repository);
  });

  describe('construction', () => {
    it('rejects null repository', () => {
      expect(() => new GiftCardRefunding(null as unknown as SqliteGiftCardRepository)).toThrow();
    });
  });

  describe('execution', () => {
    it('rejects null command', () => {
      expect(() => refunding.invoke(null as unknown as RefundGiftCard)).toThrow();
    });

    it('should persist refund', () => {
      const giftCard = GiftCardAggregateFactory.getSavedGiftCard(repository);
      const refundAmount = new Money(18.5);

      refunding.invoke(refundGiftCard(giftCard.id(), refundAmount));

      const persisted = repository.findById(giftCard.id());
      expect(persisted).not.toBeNull();
      expect(persisted!.balance()).toEqual(refundAmount);
    });

    it('should fail if card does not exist', () => {
      const nonExisting = generateId((value) => new GiftCardId(value));
      const amount = new Money(10);

      expect(() => refunding.invoke(refundGiftCard(nonExisting, amount))).toThrow();
    });
  });
});
