// Read model riassuntivo di un Payment.

import type { ClientReference } from '@/common/domain/primitive/clientReference.js';
import type { Money } from '@/common/domain/primitive/money.js';
import type { Timestamp } from '@/common/domain/primitive/timestamp.js';
import type { PaymentStatus } from '../../domain/payment/paymentStatus.js';

export type PaymentSummary = {
  readonly id: string;
  readonly clientReference: ClientReference;
  readonly amount: Money;
  readonly status: PaymentStatus;
  readonly requestedAt: Timestamp;
};
