package it.giannibombelli.wsc2026.booking.application.integration.payment.handlers;

import it.giannibombelli.wsc2026.booking.application.commands.BookingConfirmationCommands;
import it.giannibombelli.wsc2026.booking.application.integration.payment.adapter.PaymentResult;
import it.giannibombelli.wsc2026.booking.application.usecases.BookingConfirming;
import it.giannibombelli.wsc2026.booking.application.usecases.BookingRejecting;
import it.giannibombelli.wsc2026.common.utils.Require;
import it.giannibombelli.wsc2026.payment.integration.PaymentResultIntegrationEvent;

/**
 * Handler cross-BC che reagisce agli esiti di pagamento pubblicati da {@code payment}
 * confermando o rifiutando la prenotazione corrispondente.
 */
public final class HandlePaymentResultFromPayment {

    private final PaymentResult paymentResult;
    private final BookingConfirming confirming;
    private final BookingRejecting rejecting;

    public HandlePaymentResultFromPayment(PaymentResult paymentResult,
                                          BookingConfirming confirming,
                                          BookingRejecting rejecting) {
        Require.requireDependency(paymentResult, "paymentResult");
        Require.requireDependency(confirming, "confirming");
        Require.requireDependency(rejecting, "rejecting");
        this.paymentResult = paymentResult;
        this.confirming = confirming;
        this.rejecting = rejecting;
    }

    public void handle(PaymentResultIntegrationEvent event) {
        BookingConfirmationCommands cmd = paymentResult.adapt(event);
        if (cmd == null) {
            return;
        }
        switch (cmd) {
            case BookingConfirmationCommands.ConfirmBooking confirmBooking -> confirming.invoke(confirmBooking);
            case BookingConfirmationCommands.RejectBooking rejectBooking -> rejecting.invoke(rejectBooking);
        }
    }
}
