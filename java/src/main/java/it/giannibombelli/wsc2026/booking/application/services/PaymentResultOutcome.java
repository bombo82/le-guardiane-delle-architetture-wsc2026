package it.giannibombelli.wsc2026.booking.application.services;

import it.giannibombelli.wsc2026.booking.application.commands.BookingConfirmationCommands;
import it.giannibombelli.wsc2026.booking.application.usecases.BookingConfirming;
import it.giannibombelli.wsc2026.booking.application.usecases.BookingRejecting;
import it.giannibombelli.wsc2026.booking.domain.policies.PaymentPolicy;
import it.giannibombelli.wsc2026.payment.domain.events.PaymentResultEvents;

import static it.giannibombelli.wsc2026.booking.application.commands.BookingConfirmationCommands.ConfirmBooking;
import static it.giannibombelli.wsc2026.booking.application.commands.BookingConfirmationCommands.RejectBooking;
import static java.util.Objects.requireNonNull;


public class PaymentResultOutcome {
    private final PaymentPolicy policy;
    private final BookingConfirming confirmation;
    private final BookingRejecting rejection;

    public PaymentResultOutcome(PaymentPolicy policy, BookingConfirming confirmation, BookingRejecting rejection) {
        this.policy = requireNonNull(policy);
        this.confirmation = requireNonNull(confirmation);
        this.rejection = requireNonNull(rejection);
    }

    public void handlePaymentResults(PaymentResultEvents event) {
        BookingConfirmationCommands cmd = policy.evaluate(event);
        if (cmd == null) {
            return;
        }
        switch (cmd) {
            case ConfirmBooking confirmBooking -> confirmation.invoke(confirmBooking);
            case RejectBooking rejectBooking -> rejection.invoke(rejectBooking);
        }
    }
}
