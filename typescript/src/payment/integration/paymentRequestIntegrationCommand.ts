// Published Language esposta dal BC payment per richiedere un nuovo pagamento.
// Usa solo tipi stabili (string, Money) in modo da essere invocabile
// da qualsiasi bounded context senza introdurre coupling sui tipi interni di payment.

import type { Money } from '@/common/domain/primitive/money.js';
import { requireArgument } from '@/common/utils/requireArgument.js';

export type PaymentRequestIntegrationCommand = {
  readonly kind: 'PaymentRequestIntegrationCommand';
  readonly clientReference: string;
  readonly amount: Money;
};

export function paymentRequestIntegrationCommand(
  clientReference: string,
  amount: Money
): PaymentRequestIntegrationCommand {
  requireArgument(clientReference, 'clientReference');
  requireArgument(amount, 'amount');
  return { kind: 'PaymentRequestIntegrationCommand', clientReference, amount };
}
