package it.giannibombelli.wsc2026.payment.application.usecases;

import it.giannibombelli.wsc2026.common.utils.Require;

import it.giannibombelli.wsc2026.common.application.events.EventPublisher;
import it.giannibombelli.wsc2026.common.application.events.EventSubscriber;
import it.giannibombelli.wsc2026.payment.application.commands.AcceptTransaction;
import it.giannibombelli.wsc2026.payment.domain.events.PaymentEvent;
import it.giannibombelli.wsc2026.payment.domain.events.PaymentResultEvents;
import it.giannibombelli.wsc2026.payment.domain.events.TransactionAccepted;
import it.giannibombelli.wsc2026.payment.domain.payment.Payment;
import it.giannibombelli.wsc2026.payment.domain.policies.PaymentCompletion;
import it.giannibombelli.wsc2026.payment.domain.ports.PaymentRepository;

import static java.util.Objects.requireNonNull;

public final class TransactionAccepting implements EventSubscriber<TransactionAccepted> {
    private final PaymentRepository paymentRepository;
    private final EventPublisher<PaymentEvent> eventPublisher;
    private final PaymentCompletion paymentCompletion;

    public TransactionAccepting(PaymentRepository paymentRepository,
                                EventPublisher<PaymentEvent> eventPublisher,
                                PaymentCompletion paymentCompletion) {
        this.paymentRepository = requireNonNull(paymentRepository);
        this.eventPublisher = requireNonNull(eventPublisher);
        this.paymentCompletion = requireNonNull(paymentCompletion);
    }

    @Override
    public void on(TransactionAccepted event) {
        Require.requireArgument(event, "event");

        AcceptTransaction cmd = paymentCompletion.evaluate(event);
        Payment payment = paymentRepository.findById(cmd.aggregateId())
            .orElseThrow(() -> new IllegalArgumentException("payment not found"));

        PaymentResultEvents.PaymentAccepted accepted = payment.acceptTransaction(
            cmd.transactionId(),
            cmd.providerCompletedAt()
        );
        paymentRepository.save(payment);

        if (accepted != null) {
            eventPublisher.publish(accepted);
        }
    }
}
