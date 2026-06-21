import { beforeEach, describe, expect, it } from 'vitest';
import { generateId } from '@/common/domain/identity/entityId.js';
import { Description } from '@/common/domain/primitive/description.js';
import { Money } from '@/common/domain/primitive/money.js';
import { Uuid } from '@/common/domain/primitive/uuid.js';
import { placeBooking, type PlaceBooking } from '@/booking/application/commands/placeBooking.js';
import { BookingPlacing } from '@/booking/application/usecases/bookingPlacing.js';
import { BookingId } from '@/booking/domain/booking/bookingId.js';
import { SqliteBookingRepository } from '@/booking/infrastructure/sqliteBookingRepository.js';
import { GiftCardReference } from '@/booking/domain/primitive/giftCardReference.js';
import { DatabaseSetup } from '../../../testsupport/databaseSetup.js';
import type { BookingEvent } from '@/booking/domain/events/bookingEvent.js';
import type { EventPublisher } from '@/common/application/events/eventPublisher.js';

describe('BookingPlacing', () => {
  let repository: SqliteBookingRepository;
  let placement: BookingPlacing;

  const noOpPublisher: EventPublisher<BookingEvent> = {
    publish: () => {},
  };

  beforeEach(() => {
    const database = DatabaseSetup.initializeInMemoryDb('booking');
    repository = new SqliteBookingRepository(database);
    placement = new BookingPlacing(repository, noOpPublisher);
  });

  describe('construction', () => {
    it('reject null repository', () => {
      expect(() => new BookingPlacing(null as unknown as SqliteBookingRepository, noOpPublisher)).toThrow();
    });
  });

  describe('execution', () => {
    it('reject null command', () => {
      expect(() => placement.invoke(null as unknown as PlaceBooking)).toThrow();
    });

    it('should persist', () => {
      const bookingId = generateId((value) => new BookingId(value));
      const amount = new Money(123.45);
      const description = new Description('Some description');
      const giftCardReference = new GiftCardReference(Uuid.generate());

      placement.invoke(placeBooking(bookingId, amount, description, giftCardReference));

      const loaded = repository.findById(bookingId);
      expect(loaded).not.toBeNull();
      expect(loaded!.id()).toEqual(bookingId);
      expect(loaded!.description()).toEqual(description);
      expect(loaded!.giftCardReference()).toEqual(giftCardReference);
    });
  });
});
