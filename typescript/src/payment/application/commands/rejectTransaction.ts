// Command per rifiutare una transazione.

import type { Command } from '@/common/application/command.js';
import type { Description } from '@/common/domain/primitive/description.js';
import type { PaymentId } from '../../domain/payment/paymentId.js';
import type { TransactionId } from '../../domain/payment/transactionId.js';
import { requireArgument } from '@/common/utils/requireArgument.js';

export type RejectTransaction = Command<'RejectTransaction', PaymentId> & {
  readonly transactionId: TransactionId;
  readonly reason: Description;
};

export function rejectTransaction(
  aggregateId: PaymentId,
  transactionId: TransactionId,
  reason: Description
): RejectTransaction {
  requireArgument(aggregateId, 'aggregateId');
  requireArgument(transactionId, 'transactionId');
  requireArgument(reason, 'reason');
  return { kind: 'RejectTransaction', aggregateId, transactionId, reason };
}
