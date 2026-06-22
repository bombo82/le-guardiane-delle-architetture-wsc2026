// Anti-Corruption Layer che traduce la Published Language esposta da `payment`
// nei command interni del BC `booking`.
//
// Isola il modello di `booking` da eventuali evoluzioni del modello interno di `payment`:
// l'unico contratto condiviso è `PaymentResultIntegrationEvent`.

import type { PaymentResultIntegrationEvent } from '@/payment/integration/paymentResultIntegrationEvent.js';
import type { BookingConfirmationCommand } from '@/booking/application/commands/bookingConfirmationCommands.js';
import { confirmBooking, rejectBooking } from '@/booking/application/commands/bookingConfirmationCommands.js';
import { BookingId } from '@/booking/domain/booking/bookingId.js';
import type { BookingRepository } from '@/booking/domain/ports/bookingRepository.js';
import { Uuid } from '@/common/domain/primitive/uuid.js';
import { requireArgument } from '@/common/utils/requireArgument.js';
import { requireDependency } from '@/common/utils/requireDependency.js';

export class PaymentResult {
  private readonly _bookingRepository: BookingRepository;

  constructor(bookingRepository: BookingRepository) {
    requireDependency(bookingRepository, 'bookingRepository');
    this._bookingRepository = bookingRepository;
  }

  adapt(event: PaymentResultIntegrationEvent): BookingConfirmationCommand | null {
    requireArgument(event, 'Payment result integration event');

    switch (event.kind) {
      case 'PaymentAcceptedIntegrationEvent':
        return this.buildConfirmBooking(event);
      case 'PaymentRejectedIntegrationEvent':
        return this.buildRejectBooking(event);
      case 'PaymentExpiredIntegrationEvent':
        return null;
    }
  }

  private buildConfirmBooking(event: PaymentResultIntegrationEvent & { kind: 'PaymentAcceptedIntegrationEvent' }): BookingConfirmationCommand | null {
    const bookingId = Uuid.fromString(event.clientReference);
    const booking = this._bookingRepository.findById(new BookingId(bookingId));
    if (booking === null) {
      return null;
    }
    return confirmBooking(new BookingId(bookingId), booking.giftCardReference(), event.amount);
  }

  private buildRejectBooking(event: PaymentResultIntegrationEvent & { kind: 'PaymentRejectedIntegrationEvent' }): BookingConfirmationCommand | null {
    const bookingId = Uuid.fromString(event.clientReference);
    const booking = this._bookingRepository.findById(new BookingId(bookingId));
    if (booking === null) {
      return null;
    }
    return rejectBooking(new BookingId(bookingId), booking.giftCardReference(), event.amount);
  }
}
