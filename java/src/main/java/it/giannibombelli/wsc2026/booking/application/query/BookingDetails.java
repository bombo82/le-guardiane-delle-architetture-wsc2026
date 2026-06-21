package it.giannibombelli.wsc2026.booking.application.query;

import it.giannibombelli.wsc2026.common.domain.primitive.Description;

import java.util.UUID;

public record BookingDetails(UUID id, Description description, UUID giftCardId) {
}
