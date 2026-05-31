package it.giannibombelli.wsc2026.payment.domain.events;

import it.giannibombelli.wsc2026.common.utils.Require;

import it.giannibombelli.wsc2026.payment.domain.payment.PaymentId;

public record PaymentDeadlineReached(
    PaymentId aggregateId
) implements PaymentEvent {

    public PaymentDeadlineReached {
        Require.requireArgument(aggregateId, "paymentId");
    }
}
