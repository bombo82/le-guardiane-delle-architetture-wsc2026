package it.giannibombelli.wsc2026.booking.api;

import java.math.BigDecimal;
import java.util.UUID;

public record PlaceBookingRequest(BigDecimal amount, String description, UUID giftCardId) {
}
