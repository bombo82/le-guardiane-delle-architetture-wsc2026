package it.giannibombelli.wsc2026.giftcard.api;

import java.math.BigDecimal;

public record RequestTopUpRequest(BigDecimal amount) {
}
