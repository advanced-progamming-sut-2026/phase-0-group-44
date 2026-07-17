package model.events;

public interface DomainEventListener {
    void onDomainEvent(DomainEvent event);
}
