package it.giannibombelli.wsc2026.booking.application.usecases;

import it.giannibombelli.wsc2026.common.utils.Require;

import it.giannibombelli.wsc2026.booking.application.commands.PlaceBooking;
import it.giannibombelli.wsc2026.booking.domain.booking.Booking;
import it.giannibombelli.wsc2026.booking.domain.events.BookingEvent;
import it.giannibombelli.wsc2026.booking.domain.events.BookingPlaced;
import it.giannibombelli.wsc2026.booking.domain.ports.BookingRepository;
import it.giannibombelli.wsc2026.common.application.UseCase;
import it.giannibombelli.wsc2026.common.application.events.EventPublisher;

import static java.util.Objects.requireNonNull;

public final class BookingPlacing implements UseCase<PlaceBooking, BookingPlaced> {
    private final BookingRepository bookingRepository;
    private final EventPublisher<BookingEvent> eventPublisher;

    public BookingPlacing(BookingRepository bookingRepository,
                          EventPublisher<BookingEvent> eventPublisher) {
        this.bookingRepository = requireNonNull(bookingRepository);
        this.eventPublisher = requireNonNull(eventPublisher);
    }

    @Override
    public BookingPlaced invoke(PlaceBooking cmd) {
        Require.requireArgument(cmd, "command");

        Booking booking = Booking.place(cmd.aggregateId(), cmd.description(), cmd.giftCardReference());

        bookingRepository.save(booking);

        BookingPlaced bookingPlaced = new BookingPlaced(booking.id(), cmd.amount(), booking.description(), booking.giftCardReference().value());
        eventPublisher.publish(bookingPlaced);

        return bookingPlaced;
    }
}
