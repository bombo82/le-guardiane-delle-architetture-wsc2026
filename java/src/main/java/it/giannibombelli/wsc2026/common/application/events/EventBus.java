package it.giannibombelli.wsc2026.common.application.events;

public interface EventBus<E> extends EventPublisher<E> {
    void subscribe(Class<? extends E> eventType, EventSubscriber<? extends E> subscriber);
}
