package it.giannibombelli.wsc2026.booking.application.integration.payment.adapter;

import it.giannibombelli.wsc2026.booking.domain.events.BookingPlaced;
import it.giannibombelli.wsc2026.common.utils.Require;
import it.giannibombelli.wsc2026.payment.integration.PaymentRequestIntegrationCommand;

public final class PaymentRequest {

    private PaymentRequest() {
    }

    public static PaymentRequestIntegrationCommand fromBookingPlaced(BookingPlaced event) {
        Require.requireArgument(event, "event");
        return new PaymentRequestIntegrationCommand(
            event.aggregateId().value().toString(),
            event.amount()
        );
    }
}
