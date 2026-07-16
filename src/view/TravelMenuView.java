package view;

import model.quest.QuestDefinition;
import java.util.List;

public class TravelMenuView {
    public String render(List<QuestDefinition> quests) {
        if (quests == null || quests.isEmpty()) return "No active quests.";
        StringBuilder out = new StringBuilder();
        for (QuestDefinition quest : quests) {
            if (!out.isEmpty()) out.append(System.lineSeparator());
            out.append(quest.getCanonicalName()).append(" | ")
                    .append(quest.getPriority()).append(" | ")
                    .append(quest.getConditionText()).append(" | ")
                    .append(quest.getRewardText());
        }
        return out.toString();
    }
}
