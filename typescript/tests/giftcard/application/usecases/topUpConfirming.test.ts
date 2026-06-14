import { beforeAll, describe, expect, it } from 'vitest';
import { generateId } from '@/common/domain/identity/entityId.js';
import { Money } from '@/common/domain/primitive/money.js';
import { ConfirmTopUp, confirmTopUp } from '@/giftcard/application/commands/confirmTopUp.js';
import { TopUpConfirming } from '@/giftcard/application/usecases/topUpConfirming.js';
import { GiftCardId } from '@/giftcard/domain/giftcard/giftCardId.js';
import { SqliteGiftCardRepository } from '@/giftcard/infrastructure/sqliteGiftCardRepository.js';
import { DatabaseSetup } from '../../../testsupport/databaseSetup.js';
import { GiftCardAggregateFactory } from '../../../testsupport/giftcard/aggregateFactory.js';

describe('TopUpConfirming', () => {
  let repository: SqliteGiftCardRepository;
  let confirming: TopUpConfirming;

  beforeAll(() => {
    const database = DatabaseSetup.initializeInMemoryDb('giftcard');
    repository = new SqliteGiftCardRepository(database);
    confirming = new TopUpConfirming(repository);
  });

  describe('construction', () => {
    it('rejects null repository', () => {
      expect(() => new TopUpConfirming(null as unknown as SqliteGiftCardRepository)).toThrow();
    });
  });

  describe('execution', () => {
    it('rejects null command', () => {
      expect(() => confirming.invoke(null as unknown as ConfirmTopUp)).toThrow();
    });

    it('should persist top-up confirmation', () => {
      const giftCard = GiftCardAggregateFactory.getSavedGiftCard(repository);
      const topUpAmount = new Money(33);

      confirming.invoke(confirmTopUp(giftCard.id(), topUpAmount));

      const persisted = repository.findById(giftCard.id());
      expect(persisted).not.toBeNull();
      expect(persisted!.balance()).toEqual(topUpAmount);
    });

    it('should fail if card does not exist', () => {
      const nonExisting = generateId((value) => new GiftCardId(value));
      const amount = new Money(10);

      expect(() => confirming.invoke(confirmTopUp(nonExisting, amount))).toThrow();
    });
  });
});
