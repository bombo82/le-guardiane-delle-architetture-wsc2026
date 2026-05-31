package it.giannibombelli.wsc2026.payment.infrastructure;

import it.giannibombelli.wsc2026.common.utils.Require;

import it.giannibombelli.wsc2026.common.application.events.EventBus;
import it.giannibombelli.wsc2026.common.application.events.EventSubscriber;
import it.giannibombelli.wsc2026.payment.domain.events.PaymentEvent;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;

public final class InMemoryPaymentEventBus implements EventBus<PaymentEvent> {

    private final Map<Class<? extends PaymentEvent>, List<EventSubscriber<? extends PaymentEvent>>> subscribers = new ConcurrentHashMap<>();
    private final Executor executor;

    public InMemoryPaymentEventBus(Executor executor) {
        Require.requireDependency(executor, "executor");
        this.executor = executor;
    }

    @Override
    public void publish(PaymentEvent event) {
        Require.requireArgument(event, "event");

        List<EventSubscriber<? extends PaymentEvent>> matching = subscribers.entrySet().stream()
            .filter(entry -> entry.getKey().isAssignableFrom(event.getClass()))
            .flatMap(entry -> entry.getValue().stream())
            .toList();

        executor.execute(() -> matching.forEach(subscriber -> notify(subscriber, event)));
    }

    @SuppressWarnings("unchecked")
    private <E extends PaymentEvent> void notify(EventSubscriber<E> subscriber, PaymentEvent event) {
        subscriber.on((E) event);
    }

    @Override
    public void subscribe(Class<? extends PaymentEvent> eventType, EventSubscriber<? extends PaymentEvent> subscriber) {
        Require.requireArgument(eventType, "eventType");
        Require.requireArgument(subscriber, "subscriber");
        subscribers.computeIfAbsent(eventType, k -> new CopyOnWriteArrayList<>()).add(subscriber);
    }
}
