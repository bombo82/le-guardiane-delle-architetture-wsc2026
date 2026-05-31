package it.giannibombelli.wsc2026.payment.application.query;

import it.giannibombelli.wsc2026.common.domain.primitive.ClientReference;
import it.giannibombelli.wsc2026.common.domain.primitive.Money;
import it.giannibombelli.wsc2026.common.domain.primitive.Timestamp;
import it.giannibombelli.wsc2026.payment.domain.payment.PaymentStatus;
import it.giannibombelli.wsc2026.payment.domain.payment.Provider;
import it.giannibombelli.wsc2026.payment.domain.payment.TransactionStatus;

import java.util.List;
import java.util.UUID;

public record PaymentDetails(
    UUID id,
    ClientReference clientReference,
    Money amount,
    PaymentStatus status,
    Timestamp requestedAt,
    List<TransactionDetail> transactions
) {
    public record TransactionDetail(
        UUID id,
        Provider provider,
        UUID providerReference,
        Money amount,
        TransactionStatus status,
        Timestamp startedAt,
        Timestamp completedAt
    ) {
    }
}
