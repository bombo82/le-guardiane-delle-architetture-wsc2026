package it.giannibombelli.wsc2026.payment.api;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * DTO dell'endpoint interno di creazione pagamento: esposto intenzionalmente solo per
 * setup/test, non fa parte dell'API pubblica.
 */
public record CreatePaymentRequest(UUID paymentId, String clientReference, BigDecimal amount, Instant requestedAt) {
}
