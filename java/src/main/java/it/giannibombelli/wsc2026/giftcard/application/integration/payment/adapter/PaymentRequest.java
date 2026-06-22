package it.giannibombelli.wsc2026.giftcard.application.integration.payment.adapter;

import it.giannibombelli.wsc2026.common.utils.Require;
import it.giannibombelli.wsc2026.giftcard.domain.events.GiftCardTopUpRequested;
import it.giannibombelli.wsc2026.payment.integration.PaymentRequestIntegrationCommand;

public final class PaymentRequest {

    private PaymentRequest() {
    }

    public static PaymentRequestIntegrationCommand fromTopUp(GiftCardTopUpRequested event) {
        Require.requireArgument(event, "event");
        return new PaymentRequestIntegrationCommand(
            event.aggregateId().value().toString(),
            event.requestedAmount()
        );
    }
}
