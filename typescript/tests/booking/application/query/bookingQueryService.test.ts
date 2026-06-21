import { describe, expect, it } from 'vitest';
import { BookingQueryService } from '@/booking/application/query/bookingQueryService.js';
import { Booking } from '@/booking/domain/booking/booking.js';
import { BookingId } from '@/booking/domain/booking/bookingId.js';
import { BookingRepository } from '@/booking/domain/ports/bookingRepository.js';
import { Description } from '@/common/domain/primitive/description.js';
import { generateId } from '@/common/domain/identity/entityId.js';
import { GiftCardId } from '@/giftcard/domain/giftcard/giftCardId.js';

class InMemoryBookingRepository implements BookingRepository {
    private readonly bookings = new Map<string, Booking>();

    save(booking: Booking): void {
        this.bookings.set(booking.id().value.value, booking);
    }

    findById(id: BookingId): Booking | null {
        return this.bookings.get(id.value.value) ?? null;
    }
}

describe('BookingQueryService', () => {
    it('should return booking details when booking exists', () => {
        const repository = new InMemoryBookingRepository();
        const queryService = new BookingQueryService(repository);
        const bookingId = generateId((value) => new BookingId(value));
        const giftCardId = generateId((value) => new GiftCardId(value));
        const description = new Description('summer stay');
        const booking = Booking.place(bookingId, description, giftCardId);
        repository.save(booking);

        const result = queryService.findById(bookingId);

        expect(result).not.toBeNull();
        expect(result!.id).toBe(bookingId.value.value);
        expect(result!.description).toBe(description);
        expect(result!.giftCardId).toBe(giftCardId.value.value);
    });

    it('should return null when booking does not exist', () => {
        const repository = new InMemoryBookingRepository();
        const queryService = new BookingQueryService(repository);
        const bookingId = generateId((value) => new BookingId(value));

        const result = queryService.findById(bookingId);

        expect(result).toBeNull();
    });
});
