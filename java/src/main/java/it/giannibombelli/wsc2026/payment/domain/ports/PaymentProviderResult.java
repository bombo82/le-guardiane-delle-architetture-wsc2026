package it.giannibombelli.wsc2026.payment.domain.ports;

import it.giannibombelli.wsc2026.common.utils.Require;

import it.giannibombelli.wsc2026.common.domain.primitive.Description;
import it.giannibombelli.wsc2026.common.domain.primitive.Timestamp;

import java.util.UUID;

public sealed interface PaymentProviderResult {
    /**
     * Success result returned by PaymentProvider implementations.
     * The key per spec: providerCompletedAt is the datum supplied by the provider that drives the 48h
     * window calculation inside the Payment aggregate. It is **not** the AcceptTransaction command wall time.
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
