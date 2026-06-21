package it.giannibombelli.wsc2026.booking.infrastructure;

import it.giannibombelli.wsc2026.booking.domain.booking.Booking;
import it.giannibombelli.wsc2026.booking.domain.booking.BookingId;
import it.giannibombelli.wsc2026.booking.domain.booking.BookingStatus;
import it.giannibombelli.wsc2026.booking.domain.ports.BookingRepository;
import it.giannibombelli.wsc2026.booking.domain.primitive.GiftCardReference;
import it.giannibombelli.wsc2026.common.domain.identity.EntityId;
import it.giannibombelli.wsc2026.common.domain.primitive.Description;
import it.giannibombelli.wsc2026.testsupport.DatabaseSetup;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import javax.sql.DataSource;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BookingRepositoryTest {
    private BookingRepository repository;

    @BeforeAll
    void setUp() {
        final DataSource dataSource = DatabaseSetup.initializeFileDb("booking", getClass().getSimpleName());
        repository = new SqliteBookingRepository(dataSource);
    }

    @Nested
    class Save {
        @Test
        void shouldPersistNewBooking() {
            BookingId bookingId = EntityId.generate(BookingId::new);
            GiftCardReference giftCardReference = new GiftCardReference(UUID.randomUUID());
            Booking original = new Booking(bookingId, new Description("Test description"), giftCardReference, BookingStatus.PLACED);

            repository.save(original);
            Optional<Booking> reloaded = repository.findById(bookingId);

            assertThat(reloaded).isPresent();
            assertThat(reloaded.get().id()).isEqualTo(bookingId);
            assertThat(reloaded.get().description()).isEqualTo(original.description());
            assertThat(reloaded.get().giftCardReference()).isEqualTo(giftCardReference);
            assertThat(reloaded.get().status()).isEqualTo(BookingStatus.PLACED);
        }
    }

    @Nested
    class FindById {
        @Test
        void shouldReturnEmptyWhenNotFound() {
            BookingId nonExistentId = EntityId.generate(BookingId::new);

            Optional<Booking> reloaded = repository.findById(nonExistentId);

            assertThat(reloaded).isEmpty();
        }
    }
}
