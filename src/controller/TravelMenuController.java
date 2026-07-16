package controller;

import model.quest.QuestDefinition;
import service.quest.QuestService;
import service.minigame.MinigameService;
import model.miniGame.framework.MinigameDefinition;
import model.user.User;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TravelMenuController {
    private static final Pattern PAGE = Pattern.compile("^travel\\s+log\\s+page\\s+(\\S+)$", Pattern.CASE_INSENSITIVE);
    private final QuestService questService;
    private final MinigameService minigameService;

    public TravelMenuController(QuestService questService) { this(questService, null); }

    public TravelMenuController(QuestService questService, MinigameService minigameService) {
        this.questService = questService;
        this.minigameService = minigameService;
    }

    public List<QuestDefinition> execute(String command) {
        Matcher matcher = PAGE.matcher(command == null ? "" : command.trim());
        if (!matcher.matches()) throw new IllegalArgumentException("Expected: travel log page <page_name>");
        return questService.travelLogPage(matcher.group(1));
    }

    public List<MinigameDefinition> executeMinigames(User user, String command) {
        Matcher matcher = PAGE.matcher(command == null ? "" : command.trim());
        if (!matcher.matches() || !"minigame".equalsIgnoreCase(matcher.group(1))) {
            throw new IllegalArgumentException("Expected: travel log page minigame");
        }
        if (minigameService == null) throw new IllegalStateException("Minigame service is not configured.");
        return minigameService.travelLogPage(user);
    }
}
