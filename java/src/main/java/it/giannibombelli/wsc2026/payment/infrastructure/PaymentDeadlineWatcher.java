package it.giannibombelli.wsc2026.payment.infrastructure;

import it.giannibombelli.wsc2026.common.application.events.EventPublisher;
import it.giannibombelli.wsc2026.common.domain.primitive.Timestamp;
import it.giannibombelli.wsc2026.payment.domain.events.PaymentDeadlineReached;
import it.giannibombelli.wsc2026.payment.domain.events.PaymentEvent;
import it.giannibombelli.wsc2026.payment.domain.payment.Payment;
import it.giannibombelli.wsc2026.payment.domain.policies.PaymentExpiration;
import it.giannibombelli.wsc2026.payment.domain.ports.PaymentRepository;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static java.util.Objects.requireNonNull;

public final class PaymentDeadlineWatcher {
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private final PaymentRepository repository;
    private final PaymentExpiration policy;
    private final EventPublisher<PaymentEvent> eventPublisher;

    public PaymentDeadlineWatcher(PaymentRepository repository, PaymentExpiration policy, EventPublisher<PaymentEvent> eventPublisher) {
        this.repository = requireNonNull(repository);
        this.policy = requireNonNull(policy);
        this.eventPublisher = requireNonNull(eventPublisher);
    }

    public void start() {
        scheduler.scheduleAtFixedRate(this::checkDeadlines, 0, 5, TimeUnit.MINUTES);
    }

    private void checkDeadlines() {
        Timestamp now = Timestamp.now();
        Timestamp threshold = now.minusSeconds(48 * 3600);
        List<Payment> candidates = repository.findAllRequestedAndProcessingBefore(threshold);
        for (Payment payment : candidates) {
            if (policy.isDeadlineReached(payment, now)) {
                eventPublisher.publish(new PaymentDeadlineReached(payment.id()));
            }
        }
    }

    public void stop() {
        scheduler.shutdown();
    }
}
