// Servizio applicativo che reagisce ai risultati della prenotazione e accredita la gift card.
// Violazione cross-BC didattica: GiftCard non dovrebbe importare tipi da Booking.

import type { BookingResultEvent } from '@/booking/domain/events/bookingResultEvents.js';
import { CreditGiftCardPolicy } from '../../domain/policies/creditGiftCardPolicy.js';
import { GiftCardCrediting } from '../usecases/giftCardCrediting.js';
import { requireDependency } from '@/common/utils/requireDependency.js';

export class BookingResultCrediting {
  private readonly _policy: CreditGiftCardPolicy;
  private readonly _useCase: GiftCardCrediting;

  constructor(policy: CreditGiftCardPolicy, useCase: GiftCardCrediting) {
    requireDependency(policy, "policy");
    requireDependency(useCase, "useCase");
    this._policy = policy;
    this._useCase = useCase;
  }

  handleBookingResults(event: BookingResultEvent): void {
    const command = this._policy.evaluate(event);
    if (command !== null) {
      this._useCase.invoke(command);
    }
  }
}
