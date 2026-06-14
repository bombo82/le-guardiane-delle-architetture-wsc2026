import { beforeEach, describe, expect, it } from 'vitest';
import { generateId } from '@/common/domain/identity/entityId.js';
import { Money } from '@/common/domain/primitive/money.js';
import { confirmBooking } from '@/booking/application/commands/bookingConfirmationCommands.js';
import { BookingConfirming } from '@/booking/application/usecases/bookingConfirming.js';
import { BookingId } from '@/booking/domain/booking/bookingId.js';
import type { BookingConfirmed } from '@/booking/domain/events/bookingResultEvents.js';
import { SqliteBookingRepository } from '@/booking/infrastructure/sqliteBookingRepository.js';
import { GiftCardId } from '@/giftcard/domain/giftcard/giftCardId.js';
import { DatabaseSetup } from '../../../testsupport/databaseSetup.js';
import { BookingAggregateFactory } from '../../../testsupport/booking/aggregateFactory.js';
import type { BookingEvent } from '@/booking/domain/events/bookingEvent.js';
import type { EventPublisher } from '@/common/application/events/eventPublisher.js';

describe('BookingConfirming', () => {
  let repository: SqliteBookingRepository;
  let confirmation: BookingConfirming;

  const noOpPublisher: EventPublisher<BookingEvent> = {
    publish: () => {},
  };

  beforeEach(() => {
    const database = DatabaseSetup.initializeInMemoryDb('booking');
    repository = new SqliteBookingRepository(database);
    confirmation = new BookingConfirming(repository, noOpPublisher);
  });

  describe('construction', () => {
    it('reject null repository', () => {
      expect(() => new BookingConfirming(null as unknown as SqliteBookingRepository, noOpPublisher)).toThrow();
    });
  });

  describe('execution', () => {
    it('reject null command', () => {
      expect(() => confirmation.invoke(null as unknown as ReturnType<typeof confirmBooking>)).toThrow();
    });

    it('should return booking confirmed', () => {
      const booking = BookingAggregateFactory.createBooking();
      repository.save(booking);
      const giftCardId = generateId((value) => new GiftCardId(value));
      const amount = new Money(75);

      const event = confirmation.invoke(confirmBooking(booking.id(), giftCardId, amount));

      expect(event.kind).toBe('BookingConfirmed');
      const confirmed = event as BookingConfirmed;
      expect(confirmed.aggregateId).toEqual(booking.id());
      expect(confirmed.giftCardId).toEqual(giftCardId);
      expect(confirmed.amount).toEqual(amount);
    });

    it('should fail if booking not found', () => {
      const nonExistingId = generateId((value) => new BookingId(value));
      const giftCardId = generateId((value) => new GiftCardId(value));

      expect(() => confirmation.invoke(confirmBooking(nonExistingId, giftCardId, new Money(10)))).toThrow();
    });
  });
});
