package it.giannibombelli.wsc2026.booking.api;

import java.util.UUID;

public record BookingResponse(UUID id, String description, UUID giftCardId) {
}
