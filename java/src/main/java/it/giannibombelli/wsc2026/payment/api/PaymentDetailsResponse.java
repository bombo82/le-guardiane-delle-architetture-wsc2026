package it.giannibombelli.wsc2026.payment.api;

import it.giannibombelli.wsc2026.payment.application.query.PaymentDetails;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record PaymentDetailsResponse(
    UUID id,
    String clientReference,
    BigDecimal amount,
    String status,
    Instant requestedAt,
    List<TransactionResponse> transactions
) {
    public static PaymentDetailsResponse from(PaymentDetails details) {
        return new PaymentDetailsResponse(
            details.id(),
            details.clientReference().toString(),
            details.amount().value(),
            details.status().name(),
            details.requestedAt().value(),
            details.transactions().stream().map(PaymentDetailsResponse::from).toList()
        );
    }

    private static TransactionResponse from(PaymentDetails.TransactionDetail detail) {
        return new TransactionResponse(
            detail.id(),
            detail.provider().label(),
            detail.providerReference(),
            detail.amount().value(),
            detail.status().name(),
            detail.startedAt().value(),
            detail.completedAt() == null ? null : detail.completedAt().value()
        );
    }
}
