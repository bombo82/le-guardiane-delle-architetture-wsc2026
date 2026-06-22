package it.giannibombelli.wsc2026.booking.application.integration.payment.adapter;

import it.giannibombelli.wsc2026.booking.application.commands.BookingConfirmationCommands;
import it.giannibombelli.wsc2026.booking.domain.booking.Booking;
import it.giannibombelli.wsc2026.booking.domain.booking.BookingId;
import it.giannibombelli.wsc2026.booking.domain.ports.BookingRepository;
import it.giannibombelli.wsc2026.common.utils.Require;
import it.giannibombelli.wsc2026.payment.integration.PaymentResultIntegrationEvent;

import java.util.UUID;

import static it.giannibombelli.wsc2026.booking.application.commands.BookingConfirmationCommands.ConfirmBooking;
import static it.giannibombelli.wsc2026.booking.application.commands.BookingConfirmationCommands.RejectBooking;

/**
 * Anti-Corruption Layer che traduce la Published Language esposta da {@code payment}
 * nei command interni del BC {@code booking}.
 * <p>
 * Isola il modello di {@code booking} da eventuali evoluzioni del modello interno di {@code payment}:
 * l'unico contratto condiviso è {@link PaymentResultIntegrationEvent}.
 */
public final class PaymentResult {

    private final BookingRepository bookingRepository;

    public PaymentResult(BookingRepository bookingRepository) {
        Require.requireDependency(bookingRepository, "bookingRepository");
        this.bookingRepository = bookingRepository;
    }

    public BookingConfirmationCommands adapt(PaymentResultIntegrationEvent event) {
        Require.requireArgument(event, "Payment result integration event");
        return switch (event) {
            case PaymentResultIntegrationEvent.PaymentAcceptedIntegrationEvent it -> buildConfirmBooking(UUID.fromString(it.clientReference()), it);
            case PaymentResultIntegrationEvent.PaymentRejectedIntegrationEvent it -> buildRejectBooking(UUID.fromString(it.clientReference()), it);
            case PaymentResultIntegrationEvent.PaymentExpiredIntegrationEvent ignored -> null;
        };
    }

    private BookingConfirmationCommands buildConfirmBooking(UUID bookingId, PaymentResultIntegrationEvent.PaymentAcceptedIntegrationEvent event) {
        Booking booking = findBooking(bookingId);
        if (booking == null) {
            return null;
        }
        return new ConfirmBooking(new BookingId(bookingId), booking.giftCardReference(), event.amount());
    }

    private BookingConfirmationCommands buildRejectBooking(UUID bookingId, PaymentResultIntegrationEvent.PaymentRejectedIntegrationEvent event) {
        Booking booking = findBooking(bookingId);
        if (booking == null) {
            return null;
        }
        return new RejectBooking(new BookingId(bookingId), booking.giftCardReference(), event.amount());
    }

    private Booking findBooking(UUID bookingId) {
        return bookingRepository.findById(new BookingId(bookingId)).orElse(null);
    }
}
