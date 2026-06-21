// Servizio applicativo che reagisce agli esiti del Payment BC confermando/rifiutando la prenotazione.


import type { PaymentResultEvent } from '@/payment/domain/events/paymentResultEvents.js';
import { match } from 'ts-pattern';
import { PaymentPolicy } from '../../application/policies/paymentPolicy.js';
import { BookingConfirming } from '../usecases/bookingConfirming.js';
import { BookingRejecting } from '../usecases/bookingRejecting.js';
import { requireDependency } from '@/common/utils/requireDependency.js';

export class PaymentResultOutcome {
  private readonly _policy: PaymentPolicy;
  private readonly _confirmation: BookingConfirming;
  private readonly _rejection: BookingRejecting;

  constructor(policy: PaymentPolicy, confirmation: BookingConfirming, rejection: BookingRejecting) {
    requireDependency(policy, "policy");
    requireDependency(confirmation, "confirmation");
    requireDependency(rejection, "rejection");
    this._policy = policy;
    this._confirmation = confirmation;
    this._rejection = rejection;
  }

  handlePaymentResults(event: PaymentResultEvent): void {
    const command = this._policy.evaluate(event);
    if (command === null) {
      return;
    }

    match(command)
      .with({ kind: 'ConfirmBooking' }, (confirmBooking) => this._confirmation.invoke(confirmBooking))
      .with({ kind: 'RejectBooking' }, (rejectBooking) => this._rejection.invoke(rejectBooking))
      .exhaustive();
  }
}
