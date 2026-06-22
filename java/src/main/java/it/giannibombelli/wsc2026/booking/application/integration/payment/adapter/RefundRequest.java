package it.giannibombelli.wsc2026.booking.application.integration.payment.adapter;

import it.giannibombelli.wsc2026.booking.domain.events.BookingResultEvents;
import it.giannibombelli.wsc2026.common.utils.Require;
import it.giannibombelli.wsc2026.payment.integration.RefundRequestIntegrationCommand;

public final class RefundRequest {

    private RefundRequest() {
    }

    public static RefundRequestIntegrationCommand fromBookingRefused(BookingResultEvents.BookingRefused event) {
        Require.requireArgument(event, "event");
        return new RefundRequestIntegrationCommand(
            event.aggregateId().value().toString(),
            event.amount()
        );
    }
}
