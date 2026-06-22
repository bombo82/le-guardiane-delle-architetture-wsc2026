// Anti-Corruption Layer: traduce gli eventi di dominio booking nei command di integrazione del BC payment.

import type { BookingRefused } from '@/booking/domain/events/bookingResultEvents.js';
import { refundRequestIntegrationCommand } from '@/payment/integration/refundRequestIntegrationCommand.js';
import type { RefundRequestIntegrationCommand } from '@/payment/integration/refundRequestIntegrationCommand.js';
import { requireArgument } from '@/common/utils/requireArgument.js';

export function refundRequestFromBookingRefused(
  event: BookingRefused
): RefundRequestIntegrationCommand {
  requireArgument(event, 'event');
  return refundRequestIntegrationCommand(
    event.aggregateId.value.value,
    event.amount
  );
}
