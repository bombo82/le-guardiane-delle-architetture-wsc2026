package it.giannibombelli.wsc2026.payment.application.services;

import it.giannibombelli.wsc2026.common.utils.Require;

import it.giannibombelli.wsc2026.common.application.UseCase;
import it.giannibombelli.wsc2026.common.application.events.EventPublisher;
import it.giannibombelli.wsc2026.common.domain.identity.EntityId;
import it.giannibombelli.wsc2026.payment.application.commands.StartTransaction;
import it.giannibombelli.wsc2026.payment.domain.events.PaymentEvent;
import it.giannibombelli.wsc2026.payment.domain.events.TransactionAccepted;
import it.giannibombelli.wsc2026.payment.domain.events.TransactionRejected;
import it.giannibombelli.wsc2026.payment.domain.events.TransactionStarted;
import it.giannibombelli.wsc2026.payment.domain.payment.Payment;
import it.giannibombelli.wsc2026.payment.domain.payment.Provider;
import it.giannibombelli.wsc2026.payment.domain.payment.TransactionId;
import it.giannibombelli.wsc2026.payment.domain.policies.PaymentCharging;
import it.giannibombelli.wsc2026.payment.domain.ports.PaymentProvider;
import it.giannibombelli.wsc2026.payment.domain.ports.PaymentProviderResult;
import it.giannibombelli.wsc2026.payment.domain.ports.PaymentRepository;

import java.util.Map;

import static java.util.Objects.requireNonNull;

public final class PaymentProcessing implements UseCase<StartTransaction, TransactionStarted> {
    private final PaymentRepository paymentRepository;
    private final Map<String, PaymentProvider> providers;
    private final EventPublisher<PaymentEvent> eventPublisher;

    public PaymentProcessing(PaymentRepository paymentRepository,
                             Map<String, PaymentProvider> providers,
                             EventPublisher<PaymentEvent> eventPublisher) {
        this.paymentRepository = requireNonNull(paymentRepository);
        this.providers = requireNonNull(providers);
        this.eventPublisher = requireNonNull(eventPublisher);
    }

    @Override
    public TransactionStarted invoke(StartTransaction cmd) {
        Require.requireArgument(cmd, "command");

        Payment payment = paymentRepository.findById(cmd.aggregateId())
            .orElseThrow(() -> new IllegalArgumentException("payment not found"));

        PaymentProvider paymentProvider = providers.get(cmd.provider().label());
        if (paymentProvider == null) {
            throw new IllegalArgumentException("unknown provider: " + cmd.provider().label());
        }

        TransactionId transactionId = EntityId.generate(TransactionId::new);
        TransactionStarted started = payment.startTransaction(
            transactionId,
            cmd.provider(),
            cmd.providerReference(),
            cmd.amount(),
            cmd.startedAt()
        );
        paymentRepository.save(payment);
        eventPublisher.publish(started);

        PaymentCharging charging = new PaymentCharging(paymentProvider);
        PaymentProviderResult result = charging.charge(started);

        switch (result) {
            case PaymentProviderResult.Success s -> eventPublisher.publish(new TransactionAccepted(
                payment.id(), cmd.provider(), started.transactionId(), cmd.amount(), s.providerCompletedAt()));
            case PaymentProviderResult.Failure f -> eventPublisher.publish(new TransactionRejected(
                payment.id(), cmd.provider(), started.transactionId(), f.reason()));
        }

        return started;
    }
}
