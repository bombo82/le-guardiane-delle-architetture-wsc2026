// Published Language esposta dal BC payment verso i BC downstream.
// Rappresenta il risultato di un pagamento senza esporre gli eventi di dominio interni di payment.
// I campi usano solo tipi stabili (string, Money) in modo da essere consumabili
// da qualsiasi bounded context senza introdurre coupling sui tipi interni di payment.

import type { Money } from '@/common/domain/primitive/money.js';

export type PaymentAcceptedIntegrationEvent = {
  readonly kind: 'PaymentAcceptedIntegrationEvent';
  readonly clientReference: string;
  readonly amount: Money;
};

export type PaymentRejectedIntegrationEvent = {
  readonly kind: 'PaymentRejectedIntegrationEvent';
  readonly clientReference: string;
  readonly amount: Money;
  readonly reason: string;
};

export type PaymentExpiredIntegrationEvent = {
  readonly kind: 'PaymentExpiredIntegrationEvent';
  readonly clientReference: string;
  readonly amount: Money;
};

export type PaymentResultIntegrationEvent =
  | PaymentAcceptedIntegrationEvent
  | PaymentRejectedIntegrationEvent
  | PaymentExpiredIntegrationEvent;

export function paymentAcceptedIntegrationEvent(
  clientReference: string,
  amount: Money
): PaymentAcceptedIntegrationEvent {
  return { kind: 'PaymentAcceptedIntegrationEvent', clientReference, amount };
}

export function paymentRejectedIntegrationEvent(
  clientReference: string,
  amount: Money,
  reason: string
): PaymentRejectedIntegrationEvent {
  return { kind: 'PaymentRejectedIntegrationEvent', clientReference, amount, reason };
}

export function paymentExpiredIntegrationEvent(
  clientReference: string,
  amount: Money
): PaymentExpiredIntegrationEvent {
  return { kind: 'PaymentExpiredIntegrationEvent', clientReference, amount };
}
