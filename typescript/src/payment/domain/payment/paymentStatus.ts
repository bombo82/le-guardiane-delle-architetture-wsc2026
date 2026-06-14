import { requireArgument } from '@/common/utils/requireArgument.js';
// Stati dell'aggregato Payment.

export const PaymentStatus = {
  REQUESTED: 'REQUESTED',
  PROCESSING: 'PROCESSING',
  ACCEPTED: 'ACCEPTED',
  REJECTED: 'REJECTED',
  EXPIRED: 'EXPIRED',
  REFUNDED: 'REFUNDED',
} as const;

export type PaymentStatus = (typeof PaymentStatus)[keyof typeof PaymentStatus];

export function paymentStatusFromLabel(label: string): PaymentStatus {
  requireArgument(label, 'payment status label');
  const value = Object.values(PaymentStatus).find((status) => status === label);
  if (value === undefined) {
    throw new Error(`unknown payment status: ${label}`);
  }
  return value;
}
