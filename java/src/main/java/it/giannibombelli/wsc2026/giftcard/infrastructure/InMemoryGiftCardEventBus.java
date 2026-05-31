package it.giannibombelli.wsc2026.giftcard.infrastructure;

import it.giannibombelli.wsc2026.common.utils.Require;

import it.giannibombelli.wsc2026.common.application.events.EventBus;
import it.giannibombelli.wsc2026.common.application.events.EventSubscriber;
import it.giannibombelli.wsc2026.giftcard.domain.events.GiftCardEvent;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;

public final class InMemoryGiftCardEventBus implements EventBus<GiftCardEvent> {

    private final Map<Class<? extends GiftCardEvent>, List<EventSubscriber<? extends GiftCardEvent>>> subscribers = new ConcurrentHashMap<>();
    private final Executor executor;

    public InMemoryGiftCardEventBus(Executor executor) {
        Require.requireDependency(executor, "executor");
        this.executor = executor;
    }

    @Override
    public void publish(GiftCardEvent event) {
        Require.requireArgument(event, "event");

        List<EventSubscriber<? extends GiftCardEvent>> matching = subscribers.entrySet().stream()
            .filter(entry -> entry.getKey().isAssignableFrom(event.getClass()))
            .flatMap(entry -> entry.getValue().stream())
            .toList();

        executor.execute(() -> matching.forEach(subscriber -> notify(subscriber, event)));
    }

    @SuppressWarnings("unchecked")
    private <E extends GiftCardEvent> void notify(EventSubscriber<E> subscriber, GiftCardEvent event) {
        subscriber.on((E) event);
    }

    @Override
    public void subscribe(Class<? extends GiftCardEvent> eventType, EventSubscriber<? extends GiftCardEvent> subscriber) {
        Require.requireArgument(eventType, "eventType");
        Require.requireArgument(subscriber, "subscriber");

        subscribers.computeIfAbsent(eventType, k -> new CopyOnWriteArrayList<>()).add(subscriber);
    }
}
