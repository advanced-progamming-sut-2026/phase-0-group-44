package model.events;

import java.util.ArrayList;
import java.util.List;

/** Small synchronous event bus used to decouple gameplay from quest rules. */
public final class DomainEventBus {
    private final List<DomainEventListener> listeners = new ArrayList<>();

    public void subscribe(DomainEventListener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public void publish(DomainEvent event) {
        if (event == null) {
            return;
        }
        for (DomainEventListener listener : List.copyOf(listeners)) {
            listener.onDomainEvent(event);
        }
    }
}
