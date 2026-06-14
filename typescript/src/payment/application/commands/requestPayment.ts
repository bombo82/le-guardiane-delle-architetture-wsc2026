// Command per richiedere un nuovo pagamento.

import type { Command } from '@/common/application/command.js';
import type { ClientReference } from '@/common/domain/primitive/clientReference.js';
import type { Money } from '@/common/domain/primitive/money.js';
import type { Timestamp } from '@/common/domain/primitive/timestamp.js';
import type { PaymentId } from '../../domain/payment/paymentId.js';
import { requireArgument } from '@/common/utils/requireArgument.js';

export type RequestPayment = Command<'RequestPayment', PaymentId> & {
  readonly clientReference: ClientReference;
  readonly amount: Money;
  readonly requestedAt: Timestamp;
};

export function requestPayment(
  aggregateId: PaymentId,
  clientReference: ClientReference,
  amount: Money,
  requestedAt: Timestamp
): RequestPayment {
  requireArgument(aggregateId, 'paymentId');
  requireArgument(clientReference, 'clientReference');
  requireArgument(amount, 'amount');
  requireArgument(requestedAt, 'requestedAt');
  return { kind: 'RequestPayment', aggregateId, clientReference, amount, requestedAt };
}
