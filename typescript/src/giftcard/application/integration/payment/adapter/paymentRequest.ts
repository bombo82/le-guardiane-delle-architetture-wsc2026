// Anti-Corruption Layer per tradurre gli eventi di dominio GiftCard
// nei command di integrazione esposti dal BC payment.

import type { GiftCardTopUpRequested } from '@/giftcard/domain/events/giftCardTopUpRequested.js';
import { paymentRequestIntegrationCommand } from '@/payment/integration/paymentRequestIntegrationCommand.js';
import type { PaymentRequestIntegrationCommand } from '@/payment/integration/paymentRequestIntegrationCommand.js';
import { requireArgument } from '@/common/utils/requireArgument.js';

export function paymentRequestFromTopUp(
  event: GiftCardTopUpRequested
): PaymentRequestIntegrationCommand {
  requireArgument(event, 'event');
  return paymentRequestIntegrationCommand(
    event.aggregateId.value.value,
    event.requestedAmount
  );
}
