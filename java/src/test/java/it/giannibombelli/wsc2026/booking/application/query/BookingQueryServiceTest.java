package it.giannibombelli.wsc2026.booking.application.query;

import it.giannibombelli.wsc2026.booking.domain.booking.Booking;
import it.giannibombelli.wsc2026.booking.domain.booking.BookingId;
import it.giannibombelli.wsc2026.booking.domain.ports.BookingRepository;
import it.giannibombelli.wsc2026.booking.domain.primitive.GiftCardReference;
import it.giannibombelli.wsc2026.common.domain.identity.EntityId;
import it.giannibombelli.wsc2026.common.domain.primitive.Description;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class BookingQueryServiceTest {

    @Test
    void shouldReturnBookingDetailsWhenBookingExists() {
        InMemoryBookingRepository repository = new InMemoryBookingRepository();
        BookingQueryService queryService = new BookingQueryService(repository);
        BookingId bookingId = EntityId.generate(BookingId::new);
        GiftCardReference giftCardReference = new GiftCardReference(UUID.randomUUID());
        Description description = new Description("summer stay");
        Booking booking = Booking.place(bookingId, description, giftCardReference);
        repository.save(booking);

        Optional<BookingDetails> result = queryService.findById(bookingId);

        assertThat(result).isPresent();
        assertThat(result.get().id()).isEqualTo(bookingId.value());
        assertThat(result.get().description()).isEqualTo(description);
        assertThat(result.get().giftCardId()).isEqualTo(giftCardReference.value());
    }

    @Test
    void shouldReturnEmptyWhenBookingDoesNotExist() {
        InMemoryBookingRepository repository = new InMemoryBookingRepository();
        BookingQueryService queryService = new BookingQueryService(repository);
        BookingId bookingId = EntityId.generate(BookingId::new);

        Optional<BookingDetails> result = queryService.findById(bookingId);

        assertThat(result).isEmpty();
    }

    private static final class InMemoryBookingRepository implements BookingRepository {
        private final Map<UUID, Booking> bookings = new HashMap<>();

        @Override
        public void save(Booking booking) {
            bookings.put(booking.id().value(), booking);
        }

        @Override
        public Optional<Booking> findById(BookingId id) {
            return Optional.ofNullable(bookings.get(id.value()));
        }
    }
}
