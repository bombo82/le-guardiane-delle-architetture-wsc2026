package it.giannibombelli.wsc2026.booking.application.usecases;

import it.giannibombelli.wsc2026.booking.application.commands.PlaceBooking;
import it.giannibombelli.wsc2026.booking.domain.booking.Booking;
import it.giannibombelli.wsc2026.booking.domain.booking.BookingId;
import it.giannibombelli.wsc2026.booking.domain.ports.BookingRepository;
import it.giannibombelli.wsc2026.booking.infrastructure.SqliteBookingRepository;
import it.giannibombelli.wsc2026.common.domain.identity.EntityId;
import it.giannibombelli.wsc2026.common.domain.primitive.Description;
import it.giannibombelli.wsc2026.common.domain.primitive.Money;
import it.giannibombelli.wsc2026.giftcard.domain.giftcard.GiftCardId;
import it.giannibombelli.wsc2026.testsupport.DatabaseSetup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BookingPlacingTest {
    private BookingRepository repository;
    private BookingPlacing placement;

    @BeforeEach
    void setUp() {
        final DataSource dataSource = DatabaseSetup.initializeInMemoryDb("booking");
        repository = new SqliteBookingRepository(dataSource);
        placement = new BookingPlacing(repository, event -> {
        });
    }

    @Nested
    class Construction {
        @Test
        void rejectNullRepository() {
            assertThatThrownBy(() -> new BookingPlacing(null, event -> {
            }))
                .isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    class Execution {
        @Test
        void rejectNullCommand() {
            assertThatThrownBy(() -> placement.invoke(null))
                .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void shouldPersist() {
            BookingId bookingId = EntityId.generate(BookingId::new);
            Money amount = new Money(new BigDecimal("123.45"));
            Description description = new Description("Some description");
            GiftCardId giftCardId = EntityId.generate(GiftCardId::new);

            placement.invoke(new PlaceBooking(bookingId, amount, description, giftCardId));

            Optional<Booking> loaded = repository.findById(bookingId);
            assertThat(loaded).isPresent();
            assertThat(loaded.get().id()).isEqualTo(bookingId);
            assertThat(loaded.get().description()).isEqualTo(description);
            assertThat(loaded.get().giftCardId()).isEqualTo(giftCardId);
        }
    }
}
