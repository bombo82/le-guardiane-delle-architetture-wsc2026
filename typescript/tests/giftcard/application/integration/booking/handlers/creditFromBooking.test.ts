import { beforeAll, describe, expect, it } from 'vitest';
import { generateId } from '@/common/domain/identity/entityId.js';
import { Money } from '@/common/domain/primitive/money.js';
import {
  bookingCompletedIntegrationEvent,
  bookingRefusedIntegrationEvent,
  bookingRejectedIntegrationEvent,
} from '@/booking/integration/giftcard/bookingResultIntegrationEvent.js';
import { CreditFromBooking } from '@/giftcard/application/integration/booking/handlers/creditFromBooking.js';
import { GiftCardCrediting } from '@/giftcard/application/usecases/giftCardCrediting.js';
import { BookingResult } from '@/giftcard/application/integration/booking/adapter/bookingResult.js';
import { GiftCardId } from '@/giftcard/domain/giftcard/giftCardId.js';
import { SqliteGiftCardRepository } from '@/giftcard/infrastructure/sqliteGiftCardRepository.js';
import { DatabaseSetup } from '../../../../../testsupport/databaseSetup.js';
import { GiftCardAggregateFactory } from '../../../../../testsupport/giftcard/aggregateFactory.js';

describe('CreditFromBooking', () => {
  let repository: SqliteGiftCardRepository;
  let creditFromBooking: CreditFromBooking;

  beforeAll(() => {
    const database = DatabaseSetup.initializeInMemoryDb('giftcard');
    repository = new SqliteGiftCardRepository(database);
    const bookingResult = new BookingResult();
    const useCase = new GiftCardCrediting(repository);
    creditFromBooking = new CreditFromBooking(bookingResult, useCase);
  });

  describe('construction', () => {
    it('rejects null parameters', () => {
      const bookingResult = new BookingResult();
      const useCase = new GiftCardCrediting(repository);

      expect(() => new CreditFromBooking(null as unknown as BookingResult, useCase)).toThrow();
      expect(() => new CreditFromBooking(bookingResult, null as unknown as GiftCardCrediting)).toThrow();
    });
  });

  describe('booking results handling', () => {
    it('on completed should increase balance', () => {
      const giftCard = GiftCardAggregateFactory.getSavedGiftCard(repository);
      const credit = new Money(14);

      creditFromBooking.handle(
        bookingCompletedIntegrationEvent(giftCard.id().value.value, credit)
      );

      const updated = repository.findById(giftCard.id());
      expect(updated).not.toBeNull();
      expect(updated!.balance()).toEqual(credit);
    });

    it('on refused should increase balance', () => {
      const giftCard = GiftCardAggregateFactory.getSavedGiftCard(repository);
      const credit = new Money(9.99);

      creditFromBooking.handle(
        bookingRefusedIntegrationEvent(giftCard.id().value.value, credit)
      );

      const after = repository.findById(giftCard.id());
      expect(after).not.toBeNull();
      expect(after!.balance()).toEqual(credit);
    });

    it('on rejected should do nothing', () => {
      const giftCard = GiftCardAggregateFactory.getSavedGiftCard(repository);
      const amount = new Money(5);

      creditFromBooking.handle(
        bookingRejectedIntegrationEvent(giftCard.id().value.value, amount)
      );

      const persisted = repository.findById(giftCard.id());
      expect(persisted).not.toBeNull();
      expect(persisted!.balance()).toEqual(Money.zero());
    });

    it('should fail if card does not exist', () => {
      const nonExisting = generateId((value) => new GiftCardId(value));
      const amount = new Money(5);

      expect(() =>
        creditFromBooking.handle(
          bookingCompletedIntegrationEvent(nonExisting.value.value, amount)
        )
      ).toThrow();
    });
  });
});
