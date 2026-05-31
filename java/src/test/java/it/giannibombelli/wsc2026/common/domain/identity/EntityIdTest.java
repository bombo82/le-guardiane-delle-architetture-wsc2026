package it.giannibombelli.wsc2026.common.domain.identity;

import it.giannibombelli.wsc2026.booking.domain.booking.BookingId;
import it.giannibombelli.wsc2026.giftcard.domain.giftcard.GiftCardId;
import it.giannibombelli.wsc2026.payment.domain.payment.PaymentId;
import it.giannibombelli.wsc2026.payment.domain.payment.TransactionId;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EntityIdTest {

    @Nested
    class GiftCardIdValidation {
        @Test
        void shouldCreateWithValidValue() {
            UUID uuid = UUID.randomUUID();

            GiftCardId id = new GiftCardId(uuid);

            assertThat(id.value()).isEqualTo(uuid);
        }

        @Test
        void shouldFailIfNull() {
            assertThatThrownBy(() -> new GiftCardId(null))
                .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    class BookingIdValidation {
        @Test
        void shouldCreateWithValidValue() {
            UUID uuid = UUID.randomUUID();

            BookingId id = new BookingId(uuid);

            assertThat(id.value()).isEqualTo(uuid);
        }

        @Test
        void shouldFailIfNull() {
            assertThatThrownBy(() -> new BookingId(null))
                .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    class PaymentIdValidation {
        @Test
        void shouldCreateWithValidValue() {
            UUID uuid = UUID.randomUUID();

            PaymentId id = new PaymentId(uuid);

            assertThat(id.value()).isEqualTo(uuid);
        }

        @Test
        void shouldFailIfNull() {
            assertThatThrownBy(() -> new PaymentId(null))
                .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    class TransactionIdValidation {
        @Test
        void shouldCreateWithValidValue() {
            UUID uuid = UUID.randomUUID();

            TransactionId id = new TransactionId(uuid);

            assertThat(id.value()).isEqualTo(uuid);
        }

        @Test
        void shouldFailIfNull() {
            assertThatThrownBy(() -> new TransactionId(null))
                .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    class Generation {
        @Test
        void shouldGenerateNonNullId() {
            PaymentId id = EntityId.generate(PaymentId::new);

            assertThat(id.value()).isNotNull();
        }
    }
}
