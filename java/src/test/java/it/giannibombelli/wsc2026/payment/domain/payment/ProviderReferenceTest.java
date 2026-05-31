package it.giannibombelli.wsc2026.payment.domain.payment;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProviderReferenceTest {

    @Test
    void shouldCreateWithValidValue() {
        UUID uuid = UUID.randomUUID();

        ProviderReference reference = new ProviderReference(uuid);

        assertThat(reference.value()).isEqualTo(uuid);
    }

    @Test
    void shouldFailIfNull() {
        assertThatThrownBy(() -> new ProviderReference(null))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
