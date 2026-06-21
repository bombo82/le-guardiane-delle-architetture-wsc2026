// Policy che traduce TransactionRejected in RejectTransaction.

import type { Policy } from '@/common/application/policy.js';
import { RejectTransaction, rejectTransaction } from '../../application/commands/rejectTransaction.js';
import { TransactionRejected } from '../../domain/events/transactionRejected.js';
import { requireArgument } from '@/common/utils/requireArgument.js';

export class PaymentRejection implements Policy<TransactionRejected, RejectTransaction> {
  evaluate(event: TransactionRejected): RejectTransaction {
    requireArgument(event, 'event');
    return rejectTransaction(event.aggregateId, event.transactionId, event.reason);
  }
}
