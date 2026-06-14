// Evento emesso quando viene richiesto un nuovo pagamento.

import type { Event } from '@/common/domain/model/event.js';
import type { ClientReference } from '@/common/domain/primitive/clientReference.js';
import type { Money } from '@/common/domain/primitive/money.js';
import type { PaymentId } from '../payment/paymentId.js';
import { requireArgument } from '@/common/utils/requireArgument.js';

export type PaymentRequested = Event<'PaymentRequested', PaymentId> & {
  readonly clientReference: ClientReference;
  readonly amount: Money;
};

export function paymentRequested(
  aggregateId: PaymentId,
  clientReference: ClientReference,
  amount: Money
): PaymentRequested {
  requireArgument(aggregateId, 'paymentId');
  requireArgument(clientReference, 'clientReference');
  requireArgument(amount, 'amount');
  return { kind: 'PaymentRequested', aggregateId, clientReference, amount };
}
