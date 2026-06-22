import type { BookingResultIntegrationEvent } from '@/booking/integration/giftcard/bookingResultIntegrationEvent.js';
import { BookingResult } from '@/giftcard/application/integration/booking/adapter/bookingResult.js';
import { GiftCardCrediting } from '@/giftcard/application/usecases/giftCardCrediting.js';
import { requireDependency } from '@/common/utils/requireDependency.js';

export class CreditFromBooking {
  private readonly _bookingResult: BookingResult;
  private readonly _useCase: GiftCardCrediting;

  constructor(bookingResult: BookingResult, giftCardCrediting: GiftCardCrediting) {
    requireDependency(bookingResult, 'bookingResult');
    this._bookingResult = bookingResult;
    requireDependency(giftCardCrediting, 'giftCardCrediting');
    this._useCase = giftCardCrediting;
  }

  handle(event: BookingResultIntegrationEvent): void {
    const command = this._bookingResult.adapt(event);

    if (command !== null) {
      this._useCase.invoke(command);
    }
  }
}
