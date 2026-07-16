package model.quest;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class QuestEvent {
    private final QuestEventType type;
    private final long amount;
    private final Map<String, String> attributes;

    public QuestEvent(QuestEventType type, long amount, Map<String, String> attributes) {
        this.type = type; this.amount = amount;
        this.attributes = attributes == null ? Collections.emptyMap() : Collections.unmodifiableMap(new LinkedHashMap<>(attributes));
    }
    public QuestEventType getType() { return type; }
    public long getAmount() { return amount; }
    public String get(String key) { return attributes.get(key); }
    public Map<String, String> getAttributes() { return attributes; }
    public static QuestEvent of(QuestEventType type, long amount) { return new QuestEvent(type, amount, null); }
}
