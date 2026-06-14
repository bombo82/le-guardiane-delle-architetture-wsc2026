// Caso d'uso per l'inserimento di una nuova prenotazione.

import type { UseCase } from '@/common/application/usecase.js';
import type { EventPublisher } from '@/common/application/events/eventPublisher.js';
import { Booking } from '../../domain/booking/booking.js';
import type { BookingEvent } from '../../domain/events/bookingEvent.js';
import { BookingPlaced, bookingPlaced } from '../../domain/events/bookingPlaced.js';
import type { BookingRepository } from '../../domain/ports/bookingRepository.js';
import { PlaceBooking } from '../commands/placeBooking.js';
import { requireArgument } from '@/common/utils/requireArgument.js';
import { requireDependency } from '@/common/utils/requireDependency.js';

export class BookingPlacing implements UseCase<PlaceBooking, BookingPlaced> {
  private readonly _bookingRepository: BookingRepository;
  private readonly _eventPublisher: EventPublisher<BookingEvent>;

  constructor(bookingRepository: BookingRepository, eventPublisher: EventPublisher<BookingEvent>) {
    requireDependency(bookingRepository, "bookingRepository");
    requireDependency(eventPublisher, "eventPublisher");
    this._bookingRepository = bookingRepository;
    this._eventPublisher = eventPublisher;
  }

  invoke(command: PlaceBooking): BookingPlaced {
    requireArgument(command, 'command');

    const booking = Booking.place(command.aggregateId, command.description, command.giftCardId);

    this._bookingRepository.save(booking);

    const placed = bookingPlaced(booking.id(), command.amount, booking.description(), booking.giftCardId());
    this._eventPublisher.publish(placed);

    return placed;
  }
}
