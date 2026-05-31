package it.giannibombelli.wsc2026.booking.domain.booking;

import it.giannibombelli.wsc2026.common.domain.primitive.Description;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DescriptionTest {

    @Test
    void shouldCreateWithValidValue() {
        Description description = new Description("Nice trip");
        assertThat(description.value()).isEqualTo("Nice trip");
    }

    @Test
    void shouldFailIfNull() {
        assertThatThrownBy(() -> new Description(null))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldFailIfBlank() {
        assertThatThrownBy(() -> new Description(""))
            .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> new Description("   "))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
