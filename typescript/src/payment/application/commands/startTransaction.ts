// Command per avviare una transazione su un pagamento.

import type { Command } from '@/common/application/command.js';
import type { Money } from '@/common/domain/primitive/money.js';
import type { Timestamp } from '@/common/domain/primitive/timestamp.js';
import type { PaymentId } from '../../domain/payment/paymentId.js';
import type { Provider } from '../../domain/payment/provider.js';
import type { ProviderReference } from '../../domain/payment/providerReference.js';
import { requireArgument } from '@/common/utils/requireArgument.js';

export type StartTransaction = Command<'StartTransaction', PaymentId> & {
  readonly provider: Provider;
  readonly providerReference: ProviderReference | null;
  readonly amount: Money;
  readonly startedAt: Timestamp;
};

export function startTransaction(
  aggregateId: PaymentId,
  provider: Provider,
  providerReference: ProviderReference | null,
  amount: Money,
  startedAt: Timestamp
): StartTransaction {
  requireArgument(aggregateId, 'aggregateId');
  requireArgument(provider, 'provider');
  requireArgument(amount, 'amount');
  requireArgument(startedAt, 'startedAt');
  return { kind: 'StartTransaction', aggregateId, provider, providerReference, amount, startedAt };
}
