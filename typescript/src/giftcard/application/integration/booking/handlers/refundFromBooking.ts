import type { BookingRejectedIntegrationEvent } from '@/booking/integration/giftcard/bookingResultIntegrationEvent.js';
import { BookingResult } from '@/giftcard/application/integration/booking/adapter/bookingResult.js';
import { GiftCardRefunding } from '@/giftcard/application/usecases/giftCardRefunding.js';
import { requireDependency } from '@/common/utils/requireDependency.js';

export class RefundFromBooking {
  private readonly _bookingResult: BookingResult;
  private readonly _useCase: GiftCardRefunding;

  constructor(bookingResult: BookingResult, giftCardRefunding: GiftCardRefunding) {
    requireDependency(bookingResult, 'bookingResult');
    this._bookingResult = bookingResult;
    requireDependency(giftCardRefunding, 'giftCardRefunding');
    this._useCase = giftCardRefunding;
  }

  handle(event: BookingRejectedIntegrationEvent): void {
    const command = this._bookingResult.adaptRejected(event);

    if (command !== null) {
      this._useCase.invoke(command);
    }
  }
}
