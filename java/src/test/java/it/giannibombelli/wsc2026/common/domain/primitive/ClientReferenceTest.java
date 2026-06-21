package it.giannibombelli.wsc2026.common.domain.primitive;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ClientReferenceTest {

    @Test
    void shouldCreateWithValidValue() {
        UUID uuid = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
        ClientReference reference = new ClientReference(uuid);

        assertThat(reference.value()).isEqualTo(uuid);
        assertThat(reference.toString()).isEqualTo(uuid.toString());
    }

    @Test
    void shouldFailIfNull() {
        assertThatThrownBy(() -> new ClientReference((UUID) null))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
