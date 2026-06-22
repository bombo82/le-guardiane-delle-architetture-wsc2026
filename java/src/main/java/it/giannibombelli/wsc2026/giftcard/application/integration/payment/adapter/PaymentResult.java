package it.giannibombelli.wsc2026.giftcard.application.integration.payment.adapter;

import it.giannibombelli.wsc2026.common.utils.Require;
import it.giannibombelli.wsc2026.giftcard.application.commands.ConfirmTopUp;
import it.giannibombelli.wsc2026.giftcard.domain.giftcard.GiftCardId;
import it.giannibombelli.wsc2026.payment.integration.PaymentResultIntegrationEvent;

import java.util.UUID;

/**
 * Anti-Corruption Layer che traduce la Published Language esposta da {@code payment}
 * nel command interno {@link ConfirmTopUp} del BC {@code giftcard}.
 */
public final class PaymentResult {

    public ConfirmTopUp adapt(PaymentResultIntegrationEvent event) {
        Require.requireArgument(event, "Payment result integration event");
        return switch (event) {
            case PaymentResultIntegrationEvent.PaymentAcceptedIntegrationEvent it ->
                new ConfirmTopUp(new GiftCardId(UUID.fromString(it.clientReference())), it.amount());
            case PaymentResultIntegrationEvent.PaymentRejectedIntegrationEvent ignored -> null;
            case PaymentResultIntegrationEvent.PaymentExpiredIntegrationEvent ignored -> null;
        };
    }
}
