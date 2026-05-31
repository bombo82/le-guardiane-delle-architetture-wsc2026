package it.giannibombelli.wsc2026.payment.application.commands;

import it.giannibombelli.wsc2026.common.utils.Require;

import it.giannibombelli.wsc2026.common.application.Command;
import it.giannibombelli.wsc2026.common.domain.primitive.Timestamp;
import it.giannibombelli.wsc2026.payment.domain.payment.PaymentId;
import it.giannibombelli.wsc2026.payment.domain.payment.TransactionId;

public record AcceptTransaction(PaymentId aggregateId, TransactionId transactionId, Timestamp providerCompletedAt)
    implements Command<PaymentId> {
    public AcceptTransaction {
        Require.requireArgument(aggregateId, "aggregateId");
        Require.requireArgument(transactionId, "transactionId");
        Require.requireArgument(providerCompletedAt, "providerCompletedAt");
    }
}
