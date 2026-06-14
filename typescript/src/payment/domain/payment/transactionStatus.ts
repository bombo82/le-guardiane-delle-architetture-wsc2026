import { requireArgument } from '@/common/utils/requireArgument.js';
// Stati di una Transaction.

export const TransactionStatus = {
  STARTED: 'STARTED',
  ACCEPTED: 'ACCEPTED',
  REJECTED: 'REJECTED',
  REFUNDED: 'REFUNDED',
} as const;

export type TransactionStatus = (typeof TransactionStatus)[keyof typeof TransactionStatus];

export function transactionStatusFromLabel(label: string): TransactionStatus {
  requireArgument(label, 'transaction status label');
  const value = Object.values(TransactionStatus).find((status) => status === label);
  if (value === undefined) {
    throw new Error(`unknown transaction status: ${label}`);
  }
  return value;
}
