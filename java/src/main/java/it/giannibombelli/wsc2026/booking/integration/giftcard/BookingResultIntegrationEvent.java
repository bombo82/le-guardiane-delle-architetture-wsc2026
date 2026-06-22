package it.giannibombelli.wsc2026.booking.integration.giftcard;

import it.giannibombelli.wsc2026.common.domain.primitive.Money;

import java.util.UUID;

/**
 * Published Language esposta dal BC {@code booking} verso il BC {@code giftcard}.
 * <p>
 * Rappresenta il risultato di una prenotazione senza esporre gli eventi di dominio interni di {@code booking}.
 * I campi sono espressi con tipi stabili e condivisi (UUID, Money) per mantenere il contratto leggero e indipendente
 * dall'implementazione dei due BC.
 */
public sealed interface BookingResultIntegrationEvent {

    record BookingCompletedIntegrationEvent(UUID giftCardReference, Money amount) implements BookingResultIntegrationEvent {
    }

    record BookingRefusedIntegrationEvent(UUID giftCardReference, Money amount) implements BookingResultIntegrationEvent {
    }

    record BookingRejectedIntegrationEvent(UUID giftCardReference, Money amount) implements BookingResultIntegrationEvent {
    }
}
