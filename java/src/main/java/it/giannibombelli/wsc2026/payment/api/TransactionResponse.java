package it.giannibombelli.wsc2026.payment.api;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TransactionResponse(
    UUID id,
    String provider,
    UUID providerReference,
    BigDecimal amount,
    String status,
    Instant startedAt,
    Instant completedAt
) {
}
