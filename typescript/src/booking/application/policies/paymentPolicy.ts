// Policy che traduce gli esiti del Payment BC in comandi di conferma/rifiuto del Booking BC.


import { ClientReference } from '@/common/domain/primitive/clientReference.js';
import type { PaymentResultEvent } from '@/payment/domain/events/paymentResultEvents.js';
import type { Policy } from '@/common/application/policy.js';
import { match } from 'ts-pattern';
import { Booking } from '../../domain/booking/booking.js';
import { BookingId } from '../../domain/booking/bookingId.js';
import type { BookingRepository } from '../../domain/ports/bookingRepository.js';
import { requireArgument } from '@/common/utils/requireArgument.js';
import { requireDependency } from '@/common/utils/requireDependency.js';
import {
  type BookingConfirmationCommand,
  confirmBooking,
  rejectBooking,
} from '../../application/commands/bookingConfirmationCommands.js';

export class PaymentPolicy implements Policy<PaymentResultEvent, BookingConfirmationCommand> {
  private readonly _bookingRepository: BookingRepository;

  constructor(bookingRepository: BookingRepository) {
    requireDependency(bookingRepository, "bookingRepository");
    this._bookingRepository = bookingRepository;
  }

  evaluate(event: PaymentResultEvent): BookingConfirmationCommand | null {
    requireArgument(event, 'Payment event');

    return match(event)
      .with({ kind: 'PaymentAccepted' }, (accepted) => {
        const booking = this.findBooking(accepted.clientReference);
        return booking === null
          ? null
          : confirmBooking(
              this.bookingIdFrom(accepted.clientReference),
              booking.giftCardId(),
              accepted.amount
            );
      })
      .with({ kind: 'PaymentRejected' }, (rejected) => {
        const booking = this.findBooking(rejected.clientReference);
        return booking === null
          ? null
          : rejectBooking(
              this.bookingIdFrom(rejected.clientReference),
              booking.giftCardId(),
              rejected.amount
            );
      })
      .with({ kind: 'PaymentExpired' }, () => null)
      .exhaustive();
  }

  private findBooking(clientReference: ClientReference): Booking | null {
    return this._bookingRepository.findById(this.bookingIdFrom(clientReference));
  }

  private bookingIdFrom(clientReference: ClientReference): BookingId {
    return new BookingId(clientReference.value);
  }
}
