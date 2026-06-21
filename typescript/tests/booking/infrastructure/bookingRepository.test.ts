import { beforeAll, describe, expect, it } from 'vitest';
import { generateId } from '@/common/domain/identity/entityId.js';
import { Description } from '@/common/domain/primitive/description.js';
import { Uuid } from '@/common/domain/primitive/uuid.js';
import { Booking } from '@/booking/domain/booking/booking.js';
import { BookingId } from '@/booking/domain/booking/bookingId.js';
import { BookingStatus } from '@/booking/domain/booking/bookingStatus.js';
import { SqliteBookingRepository } from '@/booking/infrastructure/sqliteBookingRepository.js';
import { GiftCardReference } from '@/booking/domain/primitive/giftCardReference.js';
import { DatabaseSetup } from '../../testsupport/databaseSetup.js';

describe('BookingRepository', () => {
  let repository: SqliteBookingRepository;

  beforeAll(() => {
    const database = DatabaseSetup.initializeFileDb('booking', 'BookingRepositoryTest');
    repository = new SqliteBookingRepository(database);
  });

  describe('save', () => {
    it('should persist new booking', () => {
      const bookingId = generateId((value) => new BookingId(value));
      const giftCardReference = new GiftCardReference(Uuid.generate());
      const original = new Booking(bookingId, new Description('Test description'), giftCardReference, BookingStatus.PLACED);

      repository.save(original);
      const reloaded = repository.findById(bookingId);

      expect(reloaded).not.toBeNull();
      expect(reloaded!.id()).toEqual(bookingId);
      expect(reloaded!.description()).toEqual(original.description());
      expect(reloaded!.giftCardReference()).toEqual(giftCardReference);
      expect(reloaded!.status()).toEqual(BookingStatus.PLACED);
    });
  });

  describe('findById', () => {
    it('should return empty when not found', () => {
      const nonExistentId = generateId((value) => new BookingId(value));

      const reloaded = repository.findById(nonExistentId);

      expect(reloaded).toBeNull();
    });
  });
});
