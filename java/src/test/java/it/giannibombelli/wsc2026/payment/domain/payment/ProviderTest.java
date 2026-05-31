package it.giannibombelli.wsc2026.payment.domain.payment;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProviderTest {

    @Test
    void shouldResolveKnownProviders() {
        assertThat(Provider.fromLabel("PayPal")).isEqualTo(Provider.PAYPAL);
        assertThat(Provider.fromLabel("Klarna")).isEqualTo(Provider.KLARNA);
        assertThat(Provider.fromLabel("GiftCard")).isEqualTo(Provider.GIFT_CARD);
    }

    @Test
    void shouldFailForNullLabel() {
        assertThatThrownBy(() -> Provider.fromLabel(null))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldFailForUnknownLabel() {
        assertThatThrownBy(() -> Provider.fromLabel("Unknown"))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
