import { beforeAll, describe, expect, it } from 'vitest';
import { generateId } from '@/common/domain/identity/entityId.js';
import { Money } from '@/common/domain/primitive/money.js';
import {
  bookingConfirmed,
  bookingRefused,
  bookingRejected,
} from '@/booking/domain/events/bookingResultEvents.js';
import { BookingId } from '@/booking/domain/booking/bookingId.js';
import { BookingResultCrediting } from '@/giftcard/application/services/bookingResultCrediting.js';
import { GiftCardCrediting } from '@/giftcard/application/usecases/giftCardCrediting.js';
import { CreditGiftCardPolicy } from '@/giftcard/application/policies/creditGiftCardPolicy.js';
import { GiftCardId } from '@/giftcard/domain/giftcard/giftCardId.js';
import { SqliteGiftCardRepository } from '@/giftcard/infrastructure/sqliteGiftCardRepository.js';
import { DatabaseSetup } from '../../../testsupport/databaseSetup.js';
import { GiftCardAggregateFactory } from '../../../testsupport/giftcard/aggregateFactory.js';

describe('BookingResultCrediting', () => {
  let repository: SqliteGiftCardRepository;
  let service: BookingResultCrediting;

  beforeAll(() => {
    const database = DatabaseSetup.initializeInMemoryDb('giftcard');
    repository = new SqliteGiftCardRepository(database);
    const policy = new CreditGiftCardPolicy();
    const useCase = new GiftCardCrediting(repository);
    service = new BookingResultCrediting(policy, useCase);
  });

  function createBooking(): BookingId {
    return generateId((value) => new BookingId(value));
  }

  describe('construction', () => {
    it('rejects null parameters', () => {
      const policy = new CreditGiftCardPolicy();
      const useCase = new GiftCardCrediting(repository);

      expect(() => new BookingResultCrediting(null as unknown as CreditGiftCardPolicy, useCase)).toThrow();
      expect(() => new BookingResultCrediting(policy, null as unknown as GiftCardCrediting)).toThrow();
    });
  });

  describe('booking results handling', () => {
    it('on confirmed should increase balance', () => {
      const giftCard = GiftCardAggregateFactory.getSavedGiftCard(repository);
      const bookingId = createBooking();
      const credit = new Money(14);

      service.handleBookingResults(bookingConfirmed(bookingId, giftCard.id().value.value, credit));

      const updated = repository.findById(giftCard.id());
      expect(updated).not.toBeNull();
      expect(updated!.balance()).toEqual(credit);
    });

    it('on refused should increase balance', () => {
      const giftCard = GiftCardAggregateFactory.getSavedGiftCard(repository);
      const bookingId = createBooking();
      const credit = new Money(9.99);

      service.handleBookingResults(bookingRefused(bookingId, giftCard.id().value.value, credit));

      const after = repository.findById(giftCard.id());
      expect(after).not.toBeNull();
      expect(after!.balance()).toEqual(credit);
    });

    it('on rejected should do nothing', () => {
      const giftCard = GiftCardAggregateFactory.getSavedGiftCard(repository);
      const bookingId = createBooking();
      const amount = new Money(5);

      service.handleBookingResults(bookingRejected(bookingId, giftCard.id().value.value, amount));

      const persisted = repository.findById(giftCard.id());
      expect(persisted).not.toBeNull();
      expect(persisted!.balance()).toEqual(Money.zero());
    });

    it('should fail if card does not exist', () => {
      const nonExisting = generateId((value) => new GiftCardId(value));
      const bookingId = createBooking();
      const amount = new Money(5);

      expect(() =>
        service.handleBookingResults(bookingConfirmed(bookingId, nonExisting.value.value, amount))
      ).toThrow();
    });
  });
});
