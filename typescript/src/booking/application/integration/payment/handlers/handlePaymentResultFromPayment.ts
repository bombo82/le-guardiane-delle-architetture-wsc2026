import type { PaymentResultIntegrationEvent } from '@/payment/integration/paymentResultIntegrationEvent.js';
import { PaymentResult } from '@/booking/application/integration/payment/adapter/paymentResult.js';
import { BookingConfirming } from '@/booking/application/usecases/bookingConfirming.js';
import { BookingRejecting } from '@/booking/application/usecases/bookingRejecting.js';
import { requireDependency } from '@/common/utils/requireDependency.js';

export class HandlePaymentResultFromPayment {
  private readonly _paymentResult: PaymentResult;
  private readonly _confirming: BookingConfirming;
  private readonly _rejecting: BookingRejecting;

  constructor(paymentResult: PaymentResult, confirming: BookingConfirming, rejecting: BookingRejecting) {
    requireDependency(paymentResult, 'paymentResult');
    this._paymentResult = paymentResult;
    requireDependency(confirming, 'confirming');
    this._confirming = confirming;
    requireDependency(rejecting, 'rejecting');
    this._rejecting = rejecting;
  }

  handle(event: PaymentResultIntegrationEvent): void {
    const command = this._paymentResult.adapt(event);
    if (command === null) {
      return;
    }

    switch (command.kind) {
      case 'ConfirmBooking':
        this._confirming.invoke(command);
        break;
      case 'RejectBooking':
        this._rejecting.invoke(command);
        break;
    }
  }
}
