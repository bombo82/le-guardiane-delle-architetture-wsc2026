package it.giannibombelli.wsc2026.payment.application.commands;

import it.giannibombelli.wsc2026.common.utils.Require;

import it.giannibombelli.wsc2026.common.application.Command;
import it.giannibombelli.wsc2026.common.domain.primitive.Description;
import it.giannibombelli.wsc2026.payment.domain.payment.PaymentId;
import it.giannibombelli.wsc2026.payment.domain.payment.TransactionId;

public record RejectTransaction(PaymentId aggregateId, TransactionId transactionId, Description reason)
    implements Command<PaymentId> {
    public RejectTransaction {
        Require.requireArgument(aggregateId, "aggregateId");
        Require.requireArgument(transactionId, "transactionId");
        Require.requireArgument(reason, "reason");
    }
}
