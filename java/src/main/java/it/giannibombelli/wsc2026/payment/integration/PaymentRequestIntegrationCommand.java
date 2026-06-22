package it.giannibombelli.wsc2026.payment.integration;

import it.giannibombelli.wsc2026.common.domain.primitive.Money;
import it.giannibombelli.wsc2026.common.utils.Require;

/**
 * Published Language di {@code payment} per richiedere un nuovo pagamento,
 * senza esporre il modello interno del BC ai consumatori ({@code booking}, {@code giftcard}).
 */
public record PaymentRequestIntegrationCommand(String clientReference, Money amount) {
    public PaymentRequestIntegrationCommand {
        Require.requireArgument(clientReference, "clientReference");
        Require.requireArgument(amount, "amount");
    }
}
