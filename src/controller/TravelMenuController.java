package controller;

import model.Result;
import model.Store;
import model.miniGame.MiniGameProgress;
import model.miniGame.MiniGameSession;
import model.miniGame.MiniGameStatus;
import model.user.User;
import model.utility.QuestInstance;
import model.utility.TravelLogPage;
import service.QuestService;

import java.util.List;

/** Navigation, quest claiming, and mandatory-minigame entry for Travel Log pages. */
public class TravelMenuController {
    private final QuestService questService;
    private final MiniGameController miniGames;
    private TravelLogPage currentPage;

    public TravelMenuController(QuestService questService) {
        this(questService, null);
    }

    public TravelMenuController(
            QuestService questService,
            MiniGameController miniGames
    ) {
        if (questService == null) {
            throw new IllegalArgumentException("Quest service is required.");
        }
        this.questService = questService;
        this.miniGames = miniGames;
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

    /** Selects an unlocked mandatory minigame only from its Travel Log page. */
    public Result<MiniGameSession> selectMiniGame(String name, int level) {
        Result<MiniGameSession> result = new Result<>();
        User user = requireUser(result);
        if (user == null) {
            return result;
        }
        if (currentPage != TravelLogPage.MINIGAME) {
            result.appendToMessage("open the minigame travel log page first");
            return result;
        }
        if (miniGames == null) {
            result.appendToMessage("minigame framework is unavailable");
            return result;
        }
        return miniGames.select(user, name, level);
    }

    /** Starts the selected attempt and enters the isolated minigame runtime. */
    public Result<MiniGameSession> startMiniGame() {
        Result<MiniGameSession> result = new Result<>();
        User user = requireUser(result);
        if (user == null) {
            return result;
        }
        if (currentPage != TravelLogPage.MINIGAME) {
            result.appendToMessage("open the minigame travel log page first");
            return result;
        }
        if (miniGames == null) {
            result.appendToMessage("minigame framework is unavailable");
            return result;
        }
        return miniGames.start(user);
    }

    /** Executes a strategy-owned pre-start command such as Zombotany plant selection. */
    public Result<String> executeMiniGameCommand(String input) {
        Result<String> result = new Result<>();
        User user = requireUser(result);
        if (user == null) {
            return result;
        }
        if (currentPage != TravelLogPage.MINIGAME) {
            result.appendToMessage("open the minigame travel log page first");
            return result;
        }
        if (miniGames == null) {
            result.appendToMessage("minigame framework is unavailable");
            return result;
        }
        return miniGames.executeStrategyCommand(user, input);
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

    public TravelLogPage getCurrentPage() {
        return currentPage;
    }

    private String render(User user, TravelLogPage page) {
        if (page == TravelLogPage.MINIGAME) {
            return renderMiniGames(user);
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

    private String renderMiniGames(User user) {
        StringBuilder text = new StringBuilder("Travel Log / minigame\n")
                .append("completed mini-games: ").append(user.getCompletedMiniGames())
                .append("\nno minigame quest rows exist in the canonical quest table");
        if (miniGames == null) {
            return text.toString();
        }
        for (MiniGameStatus status : miniGames.list(user)) {
            MiniGameProgress progress = status.progress();
            text.append("\n- ").append(status.definition().getDisplayName()).append(": ");
            if (!progress.isUnlocked()) {
                text.append("locked");
                continue;
            }
            for (int level = 1; level <= 3; level++) {
                if (level > 1) {
                    text.append(", ");
                }
                text.append("L").append(level).append("=");
                if (progress.isLevelCompleted(level)) {
                    text.append("completed");
                } else if (progress.isLevelUnlocked(level)) {
                    text.append("available");
                } else {
                    text.append("locked");
                }
            }
        }
        text.append("\nuse: minigame select -n <name> -l <1|2|3>, then minigame start");
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
