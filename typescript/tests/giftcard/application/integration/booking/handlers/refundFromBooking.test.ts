import { beforeAll, describe, expect, it } from 'vitest';
import { generateId } from '@/common/domain/identity/entityId.js';
import { Money } from '@/common/domain/primitive/money.js';
import {
  bookingRejectedIntegrationEvent,
} from '@/booking/integration/giftcard/bookingResultIntegrationEvent.js';
import { RefundFromBooking } from '@/giftcard/application/integration/booking/handlers/refundFromBooking.js';
import { GiftCardRefunding } from '@/giftcard/application/usecases/giftCardRefunding.js';
import { BookingResult } from '@/giftcard/application/integration/booking/adapter/bookingResult.js';
import { GiftCardId } from '@/giftcard/domain/giftcard/giftCardId.js';
import { SqliteGiftCardRepository } from '@/giftcard/infrastructure/sqliteGiftCardRepository.js';
import { DatabaseSetup } from '../../../../../testsupport/databaseSetup.js';
import { GiftCardAggregateFactory } from '../../../../../testsupport/giftcard/aggregateFactory.js';

describe('RefundFromBooking', () => {
  let repository: SqliteGiftCardRepository;
  let refundFromBooking: RefundFromBooking;

  beforeAll(() => {
    const database = DatabaseSetup.initializeInMemoryDb('giftcard');
    repository = new SqliteGiftCardRepository(database);
    const bookingResult = new BookingResult();
    const useCase = new GiftCardRefunding(repository);
    refundFromBooking = new RefundFromBooking(bookingResult, useCase);
  });

  describe('construction', () => {
    it('rejects null parameters', () => {
      const bookingResult = new BookingResult();
      const useCase = new GiftCardRefunding(repository);

      expect(() => new RefundFromBooking(null as unknown as BookingResult, useCase)).toThrow();
      expect(() => new RefundFromBooking(bookingResult, null as unknown as GiftCardRefunding)).toThrow();
    });
  });

  describe('booking results handling', () => {
    it('on rejected should restore balance', () => {
      const card = GiftCardAggregateFactory.getSavedGiftCard(repository);
      const refundAmount = new Money(20);

      refundFromBooking.handle(
        bookingRejectedIntegrationEvent(card.id().value.value, refundAmount)
      );

      const after = repository.findById(card.id());
      expect(after).not.toBeNull();
      expect(after!.balance()).toEqual(refundAmount);
    });

    it('should fail if card does not exist', () => {
      const nonExisting = generateId((value) => new GiftCardId(value));
      const amount = new Money(10);

      expect(() =>
        refundFromBooking.handle(
          bookingRejectedIntegrationEvent(nonExisting.value.value, amount)
        )
      ).toThrow();
    });
  });
});
