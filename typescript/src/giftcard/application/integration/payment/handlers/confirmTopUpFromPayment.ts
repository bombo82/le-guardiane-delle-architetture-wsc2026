import type { PaymentResultIntegrationEvent } from '@/payment/integration/paymentResultIntegrationEvent.js';
import { PaymentResult } from '@/giftcard/application/integration/payment/adapter/paymentResult.js';
import { TopUpConfirming } from '@/giftcard/application/usecases/topUpConfirming.js';
import { requireDependency } from '@/common/utils/requireDependency.js';

export class ConfirmTopUpFromPayment {
  private readonly _paymentResult: PaymentResult;
  private readonly _useCase: TopUpConfirming;

  constructor(paymentResult: PaymentResult, topUpConfirming: TopUpConfirming) {
    requireDependency(paymentResult, 'paymentResult');
    this._paymentResult = paymentResult;
    requireDependency(topUpConfirming, 'topUpConfirming');
    this._useCase = topUpConfirming;
  }

  handle(event: PaymentResultIntegrationEvent): void {
    const command = this._paymentResult.adapt(event);

    if (command !== null) {
      this._useCase.invoke(command);
    }
  }
}
