package controller;

import model.quest.QuestDefinition;
import service.quest.QuestService;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TravelMenuController {
    private static final Pattern PAGE = Pattern.compile("^travel\\s+log\\s+page\\s+(\\S+)$", Pattern.CASE_INSENSITIVE);
    private final QuestService questService;

    public TravelMenuController(QuestService questService) { this.questService = questService; }

    public List<QuestDefinition> execute(String command) {
        Matcher matcher = PAGE.matcher(command == null ? "" : command.trim());
        if (!matcher.matches()) throw new IllegalArgumentException("Expected: travel log page <page_name>");
        return questService.travelLogPage(matcher.group(1));
    }
}
