package it.giannibombelli.wsc2026.booking.domain.ports;

import it.giannibombelli.wsc2026.booking.domain.booking.Booking;
import it.giannibombelli.wsc2026.booking.domain.booking.BookingId;

import java.util.Optional;

public interface BookingRepository {
    void save(Booking booking);

    Optional<Booking> findById(BookingId id);
}
