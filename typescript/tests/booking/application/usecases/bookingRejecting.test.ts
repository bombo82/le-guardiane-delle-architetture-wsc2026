import { beforeEach, describe, expect, it } from 'vitest';
import { generateId } from '@/common/domain/identity/entityId.js';
import { Money } from '@/common/domain/primitive/money.js';
import { rejectBooking } from '@/booking/application/commands/bookingConfirmationCommands.js';
import { BookingRejecting } from '@/booking/application/usecases/bookingRejecting.js';
import { BookingId } from '@/booking/domain/booking/bookingId.js';
import { SqliteBookingRepository } from '@/booking/infrastructure/sqliteBookingRepository.js';
import { GiftCardId } from '@/giftcard/domain/giftcard/giftCardId.js';
import { DatabaseSetup } from '../../../testsupport/databaseSetup.js';
import { BookingAggregateFactory } from '../../../testsupport/booking/aggregateFactory.js';
import type { BookingEvent } from '@/booking/domain/events/bookingEvent.js';
import type { EventPublisher } from '@/common/application/events/eventPublisher.js';

describe('BookingRejecting', () => {
  let repository: SqliteBookingRepository;
  let rejection: BookingRejecting;

  const noOpPublisher: EventPublisher<BookingEvent> = {
    publish: () => {},
  };

  beforeEach(() => {
    const database = DatabaseSetup.initializeInMemoryDb('booking');
    repository = new SqliteBookingRepository(database);
    rejection = new BookingRejecting(repository, noOpPublisher);
  });

  describe('construction', () => {
    it('reject null repository', () => {
      expect(() => new BookingRejecting(null as unknown as SqliteBookingRepository, noOpPublisher)).toThrow();
    });
  });

  describe('execution', () => {
    it('reject null command', () => {
      expect(() => rejection.invoke(null as unknown as ReturnType<typeof rejectBooking>)).toThrow();
    });

    it('should return booking rejected', () => {
      const booking = BookingAggregateFactory.createBooking();
      repository.save(booking);
      const giftCardId = generateId((value) => new GiftCardId(value));
      const amount = new Money(75);

      const event = rejection.invoke(rejectBooking(booking.id(), giftCardId, amount));

      expect(event.aggregateId).toEqual(booking.id());
      expect(event.giftCardId).toEqual(giftCardId);
      expect(event.amount).toEqual(amount);
    });

    it('should fail if booking not found', () => {
      const nonExistingId = generateId((value) => new BookingId(value));
      const giftCardId = generateId((value) => new GiftCardId(value));

      expect(() => rejection.invoke(rejectBooking(nonExistingId, giftCardId, new Money(10)))).toThrow();
    });
  });
});
