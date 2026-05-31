package it.giannibombelli.wsc2026.payment.api;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Internal request DTO for creating a Payment.
 * <p>
 * This endpoint is intentionally exposed only for internal/test setup purposes and
 * is not part of the public customer-facing API surface.
 */
public record CreatePaymentRequest(UUID paymentId, String clientReference, BigDecimal amount, Instant requestedAt) {
}
