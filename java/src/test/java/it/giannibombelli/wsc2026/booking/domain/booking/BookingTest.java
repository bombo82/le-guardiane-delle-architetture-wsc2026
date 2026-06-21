package it.giannibombelli.wsc2026.booking.domain.booking;

import it.giannibombelli.wsc2026.booking.domain.primitive.GiftCardReference;
import it.giannibombelli.wsc2026.common.domain.identity.EntityId;
import it.giannibombelli.wsc2026.common.domain.primitive.Description;
import it.giannibombelli.wsc2026.common.domain.primitive.Money;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static it.giannibombelli.wsc2026.testsupport.AggregateFactory.createBooking;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BookingTest {

    @Nested
    class Place {
        @Test
        void shouldCreateBookingInPlacedStatus() {
            BookingId id = EntityId.generate(BookingId::new);
            Description description = new Description("Some description");
            GiftCardReference giftCardReference = new GiftCardReference(UUID.randomUUID());

            Booking booking = Booking.place(id, description, giftCardReference);

            assertThat(booking.id()).isEqualTo(id);
            assertThat(booking.description()).isEqualTo(description);
            assertThat(booking.giftCardReference()).isEqualTo(giftCardReference);
            assertThat(booking.status()).isEqualTo(BookingStatus.PLACED);
        }

        @Test
        void shouldFailIfParametersAreInvalid() {
            BookingId id = EntityId.generate(BookingId::new);
            Description description = new Description("Some description");
            GiftCardReference giftCardReference = new GiftCardReference(UUID.randomUUID());

            assertThatThrownBy(() -> Booking.place(null, description, giftCardReference))
                .isInstanceOf(IllegalArgumentException.class);

            assertThatThrownBy(() -> Booking.place(id, null, giftCardReference))
                .isInstanceOf(IllegalArgumentException.class);

            assertThatThrownBy(() -> Booking.place(id, description, null))
                .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    class Confirm {
        @Test
        void shouldEmitBookingConfirmed() {
            Booking booking = createBooking();
            Money amount = new Money(new BigDecimal("75.00"));

            var event = booking.confirm(amount);

            assertThat(event).isInstanceOf(it.giannibombelli.wsc2026.booking.domain.events.BookingResultEvents.BookingConfirmed.class);
            var confirmed = (it.giannibombelli.wsc2026.booking.domain.events.BookingResultEvents.BookingConfirmed) event;
            assertThat(confirmed.aggregateId()).isEqualTo(booking.id());
            assertThat(confirmed.giftCardReference()).isEqualTo(booking.giftCardReference().value());
            assertThat(confirmed.amount()).isEqualTo(amount);
        }

        @Test
        void shouldFailIfParametersAreInvalid() {
            Booking booking = createBooking();

            assertThatThrownBy(() -> booking.confirm(null))
                .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    class Reject {
        @Test
        void shouldEmitBookingRejected() {
            Booking booking = createBooking();
            Money amount = new Money(new BigDecimal("25.00"));

            var event = booking.reject(amount);

            assertThat(event.aggregateId()).isEqualTo(booking.id());
            assertThat(event.giftCardReference()).isEqualTo(booking.giftCardReference().value());
            assertThat(event.amount()).isEqualTo(amount);
        }

        @Test
        void shouldFailIfParametersAreInvalid() {
            Booking booking = createBooking();

            assertThatThrownBy(() -> booking.reject(null))
                .isInstanceOf(IllegalArgumentException.class);
        }
    }
}
