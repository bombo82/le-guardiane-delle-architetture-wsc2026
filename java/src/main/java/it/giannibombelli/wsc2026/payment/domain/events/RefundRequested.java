package it.giannibombelli.wsc2026.payment.domain.events;

import it.giannibombelli.wsc2026.common.utils.Require;

import it.giannibombelli.wsc2026.common.domain.primitive.ClientReference;
import it.giannibombelli.wsc2026.common.domain.primitive.Money;
import it.giannibombelli.wsc2026.payment.domain.payment.PaymentId;

public record RefundRequested(
    PaymentId aggregateId,
    ClientReference clientReference,
    Money amount
) implements PaymentEvent {
    public RefundRequested {
        Require.requireArgument(aggregateId, "paymentId");
        Require.requireArgument(clientReference, "clientReference");
        Require.requireArgument(amount, "amount");
    }
}
