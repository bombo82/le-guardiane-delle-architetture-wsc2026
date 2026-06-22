package it.giannibombelli.wsc2026.booking.integration.giftcard;

import it.giannibombelli.wsc2026.common.domain.primitive.Money;

import java.util.UUID;

/**
 * Published Language di {@code booking} verso {@code giftcard}: espone l'esito di una prenotazione
 * usando solo tipi stabili ({@link UUID}, {@link Money}), senza rivelare gli eventi interni del BC.
 */
public sealed interface BookingResultIntegrationEvent {

    record BookingCompletedIntegrationEvent(UUID giftCardReference, Money amount) implements BookingResultIntegrationEvent {
    }

    record BookingRefusedIntegrationEvent(UUID giftCardReference, Money amount) implements BookingResultIntegrationEvent {
    }

    record BookingRejectedIntegrationEvent(UUID giftCardReference, Money amount) implements BookingResultIntegrationEvent {
    }
}
