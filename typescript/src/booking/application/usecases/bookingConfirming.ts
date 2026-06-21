// Caso d'uso per la conferma di una prenotazione.

import type { UseCase } from '@/common/application/usecase.js';
import type { EventPublisher } from '@/common/application/events/eventPublisher.js';
import { Booking } from '../../domain/booking/booking.js';
import { BookingId } from '../../domain/booking/bookingId.js';
import type { BookingEvent } from '../../domain/events/bookingEvent.js';
import type { BookingResultEvent } from '../../domain/events/bookingResultEvents.js';
import type { BookingRepository } from '../../domain/ports/bookingRepository.js';
import type { ConfirmBooking } from '../commands/bookingConfirmationCommands.js';
import { requireArgument } from '@/common/utils/requireArgument.js';
import { requireDependency } from '@/common/utils/requireDependency.js';

export class BookingConfirming implements UseCase<ConfirmBooking, BookingResultEvent> {
  private readonly _bookingRepository: BookingRepository;
  private readonly _eventPublisher: EventPublisher<BookingEvent>;

  constructor(bookingRepository: BookingRepository, eventPublisher: EventPublisher<BookingEvent>) {
    requireDependency(bookingRepository, "bookingRepository");
    requireDependency(eventPublisher, "eventPublisher");
    this._bookingRepository = bookingRepository;
    this._eventPublisher = eventPublisher;
  }

  invoke(command: ConfirmBooking): BookingResultEvent {
    requireArgument(command, 'command');

    const booking = this.findBooking(command.aggregateId);

    const result = booking.confirm(command.amount);

    this._bookingRepository.save(booking);
    this._eventPublisher.publish(result);
    return result;
  }

  private findBooking(id: BookingId): Booking {
    const booking = this._bookingRepository.findById(id);
    if (booking === null) {
      throw new Error(`Booking not found for confirmation: ${id.value}`);
    }
    return booking;
  }
}
