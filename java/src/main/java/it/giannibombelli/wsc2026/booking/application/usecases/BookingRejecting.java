package it.giannibombelli.wsc2026.booking.application.usecases;

import it.giannibombelli.wsc2026.common.utils.Require;

import it.giannibombelli.wsc2026.booking.domain.booking.Booking;
import it.giannibombelli.wsc2026.booking.domain.events.BookingEvent;
import it.giannibombelli.wsc2026.booking.domain.events.BookingResultEvents;
import it.giannibombelli.wsc2026.booking.domain.ports.BookingRepository;
import it.giannibombelli.wsc2026.common.application.UseCase;
import it.giannibombelli.wsc2026.common.application.events.EventPublisher;

import static it.giannibombelli.wsc2026.booking.application.commands.BookingConfirmationCommands.RejectBooking;
import static java.util.Objects.requireNonNull;

public final class BookingRejecting implements UseCase<RejectBooking, BookingResultEvents.BookingRejected> {
    private final BookingRepository bookingRepository;
    private final EventPublisher<BookingEvent> eventPublisher;

    public BookingRejecting(BookingRepository bookingRepository,
                            EventPublisher<BookingEvent> eventPublisher) {
        this.bookingRepository = requireNonNull(bookingRepository);
        this.eventPublisher = requireNonNull(eventPublisher);
    }

    @Override
    public BookingResultEvents.BookingRejected invoke(RejectBooking cmd) {
        Require.requireArgument(cmd, "command");

        Booking booking = bookingRepository.findById(cmd.aggregateId())
            .orElseThrow(() -> new IllegalStateException("Booking not found for rejection: " + cmd.aggregateId()));

        BookingResultEvents.BookingRejected rejected = booking.reject(cmd.amount());

        bookingRepository.save(booking);
        eventPublisher.publish(rejected);
        return rejected;
    }
}
