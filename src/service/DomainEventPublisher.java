package service;

import model.events.DomainEvent;
import model.events.DomainEventBus;
import model.events.DomainEventType;
import model.user.User;

import java.util.Map;

/** Clock-aware facade used by gameplay code to emit generic domain events. */
public final class DomainEventPublisher {
    private final DomainEventBus bus;
    private final UserService users;

    public DomainEventPublisher(DomainEventBus bus, UserService users) {
        this.bus = bus;
        this.users = users;
    }

    public void publish(DomainEventType type, User user, Map<String, String> attributes) {
        publish(type, user, 1, attributes);
    }

    public void publish(DomainEventType type, User user, int amount,
                        Map<String, String> attributes) {
        if (bus != null && users != null && user != null) {
            bus.publish(DomainEvent.amount(type, user, amount, users.now(), attributes));
        }
    }
}
