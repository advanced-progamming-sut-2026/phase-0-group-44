package model.events;

import model.user.User;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Generic gameplay event; producers do not depend on the quest subsystem. */
public final class DomainEvent {
    private final DomainEventType type;
    private final User user;
    private final int amount;
    private final LocalDateTime occurredAt;
    private final Map<String, String> attributes;

    public DomainEvent(DomainEventType type, User user, int amount,
                       LocalDateTime occurredAt, Map<String, String> attributes) {
        if (type == null || user == null || occurredAt == null) {
            throw new IllegalArgumentException("Event type, user and time are required.");
        }
        this.type = type;
        this.user = user;
        this.amount = amount;
        this.occurredAt = occurredAt;
        this.attributes = attributes == null
                ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(attributes));
    }

    public static DomainEvent of(DomainEventType type, User user,
                                 LocalDateTime time, Map<String, String> attributes) {
        return new DomainEvent(type, user, 1, time, attributes);
    }

    public static DomainEvent amount(DomainEventType type, User user, int amount,
                                     LocalDateTime time, Map<String, String> attributes) {
        return new DomainEvent(type, user, amount, time, attributes);
    }

    public DomainEventType getType() {
        return type;
    }

    public User getUser() {
        return user;
    }

    public int getAmount() {
        return amount;
    }

    public LocalDateTime getOccurredAt() {
        return occurredAt;
    }

    public Map<String, String> getAttributes() {
        return attributes;
    }

    public String attribute(String key) {
        return attributes.get(key);
    }

    public boolean booleanAttribute(String key) {
        return Boolean.parseBoolean(attributes.getOrDefault(key, "false"));
    }

    public int intAttribute(String key, int fallback) {
        try {
            return Integer.parseInt(attributes.getOrDefault(key, String.valueOf(fallback)));
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }
}
