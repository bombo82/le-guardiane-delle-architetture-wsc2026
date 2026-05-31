package it.giannibombelli.wsc2026.payment.application.commands;

import it.giannibombelli.wsc2026.common.utils.Require;

import it.giannibombelli.wsc2026.common.application.Command;
import it.giannibombelli.wsc2026.common.domain.primitive.ClientReference;
import it.giannibombelli.wsc2026.common.domain.primitive.Money;
import it.giannibombelli.wsc2026.common.domain.primitive.Timestamp;
import it.giannibombelli.wsc2026.payment.domain.payment.PaymentId;

public record RequestPayment(PaymentId aggregateId, ClientReference clientReference, Money amount, Timestamp requestedAt)
    implements Command<PaymentId> {
    public RequestPayment {
        Require.requireArgument(aggregateId, "paymentId");
        Require.requireArgument(clientReference, "clientReference");
        Require.requireArgument(amount, "amount");
        Require.requireArgument(requestedAt, "requestedAt");
    }
}
