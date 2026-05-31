package it.giannibombelli.wsc2026.payment.domain.events;

import it.giannibombelli.wsc2026.common.utils.Require;

import it.giannibombelli.wsc2026.common.domain.primitive.Description;
import it.giannibombelli.wsc2026.payment.domain.payment.PaymentId;
import it.giannibombelli.wsc2026.payment.domain.payment.Provider;
import it.giannibombelli.wsc2026.payment.domain.payment.TransactionId;

public record TransactionRejected(
    PaymentId aggregateId,
    Provider provider,
    TransactionId transactionId,
    Description reason
) implements PaymentEvent {
    public TransactionRejected {
        Require.requireArgument(aggregateId, "aggregateId");
        Require.requireArgument(provider, "provider");
        Require.requireArgument(transactionId, "transactionId");
        Require.requireArgument(reason, "reason");
    }
}
