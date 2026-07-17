package controller;

import model.Result;
import model.Store;
import model.user.User;
import model.utility.QuestInstance;
import model.utility.TravelLogPage;
import service.QuestService;

import java.util.List;

/** Navigation and reward claiming for Travel Log pages. */
public class TravelMenuController {
    private final QuestService questService;
    private TravelLogPage currentPage;

    public TravelMenuController(QuestService questService) {
        if (questService == null) {
            throw new IllegalArgumentException("Quest service is required.");
        }
        this.questService = questService;
    }

    /** Handles {@code travel log page <page_name>}. */
    public Result<String> page(String pageName) {
        Result<String> result = new Result<>();
        User user = requireUser(result);
        if (user == null) {
            return result;
        }
        TravelLogPage page = TravelLogPage.fromToken(pageName);
        if (page == null) {
            result.appendToMessage("unknown travel log page");
            return result;
        }
        currentPage = page;
        String rendered = render(user, page);
        result.setStatus(true);
        result.setData(rendered);
        result.appendToMessage(rendered);
        return result;
    }

    /** Claims by the stable one-based position shown on the selected page. */
    public Result<String> claim(int position) {
        Result<String> result = new Result<>();
        User user = requireUser(result);
        if (user == null) {
            return result;
        }
        if (currentPage == null || currentPage == TravelLogPage.MINIGAME) {
            result.appendToMessage("open a quest page before claiming");
            return result;
        }
        List<QuestInstance> quests = questService.getActiveQuests(user, currentPage.getCategory());
        if (position < 1 || position > quests.size()) {
            result.appendToMessage("quest number is not on the current page");
            return result;
        }
        return questService.claim(user, quests.get(position - 1));
    }

    private String render(User user, TravelLogPage page) {
        if (page == TravelLogPage.MINIGAME) {
            return "Travel Log / minigame\n"
                    + "completed mini-games: " + user.getCompletedMiniGames() + "\n"
                    + "no minigame quest rows exist in the canonical quest table";
        }
        List<QuestInstance> quests = questService.getActiveQuests(user, page.getCategory());
        StringBuilder text = new StringBuilder("Travel Log / ")
                .append(page.getToken());
        if (quests.isEmpty()) {
            return text.append("\nno active quests").toString();
        }
        for (int index = 0; index < quests.size(); index++) {
            QuestInstance instance = quests.get(index);
            text.append('\n').append(index + 1).append(". [")
                    .append(instance.getQuest().getPriority().name().toLowerCase())
                    .append("] ").append(instance.getDisplayName())
                    .append(" — ").append(instance.getQuest().getConditionText())
                    .append(" — reward: ")
                    .append(instance.getQuest().getReward().getCanonicalText())
                    .append(" — ").append(status(instance));
        }
        return text.toString();
    }

    private String status(QuestInstance instance) {
        if (instance.getProgress().isClaimed()) {
            return "claimed";
        }
        if (instance.getProgress().isCompleted()) {
            return "completed, unclaimed";
        }
        return "progress " + instance.getProgress().getProgress();
    }

    private <T> User requireUser(Result<T> result) {
        User user = Store.getLoggedInUser();
        if (user == null) {
            result.appendToMessage("no user is logged in");
        }
        return user;
    }
}
