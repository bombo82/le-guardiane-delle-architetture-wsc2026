package it.giannibombelli.wsc2026.booking.application.commands;

import it.giannibombelli.wsc2026.booking.domain.booking.BookingId;
import it.giannibombelli.wsc2026.common.domain.identity.EntityId;
import it.giannibombelli.wsc2026.common.domain.primitive.Description;
import it.giannibombelli.wsc2026.common.domain.primitive.Money;
import it.giannibombelli.wsc2026.giftcard.domain.giftcard.GiftCardId;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BookingCommandsTest {

    @Nested
    class PlaceBookingValidation {
        @Test
        void shouldFailIfParametersAreNull() {
            BookingId bookingId = EntityId.generate(BookingId::new);
            Money amount = new Money(BigDecimal.TEN);
            GiftCardId giftCardId = EntityId.generate(GiftCardId::new);

            assertThatThrownBy(() -> new PlaceBooking(null, amount, new Description("desc"), giftCardId))
                .isInstanceOf(IllegalArgumentException.class);

            assertThatThrownBy(() -> new PlaceBooking(bookingId, null, new Description("desc"), giftCardId))
                .isInstanceOf(IllegalArgumentException.class);

            assertThatThrownBy(() -> new PlaceBooking(bookingId, amount, null, giftCardId))
                .isInstanceOf(IllegalArgumentException.class);

            assertThatThrownBy(() -> new PlaceBooking(bookingId, amount, new Description("desc"), null))
                .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    class ConfirmBookingValidation {
        @Test
        void shouldFailIfParametersAreNull() {
            BookingId bookingId = EntityId.generate(BookingId::new);
            GiftCardId giftCardId = EntityId.generate(GiftCardId::new);
            Money amount = new Money(BigDecimal.TEN);

            assertThatThrownBy(() -> new BookingConfirmationCommands.ConfirmBooking(null, giftCardId, amount))
                .isInstanceOf(IllegalArgumentException.class);

            assertThatThrownBy(() -> new BookingConfirmationCommands.ConfirmBooking(bookingId, null, amount))
                .isInstanceOf(IllegalArgumentException.class);

            assertThatThrownBy(() -> new BookingConfirmationCommands.ConfirmBooking(bookingId, giftCardId, null))
                .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    class RejectBookingValidation {
        @Test
        void shouldFailIfParametersAreNull() {
            BookingId bookingId = EntityId.generate(BookingId::new);
            GiftCardId giftCardId = EntityId.generate(GiftCardId::new);
            Money amount = new Money(BigDecimal.TEN);

            assertThatThrownBy(() -> new BookingConfirmationCommands.RejectBooking(null, giftCardId, amount))
                .isInstanceOf(IllegalArgumentException.class);

            assertThatThrownBy(() -> new BookingConfirmationCommands.RejectBooking(bookingId, null, amount))
                .isInstanceOf(IllegalArgumentException.class);

            assertThatThrownBy(() -> new BookingConfirmationCommands.RejectBooking(bookingId, giftCardId, null))
                .isInstanceOf(IllegalArgumentException.class);
        }
    }
}
