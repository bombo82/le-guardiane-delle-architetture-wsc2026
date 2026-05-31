package it.giannibombelli.wsc2026.payment.application.usecases;

import it.giannibombelli.wsc2026.common.utils.Require;

import it.giannibombelli.wsc2026.common.application.UseCase;
import it.giannibombelli.wsc2026.common.application.events.EventPublisher;
import it.giannibombelli.wsc2026.payment.application.commands.RequestPayment;
import it.giannibombelli.wsc2026.payment.domain.events.PaymentEvent;
import it.giannibombelli.wsc2026.payment.domain.events.PaymentRequested;
import it.giannibombelli.wsc2026.payment.domain.payment.Payment;
import it.giannibombelli.wsc2026.payment.domain.ports.PaymentRepository;

import static java.util.Objects.requireNonNull;

public final class PaymentRequesting implements UseCase<RequestPayment, PaymentRequested> {
    private final PaymentRepository paymentRepository;
    private final EventPublisher<PaymentEvent> eventPublisher;

    public PaymentRequesting(PaymentRepository paymentRepository, EventPublisher<PaymentEvent> eventPublisher) {
        this.paymentRepository = requireNonNull(paymentRepository);
        this.eventPublisher = requireNonNull(eventPublisher);
    }

    @Override
    public PaymentRequested invoke(RequestPayment cmd) {
        Require.requireArgument(cmd, "command");

        Payment payment = Payment.request(
            cmd.aggregateId(),
            cmd.clientReference(),
            cmd.amount(),
            cmd.requestedAt()
        );

        paymentRepository.save(payment);
        PaymentRequested event = new PaymentRequested(
            payment.id(),
            payment.clientReference(),
            payment.amount()
        );
        eventPublisher.publish(event);
        return event;
    }
}
