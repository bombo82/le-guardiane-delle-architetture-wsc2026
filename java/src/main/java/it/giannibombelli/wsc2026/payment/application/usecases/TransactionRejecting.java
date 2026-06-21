package it.giannibombelli.wsc2026.payment.application.usecases;

import it.giannibombelli.wsc2026.common.utils.Require;

import it.giannibombelli.wsc2026.common.application.events.EventPublisher;
import it.giannibombelli.wsc2026.common.application.events.EventSubscriber;
import it.giannibombelli.wsc2026.payment.application.commands.RejectTransaction;
import it.giannibombelli.wsc2026.payment.domain.events.PaymentEvent;
import it.giannibombelli.wsc2026.payment.domain.events.PaymentResultEvents;
import it.giannibombelli.wsc2026.payment.domain.events.TransactionRejected;
import it.giannibombelli.wsc2026.payment.domain.payment.Payment;
import it.giannibombelli.wsc2026.payment.application.policies.PaymentRejection;
import it.giannibombelli.wsc2026.payment.domain.ports.PaymentRepository;

import static java.util.Objects.requireNonNull;

public final class TransactionRejecting implements EventSubscriber<TransactionRejected> {
    private final PaymentRepository paymentRepository;
    private final EventPublisher<PaymentEvent> eventPublisher;
    private final PaymentRejection paymentRejection;

    public TransactionRejecting(PaymentRepository paymentRepository,
                                EventPublisher<PaymentEvent> eventPublisher,
                                PaymentRejection paymentRejection) {
        this.paymentRepository = requireNonNull(paymentRepository);
        this.eventPublisher = requireNonNull(eventPublisher);
        this.paymentRejection = requireNonNull(paymentRejection);
    }

    @Override
    public void on(TransactionRejected event) {
        Require.requireArgument(event, "event");

        RejectTransaction cmd = paymentRejection.evaluate(event);
        Payment payment = paymentRepository.findById(cmd.aggregateId())
            .orElseThrow(() -> new IllegalArgumentException("payment not found"));

        PaymentResultEvents.PaymentRejected rejected = payment.rejectTransaction(
            cmd.transactionId(),
            cmd.reason()
        );
        paymentRepository.save(payment);

        eventPublisher.publish(rejected);
    }
}
