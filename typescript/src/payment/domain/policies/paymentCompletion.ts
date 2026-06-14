// Policy che traduce TransactionAccepted in AcceptTransaction.

import type { Policy } from '@/common/domain/model/policy.js';
import { AcceptTransaction, acceptTransaction } from '../../application/commands/acceptTransaction.js';
import { TransactionAccepted } from '../events/transactionAccepted.js';
import { requireArgument } from '@/common/utils/requireArgument.js';

export class PaymentCompletion implements Policy<TransactionAccepted, AcceptTransaction> {
  evaluate(event: TransactionAccepted): AcceptTransaction {
    requireArgument(event, 'event');
    return acceptTransaction(event.aggregateId, event.transactionId, event.providerCompletedAt);
  }
}
