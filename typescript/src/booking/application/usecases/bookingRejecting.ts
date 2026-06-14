// Caso d'uso per il rifiuto di una prenotazione.

import type { UseCase } from '@/common/application/usecase.js';
import type { EventPublisher } from '@/common/application/events/eventPublisher.js';
import { Booking } from '../../domain/booking/booking.js';
import { BookingId } from '../../domain/booking/bookingId.js';
import type { BookingEvent } from '../../domain/events/bookingEvent.js';
import type { BookingRejected } from '../../domain/events/bookingResultEvents.js';
import type { BookingRepository } from '../../domain/ports/bookingRepository.js';
import type { RejectBooking } from '../commands/bookingConfirmationCommands.js';
import { requireArgument } from '@/common/utils/requireArgument.js';
import { requireDependency } from '@/common/utils/requireDependency.js';

export class BookingRejecting implements UseCase<RejectBooking, BookingRejected> {
  private readonly _bookingRepository: BookingRepository;
  private readonly _eventPublisher: EventPublisher<BookingEvent>;

  constructor(bookingRepository: BookingRepository, eventPublisher: EventPublisher<BookingEvent>) {
    requireDependency(bookingRepository, "bookingRepository");
    requireDependency(eventPublisher, "eventPublisher");
    this._bookingRepository = bookingRepository;
    this._eventPublisher = eventPublisher;
  }

  invoke(command: RejectBooking): BookingRejected {
    requireArgument(command, 'command');

    const booking = this.findBooking(command.aggregateId);

    const rejected = booking.reject(command.giftCardId, command.amount);

    this._bookingRepository.save(booking);
    this._eventPublisher.publish(rejected);
    return rejected;
  }

  private findBooking(id: BookingId): Booking {
    const booking = this._bookingRepository.findById(id);
    if (booking === null) {
      throw new Error(`Booking not found for rejection: ${id.value}`);
    }
    return booking;
  }
}
