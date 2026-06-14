// Command per far scadere un pagamento.

import type { Command } from '@/common/application/command.js';
import type { PaymentId } from '../../domain/payment/paymentId.js';
import { requireArgument } from '@/common/utils/requireArgument.js';

export type ExpirePayment = Command<'ExpirePayment', PaymentId>;

export function expirePayment(aggregateId: PaymentId): ExpirePayment {
  requireArgument(aggregateId, 'paymentId');
  return { kind: 'ExpirePayment', aggregateId };
}
