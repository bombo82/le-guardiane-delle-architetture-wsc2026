// Evento emesso quando una transazione viene accettata dal provider.

import type { Event } from '@/common/domain/model/event.js';
import type { Money } from '@/common/domain/primitive/money.js';
import type { Timestamp } from '@/common/domain/primitive/timestamp.js';
import type { PaymentId } from '../payment/paymentId.js';
import type { Provider } from '../payment/provider.js';
import type { TransactionId } from '../payment/transactionId.js';
import { requireArgument } from '@/common/utils/requireArgument.js';

export type TransactionAccepted = Event<'TransactionAccepted', PaymentId> & {
  readonly provider: Provider;
  readonly transactionId: TransactionId;
  readonly amount: Money;
  readonly providerCompletedAt: Timestamp;
};

export function transactionAccepted(
  aggregateId: PaymentId,
  provider: Provider,
  transactionId: TransactionId,
  amount: Money,
  providerCompletedAt: Timestamp
): TransactionAccepted {
  requireArgument(aggregateId, 'aggregateId');
  requireArgument(provider, 'provider');
  requireArgument(transactionId, 'transactionId');
  requireArgument(amount, 'amount');
  requireArgument(providerCompletedAt, 'providerCompletedAt');
  return { kind: 'TransactionAccepted', aggregateId, provider, transactionId, amount, providerCompletedAt };
}
