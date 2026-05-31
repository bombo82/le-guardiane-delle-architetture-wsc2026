package it.giannibombelli.wsc2026.common.domain.primitive;

import it.giannibombelli.wsc2026.common.utils.Require;

import java.time.Instant;

public record Timestamp(Instant value) {
    public Timestamp {
        Require.requireArgument(value, "timestamp");
    }

    public static Timestamp now() {
        return new Timestamp(Instant.now());
    }

    public Timestamp plusSeconds(long seconds) {
        return new Timestamp(value.plusSeconds(seconds));
    }

    public Timestamp minusSeconds(long seconds) {
        return new Timestamp(value.minusSeconds(seconds));
    }

    public boolean isBefore(Timestamp other) {
        return value.isBefore(other.value);
    }

    public boolean isAfter(Timestamp other) {
        return value.isAfter(other.value);
    }

    public boolean isBeforeOrEqual(Timestamp other) {
        return !value.isAfter(other.value);
    }

    public boolean isAfterOrEqual(Timestamp other) {
        return !value.isBefore(other.value);
    }
}
