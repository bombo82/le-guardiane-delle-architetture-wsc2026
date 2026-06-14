// Command per richiedere un rimborso.

import type { Command } from '@/common/application/command.js';
import type { Money } from '@/common/domain/primitive/money.js';
import type { PaymentId } from '../../domain/payment/paymentId.js';
import { requireArgument } from '@/common/utils/requireArgument.js';

export type RefundTransaction = Command<'RefundTransaction', PaymentId> & {
  readonly amount: Money;
};

export function refundTransaction(aggregateId: PaymentId, amount: Money): RefundTransaction {
  requireArgument(aggregateId, 'aggregateId');
  requireArgument(amount, 'amount');
  return { kind: 'RefundTransaction', aggregateId, amount };
}
