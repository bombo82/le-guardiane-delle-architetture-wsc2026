package it.giannibombelli.wsc2026.testsupport.events;

import it.giannibombelli.wsc2026.common.application.events.EventSubscriber;

import java.util.ArrayList;
import java.util.List;

public final class CapturingSubscriber<T> implements EventSubscriber<T> {
    private final List<T> events = new ArrayList<>();

    @Override
    public void on(T event) {
        events.add(event);
    }

    public List<T> events() {
        return List.copyOf(events);
    }
}
