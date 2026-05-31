package it.giannibombelli.wsc2026.payment.domain.payment;

import it.giannibombelli.wsc2026.common.utils.Require;

import it.giannibombelli.wsc2026.common.domain.identity.AggregateId;

import java.util.UUID;

public record PaymentId(UUID value) implements AggregateId {
    public PaymentId {
        Require.requireArgument(value, "PaymentId value");
    }
}
