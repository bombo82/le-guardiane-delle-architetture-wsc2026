// Eventi di risultato del ciclo di vita di un pagamento.

import type { Event } from '@/common/domain/model/event.js';
import type { ClientReference } from '@/common/domain/primitive/clientReference.js';
import type { Description } from '@/common/domain/primitive/description.js';
import type { Money } from '@/common/domain/primitive/money.js';
import type { PaymentId } from '../payment/paymentId.js';
import { requireArgument } from '@/common/utils/requireArgument.js';

export type PaymentAccepted = Event<'PaymentAccepted', PaymentId> & {
  readonly clientReference: ClientReference;
  readonly amount: Money;
};

export function paymentAccepted(
  aggregateId: PaymentId,
  clientReference: ClientReference,
  amount: Money
): PaymentAccepted {
  requireArgument(aggregateId, 'paymentId');
  requireArgument(clientReference, 'clientReference');
  requireArgument(amount, 'amount');
  return { kind: 'PaymentAccepted', aggregateId, clientReference, amount };
}

export type PaymentRejected = Event<'PaymentRejected', PaymentId> & {
  readonly clientReference: ClientReference;
  readonly amount: Money;
  readonly reason: Description;
};

export function paymentRejected(
  aggregateId: PaymentId,
  clientReference: ClientReference,
  amount: Money,
  reason: Description
): PaymentRejected {
  requireArgument(aggregateId, 'paymentId');
  requireArgument(clientReference, 'clientReference');
  requireArgument(amount, 'amount');
  requireArgument(reason, 'reason');
  return { kind: 'PaymentRejected', aggregateId, clientReference, amount, reason };
}

export type PaymentExpired = Event<'PaymentExpired', PaymentId> & {
  readonly clientReference: ClientReference;
  readonly amount: Money;
};

export function paymentExpired(
  aggregateId: PaymentId,
  clientReference: ClientReference,
  amount: Money
): PaymentExpired {
  requireArgument(aggregateId, 'paymentId');
  requireArgument(clientReference, 'clientReference');
  requireArgument(amount, 'amount');
  return { kind: 'PaymentExpired', aggregateId, clientReference, amount };
}

export type PaymentResultEvent = PaymentAccepted | PaymentRejected | PaymentExpired;
