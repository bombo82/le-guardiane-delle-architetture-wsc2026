package it.giannibombelli.wsc2026.common.domain.primitive;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TimestampTest {

    @Test
    void shouldCreateWithValidValue() {
        Instant instant = Instant.parse("2026-06-07T10:00:00Z");

        Timestamp timestamp = new Timestamp(instant);

        assertThat(timestamp.value()).isEqualTo(instant);
    }

    @Test
    void shouldFailIfNull() {
        assertThatThrownBy(() -> new Timestamp(null))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldCreateNow() {
        Instant before = Instant.now();

        Timestamp timestamp = Timestamp.now();

        Instant after = Instant.now();
        assertThat(timestamp.value()).isAfterOrEqualTo(before);
        assertThat(timestamp.value()).isBeforeOrEqualTo(after);
    }
}
