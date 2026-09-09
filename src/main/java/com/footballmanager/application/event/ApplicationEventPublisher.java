package com.footballmanager.application.event;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public final class ApplicationEventPublisher {
    private final List<Consumer<Object>> listeners = new CopyOnWriteArrayList<>();

    public void subscribe(Consumer<Object> listener) {
        listeners.add(listener);
    }

    public void unsubscribe(Consumer<Object> listener) {
        listeners.remove(listener);
    }

    public void publish(Object event) {
        for (Consumer<Object> listener : listeners) {
            listener.accept(event);
        }
    }
}
