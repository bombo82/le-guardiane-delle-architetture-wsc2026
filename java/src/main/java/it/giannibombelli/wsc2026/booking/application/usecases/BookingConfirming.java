package it.giannibombelli.wsc2026.booking.application.usecases;

import it.giannibombelli.wsc2026.common.utils.Require;

import it.giannibombelli.wsc2026.booking.domain.booking.Booking;
import it.giannibombelli.wsc2026.booking.domain.events.BookingEvent;
import it.giannibombelli.wsc2026.booking.domain.events.BookingResultEvents;
import it.giannibombelli.wsc2026.booking.domain.ports.BookingRepository;
import it.giannibombelli.wsc2026.common.application.UseCase;
import it.giannibombelli.wsc2026.common.application.events.EventPublisher;

import static it.giannibombelli.wsc2026.booking.application.commands.BookingConfirmationCommands.ConfirmBooking;
import static java.util.Objects.requireNonNull;

public final class BookingConfirming implements UseCase<ConfirmBooking, BookingResultEvents> {
    private final BookingRepository bookingRepository;
    private final EventPublisher<BookingEvent> eventPublisher;

    public BookingConfirming(BookingRepository bookingRepository,
                             EventPublisher<BookingEvent> eventPublisher) {
        this.bookingRepository = requireNonNull(bookingRepository);
        this.eventPublisher = requireNonNull(eventPublisher);
    }

    @Override
    public BookingResultEvents invoke(ConfirmBooking cmd) {
        Require.requireArgument(cmd, "command");

        Booking booking = bookingRepository.findById(cmd.aggregateId())
            .orElseThrow(() -> new IllegalStateException("Booking not found for confirmation: " + cmd.aggregateId()));

        BookingResultEvents result = booking.confirm(cmd.giftCardId(), cmd.amount());

        bookingRepository.save(booking);
        eventPublisher.publish(result);
        return result;
    }
}
