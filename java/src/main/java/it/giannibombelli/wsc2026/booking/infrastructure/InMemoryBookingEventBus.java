package it.giannibombelli.wsc2026.booking.infrastructure;

import it.giannibombelli.wsc2026.common.utils.Require;

import it.giannibombelli.wsc2026.booking.domain.events.BookingEvent;
import it.giannibombelli.wsc2026.common.application.events.EventBus;
import it.giannibombelli.wsc2026.common.application.events.EventSubscriber;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;

public final class InMemoryBookingEventBus implements EventBus<BookingEvent> {

    private final Map<Class<? extends BookingEvent>, List<EventSubscriber<? extends BookingEvent>>> subscribers = new ConcurrentHashMap<>();
    private final Executor executor;

    public InMemoryBookingEventBus(Executor executor) {
        Require.requireDependency(executor, "executor");
        this.executor = executor;
    }

    @Override
    public void publish(BookingEvent event) {
        Require.requireArgument(event, "event");

        List<EventSubscriber<? extends BookingEvent>> matching = subscribers.entrySet().stream()
            .filter(entry -> entry.getKey().isAssignableFrom(event.getClass()))
            .flatMap(entry -> entry.getValue().stream())
            .toList();

        executor.execute(() -> matching.forEach(subscriber -> notify(subscriber, event)));
    }

    @SuppressWarnings("unchecked")
    private <E extends BookingEvent> void notify(EventSubscriber<E> subscriber, BookingEvent event) {
        subscriber.on((E) event);
    }

    @Override
    public void subscribe(Class<? extends BookingEvent> eventType, EventSubscriber<? extends BookingEvent> subscriber) {
        Require.requireArgument(eventType, "eventType");
        Require.requireArgument(subscriber, "subscriber");
        subscribers.computeIfAbsent(eventType, k -> new CopyOnWriteArrayList<>()).add(subscriber);
    }
}
