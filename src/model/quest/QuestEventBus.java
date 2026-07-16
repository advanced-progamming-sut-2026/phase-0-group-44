package model.quest;

import java.util.ArrayList;
import java.util.List;

public class QuestEventBus {
    private final List<QuestEventListener> listeners = new ArrayList<>();
    public void subscribe(QuestEventListener listener) { if (listener != null) listeners.add(listener); }
    public void publish(QuestEvent event) { for (QuestEventListener listener : List.copyOf(listeners)) listener.onQuestEvent(event); }
}
