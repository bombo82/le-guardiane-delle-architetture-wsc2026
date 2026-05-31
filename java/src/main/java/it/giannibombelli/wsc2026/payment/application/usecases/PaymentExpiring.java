package it.giannibombelli.wsc2026.payment.application.usecases;

import it.giannibombelli.wsc2026.common.utils.Require;

import it.giannibombelli.wsc2026.common.application.events.EventPublisher;
import it.giannibombelli.wsc2026.common.application.events.EventSubscriber;
import it.giannibombelli.wsc2026.payment.application.commands.ExpirePayment;
import it.giannibombelli.wsc2026.payment.domain.events.PaymentDeadlineReached;
import it.giannibombelli.wsc2026.payment.domain.events.PaymentEvent;
import it.giannibombelli.wsc2026.payment.domain.events.PaymentResultEvents;
import it.giannibombelli.wsc2026.payment.domain.payment.Payment;
import it.giannibombelli.wsc2026.payment.domain.policies.PaymentExpiration;
import it.giannibombelli.wsc2026.payment.domain.ports.PaymentRepository;

import static java.util.Objects.requireNonNull;

public final class PaymentExpiring implements EventSubscriber<PaymentDeadlineReached> {
    private final PaymentRepository paymentRepository;
    private final EventPublisher<PaymentEvent> eventPublisher;
    private final PaymentExpiration paymentExpiration;

    public PaymentExpiring(PaymentRepository paymentRepository,
                           EventPublisher<PaymentEvent> eventPublisher,
                           PaymentExpiration paymentExpiration) {
        this.paymentRepository = requireNonNull(paymentRepository);
        this.eventPublisher = requireNonNull(eventPublisher);
        this.paymentExpiration = requireNonNull(paymentExpiration);
    }

    @Override
    public void on(PaymentDeadlineReached event) {
        Require.requireArgument(event, "event");

        ExpirePayment cmd = paymentExpiration.evaluate(event);
        Payment payment = paymentRepository.findById(cmd.aggregateId())
            .orElseThrow(() -> new IllegalArgumentException("payment not found"));

        PaymentResultEvents.PaymentExpired expired = payment.expire();
        paymentRepository.save(payment);

        eventPublisher.publish(expired);
    }
}
