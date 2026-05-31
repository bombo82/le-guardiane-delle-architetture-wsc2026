package it.giannibombelli.wsc2026.payment.application.commands;

import it.giannibombelli.wsc2026.common.utils.Require;

import it.giannibombelli.wsc2026.common.application.Command;
import it.giannibombelli.wsc2026.common.domain.primitive.Money;
import it.giannibombelli.wsc2026.payment.domain.payment.PaymentId;

public record RefundTransaction(PaymentId aggregateId, Money amount)
    implements Command<PaymentId> {
    public RefundTransaction {
        Require.requireArgument(aggregateId, "aggregateId");
        Require.requireArgument(amount, "amount");
    }
}
