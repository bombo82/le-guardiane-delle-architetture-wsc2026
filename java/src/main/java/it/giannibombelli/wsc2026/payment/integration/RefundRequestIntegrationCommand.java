package it.giannibombelli.wsc2026.payment.integration;

import it.giannibombelli.wsc2026.common.domain.primitive.Money;
import it.giannibombelli.wsc2026.common.utils.Require;

/**
 * Published Language di {@code payment} per richiedere un rimborso: il {@code clientReference}
 * identifica il pagamento, senza esporre il modello interno del BC.
 */
public record RefundRequestIntegrationCommand(String clientReference, Money amount) {
    public RefundRequestIntegrationCommand {
        Require.requireArgument(clientReference, "clientReference");
        Require.requireArgument(amount, "amount");
    }
}
