package it.giannibombelli.wsc2026.booking.application.usecases;

import it.giannibombelli.wsc2026.booking.domain.booking.Booking;
import it.giannibombelli.wsc2026.booking.domain.ports.BookingRepository;
import it.giannibombelli.wsc2026.booking.infrastructure.SqliteBookingRepository;
import it.giannibombelli.wsc2026.common.domain.identity.EntityId;
import it.giannibombelli.wsc2026.common.domain.primitive.Money;
import it.giannibombelli.wsc2026.giftcard.domain.giftcard.GiftCardId;
import it.giannibombelli.wsc2026.testsupport.AggregateFactory;
import it.giannibombelli.wsc2026.testsupport.DatabaseSetup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.math.BigDecimal;

import static it.giannibombelli.wsc2026.booking.application.commands.BookingConfirmationCommands.ConfirmBooking;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BookingConfirmingTest {
    private BookingRepository repository;
    private BookingConfirming confirmation;

    @BeforeEach
    void setUp() {
        final DataSource dataSource = DatabaseSetup.initializeInMemoryDb("booking");
        repository = new SqliteBookingRepository(dataSource);
        confirmation = new BookingConfirming(repository, event -> {
        });
    }

    @Nested
    class Construction {
        @Test
        void rejectNullRepository() {
            assertThatThrownBy(() -> new BookingConfirming(null, event -> {
            }))
                .isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    class Execution {
        @Test
        void rejectNullCommand() {
            assertThatThrownBy(() -> confirmation.invoke(null))
                .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void shouldReturnBookingConfirmed() {
            Booking booking = AggregateFactory.createBooking();
            repository.save(booking);
            GiftCardId giftCardId = EntityId.generate(GiftCardId::new);
            Money amount = new Money(new BigDecimal("75.00"));

            var event = confirmation.invoke(new ConfirmBooking(booking.id(), giftCardId, amount));

            assertThat(event).isInstanceOf(it.giannibombelli.wsc2026.booking.domain.events.BookingResultEvents.BookingConfirmed.class);
            var confirmed = (it.giannibombelli.wsc2026.booking.domain.events.BookingResultEvents.BookingConfirmed) event;
            assertThat(confirmed.aggregateId()).isEqualTo(booking.id());
            assertThat(confirmed.giftCardId()).isEqualTo(giftCardId);
            assertThat(confirmed.amount()).isEqualTo(amount);
        }

        @Test
        void shouldFailIfBookingNotFound() {
            var nonExistingId = EntityId.generate(it.giannibombelli.wsc2026.booking.domain.booking.BookingId::new);
            GiftCardId giftCardId = EntityId.generate(GiftCardId::new);

            assertThatThrownBy(() -> confirmation.invoke(new ConfirmBooking(nonExistingId, giftCardId, new Money(new BigDecimal("10.00")))))
                .isInstanceOf(IllegalStateException.class);
        }
    }
}
