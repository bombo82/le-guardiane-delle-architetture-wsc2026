// Published Language esposta dal BC payment per richiedere un rimborso.
// Usa solo tipi stabili (string, Money) in modo da essere invocabile
// da qualsiasi bounded context senza introdurre coupling sui tipi interni di payment.

import type { Money } from '@/common/domain/primitive/money.js';
import { requireArgument } from '@/common/utils/requireArgument.js';

export type RefundRequestIntegrationCommand = {
  readonly kind: 'RefundRequestIntegrationCommand';
  readonly clientReference: string;
  readonly amount: Money;
};

export function refundRequestIntegrationCommand(
  clientReference: string,
  amount: Money
): RefundRequestIntegrationCommand {
  requireArgument(clientReference, 'clientReference');
  requireArgument(amount, 'amount');
  return { kind: 'RefundRequestIntegrationCommand', clientReference, amount };
}
