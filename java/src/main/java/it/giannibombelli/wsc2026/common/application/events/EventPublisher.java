package it.giannibombelli.wsc2026.common.application.events;

public interface EventPublisher<E> {
    void publish(E event);
}
