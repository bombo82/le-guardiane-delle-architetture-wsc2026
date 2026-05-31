package it.giannibombelli.wsc2026.booking.domain.policies;

import it.giannibombelli.wsc2026.common.utils.Require;

import it.giannibombelli.wsc2026.booking.domain.events.BookingResultEvents;
import it.giannibombelli.wsc2026.common.domain.model.Policy;
import it.giannibombelli.wsc2026.common.domain.primitive.ClientReference;
import it.giannibombelli.wsc2026.payment.application.commands.RefundTransaction;
import it.giannibombelli.wsc2026.payment.domain.payment.Payment;
import it.giannibombelli.wsc2026.payment.domain.ports.PaymentRepository;

import static java.util.Objects.requireNonNull;



public class BookingRefundRequestPolicy implements Policy<BookingResultEvents.BookingRefused, RefundTransaction> {

    private final PaymentRepository paymentRepository;

    public BookingRefundRequestPolicy(PaymentRepository paymentRepository) {
        this.paymentRepository = requireNonNull(paymentRepository);
    }

    @Override
    public RefundTransaction evaluate(BookingResultEvents.BookingRefused event) {
        Require.requireArgument(event, "Booking refused event");

        ClientReference clientReference = new ClientReference(event.aggregateId().value().toString());
        Payment payment = paymentRepository.findByClientReference(clientReference)
            .orElseThrow(() -> new IllegalStateException("Payment not found for booking: " + clientReference.value()));

        return new RefundTransaction(payment.id(), event.amount());
    }
}
