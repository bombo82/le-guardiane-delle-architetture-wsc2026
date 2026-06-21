package it.giannibombelli.wsc2026.booking.application.usecases;

import it.giannibombelli.wsc2026.booking.domain.booking.Booking;
import it.giannibombelli.wsc2026.booking.domain.ports.BookingRepository;
import it.giannibombelli.wsc2026.booking.infrastructure.SqliteBookingRepository;
import it.giannibombelli.wsc2026.common.domain.identity.EntityId;
import it.giannibombelli.wsc2026.common.domain.primitive.Money;
import it.giannibombelli.wsc2026.testsupport.AggregateFactory;
import it.giannibombelli.wsc2026.testsupport.AggregateFactory;
import it.giannibombelli.wsc2026.testsupport.DatabaseSetup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.math.BigDecimal;

import static it.giannibombelli.wsc2026.booking.application.commands.BookingConfirmationCommands.RejectBooking;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BookingRejectingTest {
    private BookingRepository repository;
    private BookingRejecting rejection;

    @BeforeEach
    void setUp() {
        final DataSource dataSource = DatabaseSetup.initializeInMemoryDb("booking");
        repository = new SqliteBookingRepository(dataSource);
        rejection = new BookingRejecting(repository, event -> {
        });
    }

    @Nested
    class Construction {
        @Test
        void rejectNullRepository() {
            assertThatThrownBy(() -> new BookingRejecting(null, event -> {
            }))
                .isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    class Execution {
        @Test
        void rejectNullCommand() {
            assertThatThrownBy(() -> rejection.invoke(null))
                .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void shouldReturnBookingRejected() {
            Booking booking = AggregateFactory.createBooking();
            repository.save(booking);
            Money amount = new Money(new BigDecimal("75.00"));

            var event = rejection.invoke(new RejectBooking(booking.id(), booking.giftCardReference(), amount));

            assertThat(event.aggregateId()).isEqualTo(booking.id());
            assertThat(event.giftCardReference()).isEqualTo(booking.giftCardReference().value());
            assertThat(event.amount()).isEqualTo(amount);
        }

        @Test
        void shouldFailIfBookingNotFound() {
            var nonExistingId = EntityId.generate(it.giannibombelli.wsc2026.booking.domain.booking.BookingId::new);

            assertThatThrownBy(() -> rejection.invoke(new RejectBooking(nonExistingId, AggregateFactory.createBooking().giftCardReference(), new Money(new BigDecimal("10.00")))))
                .isInstanceOf(IllegalStateException.class);
        }
    }
}
