import { beforeAll, describe, expect, it } from 'vitest';
import { generateId } from '@/common/domain/identity/entityId.js';
import { Money } from '@/common/domain/primitive/money.js';
import { RequestGiftCardTopUp, requestGiftCardTopUp } from '@/giftcard/application/commands/requestGiftCardTopUp.js';
import { TopUpRequesting } from '@/giftcard/application/usecases/topUpRequesting.js';
import { GiftCardId } from '@/giftcard/domain/giftcard/giftCardId.js';
import { SqliteGiftCardRepository } from '@/giftcard/infrastructure/sqliteGiftCardRepository.js';
import { DatabaseSetup } from '../../../testsupport/databaseSetup.js';
import { GiftCardAggregateFactory } from '../../../testsupport/giftcard/aggregateFactory.js';

describe('TopUpRequesting', () => {
  let repository: SqliteGiftCardRepository;
  let topUp: TopUpRequesting;

  beforeAll(() => {
    const database = DatabaseSetup.initializeInMemoryDb('giftcard');
    repository = new SqliteGiftCardRepository(database);
    topUp = new TopUpRequesting(repository, { publish: () => {} });
  });

  describe('construction', () => {
    it('rejects null repository', () => {
      expect(() => new TopUpRequesting(null as unknown as SqliteGiftCardRepository, { publish: () => {} })).toThrow();
    });
  });

  describe('execution', () => {
    it('rejects null command', () => {
      expect(() => topUp.invoke(null as unknown as RequestGiftCardTopUp)).toThrow();
    });

    it('should persist without changing balance', () => {
      const original = GiftCardAggregateFactory.getSavedGiftCard(repository);
      const requested = new Money(42);

      topUp.invoke(requestGiftCardTopUp(original.id(), requested));

      const persisted = repository.findById(original.id());
      expect(persisted).not.toBeNull();
      expect(persisted!.balance()).toEqual(Money.zero());
    });

    it('should fail if card does not exist', () => {
      const nonExisting = generateId((value) => new GiftCardId(value));
      const cmd = requestGiftCardTopUp(nonExisting, new Money(10));

      expect(() => topUp.invoke(cmd)).toThrow();
    });
  });
});
