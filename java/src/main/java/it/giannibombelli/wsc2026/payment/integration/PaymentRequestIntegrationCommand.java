package it.giannibombelli.wsc2026.payment.integration;

import it.giannibombelli.wsc2026.common.domain.primitive.Money;
import it.giannibombelli.wsc2026.common.utils.Require;

/**
 * Published Language per richiedere un nuovo pagamento a {@code payment}.
 * <p>
 * Usata dai Bounded Context downstream ({@code booking}, {@code giftcard}) per chiedere
 * l'emissione di una richiesta di pagamento senza conoscere il modello interno di {@code payment}.
 */
public record PaymentRequestIntegrationCommand(String clientReference, Money amount) {
    public PaymentRequestIntegrationCommand {
        Require.requireArgument(clientReference, "clientReference");
        Require.requireArgument(amount, "amount");
    }
}
