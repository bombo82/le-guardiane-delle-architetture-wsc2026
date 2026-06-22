// Anti-Corruption Layer: traduce gli eventi di dominio booking nei command di integrazione del BC payment.

import type { BookingPlaced } from '@/booking/domain/events/bookingPlaced.js';
import { paymentRequestIntegrationCommand } from '@/payment/integration/paymentRequestIntegrationCommand.js';
import type { PaymentRequestIntegrationCommand } from '@/payment/integration/paymentRequestIntegrationCommand.js';
import { requireArgument } from '@/common/utils/requireArgument.js';

export function paymentRequestFromBookingPlaced(
  event: BookingPlaced
): PaymentRequestIntegrationCommand {
  requireArgument(event, 'event');
  return paymentRequestIntegrationCommand(
    event.aggregateId.value.value,
    event.amount
  );
}
