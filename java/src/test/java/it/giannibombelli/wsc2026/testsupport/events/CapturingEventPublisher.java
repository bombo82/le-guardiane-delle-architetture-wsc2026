package it.giannibombelli.wsc2026.testsupport.events;

import it.giannibombelli.wsc2026.common.application.events.EventPublisher;

import java.util.ArrayList;
import java.util.List;

public final class CapturingEventPublisher<T> implements EventPublisher<T> {
    private final List<T> events = new ArrayList<>();

    @Override
    public void publish(T event) {
        if (event == null) {
            throw new IllegalArgumentException("event must not be null");
        }
        events.add(event);
    }

    public List<T> events() {
        return List.copyOf(events);
    }
}
