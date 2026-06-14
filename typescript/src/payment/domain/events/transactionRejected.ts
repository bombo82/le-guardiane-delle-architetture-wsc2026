// Evento emesso quando una transazione viene rifiutata dal provider.

import type { Event } from '@/common/domain/model/event.js';
import type { Description } from '@/common/domain/primitive/description.js';
import type { PaymentId } from '../payment/paymentId.js';
import type { Provider } from '../payment/provider.js';
import type { TransactionId } from '../payment/transactionId.js';
import { requireArgument } from '@/common/utils/requireArgument.js';

export type TransactionRejected = Event<'TransactionRejected', PaymentId> & {
  readonly provider: Provider;
  readonly transactionId: TransactionId;
  readonly reason: Description;
};

export function transactionRejected(
  aggregateId: PaymentId,
  provider: Provider,
  transactionId: TransactionId,
  reason: Description
): TransactionRejected {
  requireArgument(aggregateId, 'aggregateId');
  requireArgument(provider, 'provider');
  requireArgument(transactionId, 'transactionId');
  requireArgument(reason, 'reason');
  return { kind: 'TransactionRejected', aggregateId, provider, transactionId, reason };
}
