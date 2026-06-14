// Command per accettare una transazione.

import type { Command } from '@/common/application/command.js';
import type { Timestamp } from '@/common/domain/primitive/timestamp.js';
import type { PaymentId } from '../../domain/payment/paymentId.js';
import type { TransactionId } from '../../domain/payment/transactionId.js';
import { requireArgument } from '@/common/utils/requireArgument.js';

export type AcceptTransaction = Command<'AcceptTransaction', PaymentId> & {
  readonly transactionId: TransactionId;
  readonly providerCompletedAt: Timestamp;
};

export function acceptTransaction(
  aggregateId: PaymentId,
  transactionId: TransactionId,
  providerCompletedAt: Timestamp
): AcceptTransaction {
  requireArgument(aggregateId, 'aggregateId');
  requireArgument(transactionId, 'transactionId');
  requireArgument(providerCompletedAt, 'providerCompletedAt');
  return { kind: 'AcceptTransaction', aggregateId, transactionId, providerCompletedAt };
}
