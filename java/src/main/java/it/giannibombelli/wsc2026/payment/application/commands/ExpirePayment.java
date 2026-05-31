package it.giannibombelli.wsc2026.payment.application.commands;

import it.giannibombelli.wsc2026.common.utils.Require;

import it.giannibombelli.wsc2026.common.application.Command;
import it.giannibombelli.wsc2026.payment.domain.payment.PaymentId;

public record ExpirePayment(PaymentId aggregateId) implements Command<PaymentId> {
    public ExpirePayment {
        Require.requireArgument(aggregateId, "paymentId");
    }
}
