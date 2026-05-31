package it.giannibombelli.wsc2026.giftcard.api;

import java.math.BigDecimal;
import java.util.UUID;

public record GiftCardResponse(UUID id, BigDecimal balance) {
}
