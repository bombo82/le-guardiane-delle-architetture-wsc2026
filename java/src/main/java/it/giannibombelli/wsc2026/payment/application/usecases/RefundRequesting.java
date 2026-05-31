package it.giannibombelli.wsc2026.payment.application.usecases;

import it.giannibombelli.wsc2026.common.utils.Require;

import it.giannibombelli.wsc2026.common.application.UseCase;
import it.giannibombelli.wsc2026.common.application.events.EventPublisher;
import it.giannibombelli.wsc2026.payment.application.commands.RefundTransaction;
import it.giannibombelli.wsc2026.payment.domain.events.PaymentEvent;
import it.giannibombelli.wsc2026.payment.domain.events.RefundRequested;
import it.giannibombelli.wsc2026.payment.domain.payment.Payment;
import it.giannibombelli.wsc2026.payment.domain.ports.PaymentRepository;

import static java.util.Objects.requireNonNull;

public final class RefundRequesting implements UseCase<RefundTransaction, RefundRequested> {
    private final PaymentRepository paymentRepository;
    private final EventPublisher<PaymentEvent> eventPublisher;

    public RefundRequesting(PaymentRepository paymentRepository, EventPublisher<PaymentEvent> eventPublisher) {
        this.paymentRepository = requireNonNull(paymentRepository);
        this.eventPublisher = requireNonNull(eventPublisher);
    }

    @Override
    public RefundRequested invoke(RefundTransaction cmd) {
        Require.requireArgument(cmd, "command");

        Payment payment = paymentRepository.findById(cmd.aggregateId())
            .orElseThrow(() -> new IllegalArgumentException("payment not found"));

        RefundRequested event = payment.requestRefund(cmd.amount());
        paymentRepository.save(payment);

        eventPublisher.publish(event);
        return event;
    }
}
