package it.giannibombelli.wsc2026.booking.domain.booking;

import it.giannibombelli.wsc2026.common.domain.identity.EntityId;
import it.giannibombelli.wsc2026.common.domain.primitive.Description;
import it.giannibombelli.wsc2026.common.domain.primitive.Money;
import it.giannibombelli.wsc2026.giftcard.domain.giftcard.GiftCardId;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

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
            GiftCardId giftCardId = EntityId.generate(GiftCardId::new);

            Booking booking = Booking.place(id, description, giftCardId);

            assertThat(booking.id()).isEqualTo(id);
            assertThat(booking.description()).isEqualTo(description);
            assertThat(booking.giftCardId()).isEqualTo(giftCardId);
            assertThat(booking.status()).isEqualTo(BookingStatus.PLACED);
        }

        @Test
        void shouldFailIfParametersAreInvalid() {
            BookingId id = EntityId.generate(BookingId::new);
            Description description = new Description("Some description");
            GiftCardId giftCardId = EntityId.generate(GiftCardId::new);

            assertThatThrownBy(() -> Booking.place(null, description, giftCardId))
                .isInstanceOf(IllegalArgumentException.class);

            assertThatThrownBy(() -> Booking.place(id, null, giftCardId))
                .isInstanceOf(IllegalArgumentException.class);

            assertThatThrownBy(() -> Booking.place(id, description, null))
                .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    class Confirm {
        @Test
        void shouldEmitBookingConfirmed() {
            GiftCardId giftCardId = EntityId.generate(GiftCardId::new);
            Booking booking = Booking.place(EntityId.generate(BookingId::new), new Description("Test booking"), giftCardId);
            Money amount = new Money(new BigDecimal("75.00"));

            var event = booking.confirm(giftCardId, amount);

            assertThat(event).isInstanceOf(it.giannibombelli.wsc2026.booking.domain.events.BookingResultEvents.BookingConfirmed.class);
            var confirmed = (it.giannibombelli.wsc2026.booking.domain.events.BookingResultEvents.BookingConfirmed) event;
            assertThat(confirmed.aggregateId()).isEqualTo(booking.id());
            assertThat(confirmed.giftCardId()).isEqualTo(giftCardId);
            assertThat(confirmed.amount()).isEqualTo(amount);
        }

        @Test
        void shouldFailIfParametersAreInvalid() {
            GiftCardId validGiftCardId = EntityId.generate(GiftCardId::new);
            Booking booking = createBooking();

            assertThatThrownBy(() -> booking.confirm(null, new Money(new BigDecimal("10.00"))))
                .isInstanceOf(IllegalArgumentException.class);

            assertThatThrownBy(() -> booking.confirm(validGiftCardId, null))
                .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    class Reject {
        @Test
        void shouldEmitBookingRejected() {
            GiftCardId giftCardId = EntityId.generate(GiftCardId::new);
            Booking booking = Booking.place(EntityId.generate(BookingId::new), new Description("Test booking"), giftCardId);
            Money amount = new Money(new BigDecimal("25.00"));

            var event = booking.reject(giftCardId, amount);

            assertThat(event.aggregateId()).isEqualTo(booking.id());
            assertThat(event.giftCardId()).isEqualTo(giftCardId);
            assertThat(event.amount()).isEqualTo(amount);
        }

        @Test
        void shouldFailIfParametersAreInvalid() {
            GiftCardId validGiftCardId = EntityId.generate(GiftCardId::new);
            Booking booking = createBooking();

            assertThatThrownBy(() -> booking.reject(null, new Money(new BigDecimal("10.00"))))
                .isInstanceOf(IllegalArgumentException.class);

            assertThatThrownBy(() -> booking.reject(validGiftCardId, null))
                .isInstanceOf(IllegalArgumentException.class);
        }
    }
}
