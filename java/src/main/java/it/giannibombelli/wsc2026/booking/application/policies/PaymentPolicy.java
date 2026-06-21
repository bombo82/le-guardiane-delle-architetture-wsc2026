package it.giannibombelli.wsc2026.booking.application.policies;

import it.giannibombelli.wsc2026.common.utils.Require;

import it.giannibombelli.wsc2026.booking.application.commands.BookingConfirmationCommands;
import it.giannibombelli.wsc2026.booking.domain.booking.Booking;
import it.giannibombelli.wsc2026.booking.domain.booking.BookingId;
import it.giannibombelli.wsc2026.booking.domain.ports.BookingRepository;
import it.giannibombelli.wsc2026.common.application.Policy;
import it.giannibombelli.wsc2026.common.domain.primitive.ClientReference;
import it.giannibombelli.wsc2026.payment.domain.events.PaymentResultEvents;

import java.util.UUID;

import static it.giannibombelli.wsc2026.booking.application.commands.BookingConfirmationCommands.ConfirmBooking;
import static it.giannibombelli.wsc2026.booking.application.commands.BookingConfirmationCommands.RejectBooking;
import static it.giannibombelli.wsc2026.payment.domain.events.PaymentResultEvents.*;


public final class PaymentPolicy implements Policy<PaymentResultEvents, BookingConfirmationCommands> {
    private final BookingRepository bookingRepository;

    public PaymentPolicy(BookingRepository bookingRepository) {
        Require.requireDependency(bookingRepository, "bookingRepository");
        this.bookingRepository = bookingRepository;
    }

    @Override
    public BookingConfirmationCommands evaluate(PaymentResultEvents event) {
        Require.requireArgument(event, "Payment event");
        return switch (event) {
            case PaymentAccepted it -> {
                Booking booking = findBooking(it.clientReference());
                yield booking == null
                    ? null
                    : new ConfirmBooking(bookingIdFrom(it.clientReference()), booking.giftCardReference(), it.amount());
            }
            case PaymentRejected it -> {
                Booking booking = findBooking(it.clientReference());
                yield booking == null
                    ? null
                    : new RejectBooking(bookingIdFrom(it.clientReference()), booking.giftCardReference(), it.amount());
            }
            case PaymentExpired it -> null;
        };
    }

    private Booking findBooking(ClientReference clientReference) {
        return bookingRepository.findById(bookingIdFrom(clientReference)).orElse(null);
    }

    private BookingId bookingIdFrom(ClientReference clientReference) {
        return new BookingId(clientReference.value());
    }
}
