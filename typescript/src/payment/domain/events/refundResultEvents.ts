// Eventi di risultato del ciclo di vita di un rimborso.

import type { Event } from '@/common/domain/model/event.js';
import type { ClientReference } from '@/common/domain/primitive/clientReference.js';
import type { Description } from '@/common/domain/primitive/description.js';
import type { Money } from '@/common/domain/primitive/money.js';
import type { PaymentId } from '../payment/paymentId.js';
import { requireArgument } from '@/common/utils/requireArgument.js';

export type TransactionRefunded = Event<'TransactionRefunded', PaymentId> & {
  readonly clientReference: ClientReference;
  readonly amount: Money;
};

export function transactionRefunded(
  aggregateId: PaymentId,
  clientReference: ClientReference,
  amount: Money
): TransactionRefunded {
  requireArgument(aggregateId, 'paymentId');
  requireArgument(clientReference, 'clientReference');
  requireArgument(amount, 'amount');
  return { kind: 'TransactionRefunded', aggregateId, clientReference, amount };
}

export type TransactionNotRefunded = Event<'TransactionNotRefunded', PaymentId> & {
  readonly clientReference: ClientReference;
  readonly reason: Description;
};

export function transactionNotRefunded(
  aggregateId: PaymentId,
  clientReference: ClientReference,
  reason: Description
): TransactionNotRefunded {
  requireArgument(aggregateId, 'paymentId');
  requireArgument(clientReference, 'clientReference');
  requireArgument(reason, 'reason');
  return { kind: 'TransactionNotRefunded', aggregateId, clientReference, reason };
}

export type RefundResultEvent = TransactionRefunded | TransactionNotRefunded;
