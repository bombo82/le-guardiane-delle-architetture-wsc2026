package it.giannibombelli.wsc2026.common.domain.primitive;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ClientReferenceTest {

    @Test
    void shouldCreateWithValidValue() {
        ClientReference reference = new ClientReference("client-123");

        assertThat(reference.value()).isEqualTo("client-123");
    }

    @Test
    void shouldFailIfNull() {
        assertThatThrownBy(() -> new ClientReference(null))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldFailIfBlank() {
        assertThatThrownBy(() -> new ClientReference(""))
            .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> new ClientReference("   "))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
