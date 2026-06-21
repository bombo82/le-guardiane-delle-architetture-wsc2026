package it.giannibombelli.wsc2026.booking.application.query;

import it.giannibombelli.wsc2026.booking.domain.booking.Booking;
import it.giannibombelli.wsc2026.booking.domain.booking.BookingId;
import it.giannibombelli.wsc2026.booking.domain.ports.BookingRepository;

import java.util.Optional;

public final class BookingQueryService {
    private final BookingRepository repository;

    public BookingQueryService(BookingRepository repository) {
        this.repository = repository;
    }

    public Optional<BookingDetails> findById(BookingId id) {
        return repository.findById(id)
            .map(this::toDetails);
    }

    private BookingDetails toDetails(Booking booking) {
        return new BookingDetails(
            booking.id().value(),
            booking.description(),
            booking.giftCardId().value()
        );
    }
}
