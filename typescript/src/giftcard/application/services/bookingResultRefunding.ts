// Servizio applicativo che reagisce ai risultati della prenotazione e rimborsa la gift card.
// Violazione cross-BC didattica: GiftCard non dovrebbe importare tipi da Booking.

import type { BookingResultEvent } from '@/booking/domain/events/bookingResultEvents.js';
import { RefundGiftCardPolicy } from '../../domain/policies/refundGiftCardPolicy.js';
import { GiftCardRefunding } from '../usecases/giftCardRefunding.js';
import { requireDependency } from '@/common/utils/requireDependency.js';

export class BookingResultRefunding {
  private readonly _policy: RefundGiftCardPolicy;
  private readonly _useCase: GiftCardRefunding;

  constructor(policy: RefundGiftCardPolicy, useCase: GiftCardRefunding) {
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
