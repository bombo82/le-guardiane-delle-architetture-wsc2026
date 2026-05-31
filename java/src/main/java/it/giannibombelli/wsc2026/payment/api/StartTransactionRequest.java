package it.giannibombelli.wsc2026.payment.api;

import java.math.BigDecimal;
import java.util.UUID;

public record StartTransactionRequest(String provider, UUID providerReference, BigDecimal amount) {
}
