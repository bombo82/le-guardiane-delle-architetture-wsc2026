// Servizio applicativo che reagisce agli esiti dei pagamenti e conferma la ricarica.
// Violazione cross-BC didattica: GiftCard non dovrebbe importare tipi da Payment.

import type { PaymentResultEvent } from '@/payment/domain/events/paymentResultEvents.js';
import { TopUpConfirming } from '../usecases/topUpConfirming.js';
import { ConfirmTopUpPolicy } from '../../domain/policies/confirmTopUpPolicy.js';
import { requireDependency } from '@/common/utils/requireDependency.js';

export class TopUpConfirmation {
  private readonly _policy: ConfirmTopUpPolicy;
  private readonly _useCase: TopUpConfirming;

  constructor(policy: ConfirmTopUpPolicy, useCase: TopUpConfirming) {
    requireDependency(policy, "policy");
    requireDependency(useCase, "useCase");
    this._policy = policy;
    this._useCase = useCase;
  }

  handlePaymentResults(event: PaymentResultEvent): void {
    const command = this._policy.evaluate(event);
    if (command !== null) {
      this._useCase.invoke(command);
    }
  }
}
