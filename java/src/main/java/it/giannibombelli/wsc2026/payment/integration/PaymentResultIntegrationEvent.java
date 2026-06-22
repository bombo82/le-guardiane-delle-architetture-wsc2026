package it.giannibombelli.wsc2026.payment.integration;

import it.giannibombelli.wsc2026.common.domain.primitive.Money;
import it.giannibombelli.wsc2026.common.utils.Require;

/**
 * Published Language esposta dal BC {@code payment} verso i BC downstream.
 * <p>
 * Rappresenta il risultato di un pagamento senza esporre gli eventi di dominio interni di {@code payment}.
 * I campi usano solo tipi stabili ({@code String}, {@link Money}) in modo da essere consumabili
 * da qualsiasi bounded context senza introdurre coupling sui tipi interni di {@code payment}.
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
