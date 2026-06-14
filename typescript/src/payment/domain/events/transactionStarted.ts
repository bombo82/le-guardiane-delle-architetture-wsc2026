// Evento emesso quando viene avviata una transazione di pagamento.

import type { Event } from '@/common/domain/model/event.js';
import type { Money } from '@/common/domain/primitive/money.js';
import type { PaymentId } from '../payment/paymentId.js';
import type { Provider } from '../payment/provider.js';
import type { TransactionId } from '../payment/transactionId.js';
import { requireArgument } from '@/common/utils/requireArgument.js';

export type TransactionStarted = Event<'TransactionStarted', PaymentId> & {
  readonly provider: Provider;
  readonly transactionId: TransactionId;
  readonly amount: Money;
};

export function transactionStarted(
  aggregateId: PaymentId,
  provider: Provider,
  transactionId: TransactionId,
  amount: Money
): TransactionStarted {
  requireArgument(aggregateId, 'aggregateId');
  requireArgument(provider, 'provider');
  requireArgument(transactionId, 'transactionId');
  requireArgument(amount, 'amount');
  return { kind: 'TransactionStarted', aggregateId, provider, transactionId, amount };
}
