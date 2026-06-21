// Policy che invoca il provider di pagamento a fronte di una TransactionStarted.

import { TransactionStarted } from '../../domain/events/transactionStarted.js';
import type { PaymentProvider } from '../../domain/ports/paymentProvider.js';
import type { PaymentProviderResult } from '../../domain/ports/paymentProviderResult.js';
import { requireArgument } from '@/common/utils/requireArgument.js';
import { requireDependency } from '@/common/utils/requireDependency.js';

export class PaymentCharging {
  private readonly _provider: PaymentProvider;

  constructor(provider: PaymentProvider) {
    requireDependency(provider, "provider");
    this._provider = provider;
  }

  charge(event: TransactionStarted): PaymentProviderResult {
    requireArgument(event, 'event');
    return this._provider.process(event.aggregateId.value, event.transactionId.value, event.amount);
  }
}
