package it.giannibombelli.wsc2026.payment.application.query;

import it.giannibombelli.wsc2026.common.domain.primitive.ClientReference;
import it.giannibombelli.wsc2026.common.domain.primitive.Money;
import it.giannibombelli.wsc2026.common.domain.primitive.Timestamp;
import it.giannibombelli.wsc2026.payment.domain.payment.PaymentStatus;

import java.util.UUID;

public record PaymentSummary(
    UUID id,
    ClientReference clientReference,
    Money amount,
    PaymentStatus status,
    Timestamp requestedAt
) {
}
