package it.giannibombelli.wsc2026.common.application.events;

public interface EventSubscriber<E> {
    void on(E event);
}
