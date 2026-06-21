// Query service che espone i dati delle prenotazioni all'API.

import { BookingId } from '../../domain/booking/bookingId.js';
import type { BookingRepository } from '../../domain/ports/bookingRepository.js';
import { type BookingDetails } from './bookingDetails.js';

export class BookingQueryService {
  private readonly _repository: BookingRepository;

  constructor(repository: BookingRepository) {
    this._repository = repository;
  }

  findById(id: BookingId): BookingDetails | null {
    const booking = this._repository.findById(id);
    if (booking === null) {
      return null;
    }
    return {
      id: booking.id().value.value,
      description: booking.description(),
      giftCardId: booking.giftCardId().value.value,
    };
  }
}
