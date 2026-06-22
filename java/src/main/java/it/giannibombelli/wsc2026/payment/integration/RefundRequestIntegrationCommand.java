package it.giannibombelli.wsc2026.payment.integration;

import it.giannibombelli.wsc2026.common.domain.primitive.Money;
import it.giannibombelli.wsc2026.common.utils.Require;

/**
 * Published Language per richiedere il rimborso di un pagamento a {@code payment}.
 * <p>
 * Usata dal Bounded Context downstream {@code booking} per chiedere il rimborso
 * senza conoscere il modello interno di {@code payment}. Il {@code clientReference}
 * identifica il pagamento da rimborsare.
 */
public record RefundRequestIntegrationCommand(String clientReference, Money amount) {
    public RefundRequestIntegrationCommand {
        Require.requireArgument(clientReference, "clientReference");
        Require.requireArgument(amount, "amount");
    }
}
