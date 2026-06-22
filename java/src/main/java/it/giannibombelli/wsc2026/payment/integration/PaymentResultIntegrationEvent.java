package it.giannibombelli.wsc2026.payment.integration;

import it.giannibombelli.wsc2026.common.domain.primitive.Money;
import it.giannibombelli.wsc2026.common.utils.Require;

/**
 * Published Language di {@code payment}: espone l'esito di un pagamento usando solo tipi stabili
 * ({@code String}, {@link Money}), senza rivelare eventi e tipi interni del BC.
 */
public sealed interface PaymentResultIntegrationEvent {

    record PaymentAcceptedIntegrationEvent(
        String clientReference,
        Money amount
    ) implements PaymentResultIntegrationEvent {
        public PaymentAcceptedIntegrationEvent {
            Require.requireArgument(clientReference, "clientReference");
            Require.requireArgument(amount, "amount");
        }
    }

    record PaymentRejectedIntegrationEvent(
        String clientReference,
        Money amount,
        String reason
    ) implements PaymentResultIntegrationEvent {
        public PaymentRejectedIntegrationEvent {
            Require.requireArgument(clientReference, "clientReference");
            Require.requireArgument(amount, "amount");
            Require.requireArgument(reason, "reason");
        }
    }

    record PaymentExpiredIntegrationEvent(
        String clientReference,
        Money amount
    ) implements PaymentResultIntegrationEvent {
        public PaymentExpiredIntegrationEvent {
            Require.requireArgument(clientReference, "clientReference");
            Require.requireArgument(amount, "amount");
        }
    }
}
