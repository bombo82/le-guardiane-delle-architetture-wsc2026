import { beforeAll, describe, expect, it } from 'vitest';
import { generateId } from '@/common/domain/identity/entityId.js';
import { Money } from '@/common/domain/primitive/money.js';
import { IssueGiftCard, issueGiftCard } from '@/giftcard/application/commands/issueGiftCard.js';
import { GiftCardIssuing } from '@/giftcard/application/usecases/giftCardIssuing.js';
import { GiftCardId } from '@/giftcard/domain/giftcard/giftCardId.js';
import { SqliteGiftCardRepository } from '@/giftcard/infrastructure/sqliteGiftCardRepository.js';
import { DatabaseSetup } from '../../../testsupport/databaseSetup.js';

describe('GiftCardIssuing', () => {
  let repository: SqliteGiftCardRepository;
  let issuance: GiftCardIssuing;

  beforeAll(() => {
    const database = DatabaseSetup.initializeInMemoryDb('giftcard');
    repository = new SqliteGiftCardRepository(database);
    issuance = new GiftCardIssuing(repository);
  });

  describe('construction', () => {
    it('rejects null repository', () => {
      expect(() => new GiftCardIssuing(null as unknown as SqliteGiftCardRepository)).toThrow();
    });
  });

  describe('execution', () => {
    it('rejects null command', () => {
      expect(() => issuance.invoke(null as unknown as IssueGiftCard)).toThrow();
    });

    it('should persist', () => {
      const cardId = generateId((value) => new GiftCardId(value));

      issuance.invoke(issueGiftCard(cardId));

      const loaded = repository.findById(cardId);
      expect(loaded).not.toBeNull();
      expect(loaded!.id()).toEqual(cardId);
      expect(loaded!.balance()).toEqual(Money.zero());
    });
  });
});
