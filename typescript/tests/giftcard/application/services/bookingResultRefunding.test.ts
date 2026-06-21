import { beforeAll, describe, expect, it } from 'vitest';
import { generateId } from '@/common/domain/identity/entityId.js';
import { Money } from '@/common/domain/primitive/money.js';
import {
  bookingConfirmed,
  bookingRejected,
} from '@/booking/domain/events/bookingResultEvents.js';
import { BookingId } from '@/booking/domain/booking/bookingId.js';
import { BookingResultRefunding } from '@/giftcard/application/services/bookingResultRefunding.js';
import { GiftCardRefunding } from '@/giftcard/application/usecases/giftCardRefunding.js';
import { RefundGiftCardPolicy } from '@/giftcard/application/policies/refundGiftCardPolicy.js';
import { GiftCardId } from '@/giftcard/domain/giftcard/giftCardId.js';
import { SqliteGiftCardRepository } from '@/giftcard/infrastructure/sqliteGiftCardRepository.js';
import { DatabaseSetup } from '../../../testsupport/databaseSetup.js';
import { GiftCardAggregateFactory } from '../../../testsupport/giftcard/aggregateFactory.js';

describe('BookingResultRefunding', () => {
  let repository: SqliteGiftCardRepository;
  let service: BookingResultRefunding;

  beforeAll(() => {
    const database = DatabaseSetup.initializeInMemoryDb('giftcard');
    repository = new SqliteGiftCardRepository(database);
    const policy = new RefundGiftCardPolicy();
    const useCase = new GiftCardRefunding(repository);
    service = new BookingResultRefunding(policy, useCase);
  });

  function createBooking(): BookingId {
    return generateId((value) => new BookingId(value));
  }

  describe('construction', () => {
    it('rejects null parameters', () => {
      const policy = new RefundGiftCardPolicy();
      const useCase = new GiftCardRefunding(repository);

      expect(() => new BookingResultRefunding(null as unknown as RefundGiftCardPolicy, useCase)).toThrow();
      expect(() => new BookingResultRefunding(policy, null as unknown as GiftCardRefunding)).toThrow();
    });
  });

  describe('booking results handling', () => {
    it('on rejected should restore balance', () => {
      const card = GiftCardAggregateFactory.getSavedGiftCard(repository);
      const bookingId = createBooking();
      const refundAmount = new Money(20);

      service.handleBookingResults(bookingRejected(bookingId, card.id().value.value, refundAmount));

      const after = repository.findById(card.id());
      expect(after).not.toBeNull();
      expect(after!.balance()).toEqual(refundAmount);
    });

    it('on confirmed should do nothing', () => {
      const card = GiftCardAggregateFactory.getSavedGiftCard(repository);
      const bookingId = createBooking();
      const amount = new Money(10);

      service.handleBookingResults(bookingConfirmed(bookingId, card.id().value.value, amount));

      const persisted = repository.findById(card.id());
      expect(persisted).not.toBeNull();
      expect(persisted!.balance()).toEqual(Money.zero());
    });

    it('should fail if card does not exist', () => {
      const nonExisting = generateId((value) => new GiftCardId(value));
      const bookingId = createBooking();
      const amount = new Money(10);

      expect(() =>
        service.handleBookingResults(bookingRejected(bookingId, nonExisting.value.value, amount))
      ).toThrow();
    });
  });
});
