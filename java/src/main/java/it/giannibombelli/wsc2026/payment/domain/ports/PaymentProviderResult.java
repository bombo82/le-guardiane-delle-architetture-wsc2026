package it.giannibombelli.wsc2026.payment.domain.ports;

import it.giannibombelli.wsc2026.common.utils.Require;

import it.giannibombelli.wsc2026.common.domain.primitive.Description;
import it.giannibombelli.wsc2026.common.domain.primitive.Timestamp;

import java.util.UUID;

public sealed interface PaymentProviderResult {
    /**
     * {@code providerCompletedAt} è il dato fornito dal provider che determina la finestra delle 48h
     * nell'aggregato Payment: non è il wall-clock del comando AcceptTransaction.
     */
    record Success(UUID transactionId, Timestamp providerCompletedAt) implements PaymentProviderResult {
        public Success {
            Require.requireArgument(transactionId, "transactionId");
            Require.requireArgument(providerCompletedAt, "providerCompletedAt");
        }
    }

    record Failure(Description reason) implements PaymentProviderResult {
        public Failure {
            Require.requireArgument(reason, "reason");
        }
    }
}
