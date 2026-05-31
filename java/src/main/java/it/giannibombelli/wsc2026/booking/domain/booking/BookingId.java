package it.giannibombelli.wsc2026.booking.domain.booking;

import it.giannibombelli.wsc2026.common.utils.Require;

import it.giannibombelli.wsc2026.common.domain.identity.AggregateId;

import java.util.UUID;

public record BookingId(UUID value) implements AggregateId {
    public BookingId {
        Require.requireArgument(value, "BookingId value");
    }
}
