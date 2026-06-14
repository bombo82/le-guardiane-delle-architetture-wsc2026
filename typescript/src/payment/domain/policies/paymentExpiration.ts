// Policy che verifica la scadenza di 48h e traduce PaymentDeadlineReached in ExpirePayment.

import type { Policy } from '@/common/domain/model/policy.js';
import { Timestamp } from '@/common/domain/primitive/timestamp.js';
import { ExpirePayment, expirePayment } from '../../application/commands/expirePayment.js';
import { PaymentDeadlineReached } from '../events/paymentDeadlineReached.js';
import type { Payment } from '../payment/payment.js';
import { requireArgument } from '@/common/utils/requireArgument.js';

const DEADLINE_WINDOW_SECONDS = 48 * 3600;

export class PaymentExpiration implements Policy<PaymentDeadlineReached, ExpirePayment> {
  isDeadlineReached(payment: Payment, now: Timestamp): boolean {
    requireArgument(payment, 'payment');
    requireArgument(now, 'now');

    const deadline = payment.requestedAt().plusSeconds(DEADLINE_WINDOW_SECONDS);
    return now.isAfterOrEqual(deadline);
  }

  evaluate(event: PaymentDeadlineReached): ExpirePayment {
    requireArgument(event, 'event');
    return expirePayment(event.aggregateId);
  }
}
