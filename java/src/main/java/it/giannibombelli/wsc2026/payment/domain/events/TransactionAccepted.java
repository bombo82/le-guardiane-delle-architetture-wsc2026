package it.giannibombelli.wsc2026.payment.domain.events;

import it.giannibombelli.wsc2026.common.utils.Require;

import it.giannibombelli.wsc2026.common.domain.primitive.Money;
import it.giannibombelli.wsc2026.common.domain.primitive.Timestamp;
import it.giannibombelli.wsc2026.payment.domain.payment.PaymentId;
import it.giannibombelli.wsc2026.payment.domain.payment.Provider;
import it.giannibombelli.wsc2026.payment.domain.payment.TransactionId;

public record TransactionAccepted(
    PaymentId aggregateId,
    Provider provider,
    TransactionId transactionId,
    Money amount,
    Timestamp providerCompletedAt
) implements PaymentEvent {
    public TransactionAccepted {
        Require.requireArgument(aggregateId, "aggregateId");
        Require.requireArgument(provider, "provider");
        Require.requireArgument(transactionId, "transactionId");
        Require.requireArgument(amount, "amount");
        Require.requireArgument(providerCompletedAt, "providerCompletedAt");
    }
}
