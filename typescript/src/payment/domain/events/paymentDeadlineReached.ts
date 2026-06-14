// Evento emesso quando viene raggiunta la scadenza di 48h per un pagamento.

import type { Event } from '@/common/domain/model/event.js';
import type { PaymentId } from '../payment/paymentId.js';
import { requireArgument } from '@/common/utils/requireArgument.js';

export type PaymentDeadlineReached = Event<'PaymentDeadlineReached', PaymentId>;

export function paymentDeadlineReached(aggregateId: PaymentId): PaymentDeadlineReached {
  requireArgument(aggregateId, 'paymentId');
  return { kind: 'PaymentDeadlineReached', aggregateId };
}
