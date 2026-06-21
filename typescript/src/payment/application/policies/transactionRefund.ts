// Policy che traduce RefundRequested in RefundTransaction.

import type { Policy } from '@/common/application/policy.js';
import { RefundTransaction, refundTransaction } from '../../application/commands/refundTransaction.js';
import { RefundRequested } from '../../domain/events/refundRequested.js';
import { requireArgument } from '@/common/utils/requireArgument.js';

export class TransactionRefund implements Policy<RefundRequested, RefundTransaction> {
  evaluate(event: RefundRequested): RefundTransaction {
    requireArgument(event, 'event');
    return refundTransaction(event.aggregateId, event.amount);
  }
}
