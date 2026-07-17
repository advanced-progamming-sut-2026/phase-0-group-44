package service;

import model.Result;
import model.Store;
import model.enums.MenuName;
import model.events.DomainEventType;
import model.miniGame.MiniGameCatalog;
import model.miniGame.MiniGameDefinition;
import model.miniGame.MiniGameId;
import model.miniGame.MiniGameLifecycleState;
import model.miniGame.MiniGameProgress;
import model.miniGame.MiniGameSession;
import model.miniGame.MiniGameSessionFactory;
import model.miniGame.MiniGameStatus;
import model.sim.GameOutcome;
import model.user.User;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Owns mandatory minigame selection, lifecycle, persisted progression and news.
 * Completed-minigame statistics count each fully cleared three-level minigame
 * once, never individual attempts or repeated level wins.
 */
public final class MiniGameService {
    private final MiniGameCatalog catalog;
    private final MiniGameSessionFactory sessions;
    private final UserService users;
    private final NewsService news;
    private final DomainEventPublisher events;

    public MiniGameService(
            MiniGameCatalog catalog,
            MiniGameSessionFactory sessions,
            UserService users,
            NewsService news,
            DomainEventPublisher events
    ) {
        if (catalog == null || sessions == null || users == null || news == null) {
            throw new IllegalArgumentException("Minigame dependencies are required.");
        }
        this.catalog = catalog;
        this.sessions = sessions;
        this.users = users;
        this.news = news;
        this.events = events;
    }

    public MiniGameCatalog getCatalog() {
        return catalog;
    }

    public List<MiniGameStatus> list(User user) {
        if (user == null) {
            return List.of();
        }
        ensureDefaults(user);
        List<MiniGameStatus> result = new ArrayList<>();
        for (MiniGameDefinition definition : catalog.getDefinitions()) {
            result.add(new MiniGameStatus(
                    definition,
                    progressFor(user, definition.getId())
            ));
        }
        return List.copyOf(result);
    }

    public Result<MiniGameSession> select(User user, String token, int level) {
        Result<MiniGameSession> result = new Result<>();
        if (user == null) {
            result.appendToMessage("no user is logged in");
            return result;
        }
        if (user.getUsername() == null || user.getUsername().isBlank()) {
            result.appendToMessage("user is missing a username");
            return result;
        }
        MiniGameSession active = Store.getActiveMiniGameSession();
        if (active != null && active.getState() == MiniGameLifecycleState.RUNNING) {
            result.appendToMessage("finish the running minigame before selecting another");
            return result;
        }
        MiniGameDefinition definition = catalog.find(token);
        if (definition == null) {
            result.appendToMessage("unknown or bonus minigame");
            return result;
        }
        if (level < 1 || level > 3) {
            result.appendToMessage("minigame level must be between 1 and 3");
            return result;
        }
        ensureDefaults(user);
        MiniGameProgress progress = progressFor(user, definition.getId());
        if (!progress.isLevelUnlocked(level)) {
            result.appendToMessage("that minigame level is locked");
            return result;
        }

        MiniGameSession session = sessions.create(
                user.getUsername(), definition, definition.getLevel(level));
        Store.setActiveMiniGameSession(session);
        result.setStatus(true);
        result.setData(session);
        result.appendToMessage("selected " + definition.getDisplayName() + " level " + level);
        return result;
    }

    public Result<MiniGameSession> start(User user) {
        Result<MiniGameSession> result = new Result<>();
        if (user == null) {
            result.appendToMessage("no user is logged in");
            return result;
        }
        MiniGameSession session = Store.getActiveMiniGameSession();
        if (session == null) {
            result.appendToMessage("select a minigame level first");
            return result;
        }
        if (!session.getOwnerUsername().equals(user.getUsername())) {
            result.appendToMessage("the selected minigame belongs to another user");
            return result;
        }
        Result<MiniGameSession> started = session.start();
        if (!started.getStatus()) {
            return started;
        }
        Store.setCurrentMenu(MenuName.MINIGAME);
        return started;
    }

    public Result<List<String>> advance(User user, int ticks) {
        Result<List<String>> result = new Result<>();
        MiniGameSession session = runningSession(user, result);
        if (session == null) {
            return result;
        }
        Result<List<String>> advanced = session.advance(ticks);
        if (advanced.getStatus() && isTerminal(session)) {
            settle(user, session);
        }
        return advanced;
    }

    public Result<String> executeStrategyCommand(User user, String input) {
        Result<String> result = new Result<>();
        MiniGameSession session = runningSession(user, result);
        if (session == null) {
            return result;
        }
        Result<String> executed = session.executeStrategyCommand(input);
        if (executed.getStatus() && isTerminal(session)) {
            settle(user, session);
        }
        return executed;
    }

    public Result<MiniGameSession> finish(User user, GameOutcome outcome) {
        Result<MiniGameSession> result = new Result<>();
        if (outcome != GameOutcome.WON && outcome != GameOutcome.LOST) {
            result.appendToMessage("minigame result must be won or lost");
            return result;
        }
        MiniGameSession session = runningSession(user, result);
        if (session == null) {
            return result;
        }
        if (!session.finish(outcome)) {
            result.appendToMessage("minigame result was already recorded");
            return result;
        }
        settle(user, session);
        result.setStatus(true);
        result.setData(session);
        result.appendToMessage(outcome == GameOutcome.WON
                ? "minigame won" : "minigame lost");
        return result;
    }

    public Result<MiniGameSession> forfeit(User user) {
        return finish(user, GameOutcome.LOST);
    }

    private <T> MiniGameSession runningSession(User user, Result<T> result) {
        if (user == null) {
            result.appendToMessage("no user is logged in");
            return null;
        }
        MiniGameSession session = Store.getActiveMiniGameSession();
        if (session == null || session.getState() != MiniGameLifecycleState.RUNNING) {
            result.appendToMessage("no minigame is currently running");
            return null;
        }
        if (!session.getOwnerUsername().equals(user.getUsername())) {
            result.appendToMessage("the running minigame belongs to another user");
            return null;
        }
        return session;
    }

    private boolean isTerminal(MiniGameSession session) {
        return session.getState() == MiniGameLifecycleState.WON
                || session.getState() == MiniGameLifecycleState.LOST;
    }

    private void settle(User user, MiniGameSession session) {
        if (!session.markSettled()) {
            return;
        }
        boolean won = session.getState() == MiniGameLifecycleState.WON;
        boolean firstLevelCompletion = false;
        if (won) {
            firstLevelCompletion = progressWin(user, session);
        }
        if (events != null) {
            events.publish(DomainEventType.MINIGAME_COMPLETED, user, Map.of(
                    "minigame", session.getDefinition().getId().getToken(),
                    "level", String.valueOf(session.getConfig().getLevelNumber()),
                    "won", String.valueOf(won),
                    "firstCompletion", String.valueOf(firstLevelCompletion)
            ));
        }
        users.updateUser(user);
        Store.setCurrentMenu(MenuName.TRAVEL_LOG);
        Store.setActiveMiniGameSession(null);
    }

    private boolean progressWin(User user, MiniGameSession session) {
        MiniGameDefinition definition = session.getDefinition();
        int level = session.getConfig().getLevelNumber();
        MiniGameProgress progress = progressFor(user, definition.getId());
        boolean firstCompletion = progress.completeLevel(level);
        if (!firstCompletion) {
            return false;
        }
        if (level < 3) {
            progress.unlockLevel(level + 1);
        }
        if (progress.markCompletionCounted()) {
            user.setCompletedMiniGames(user.getCompletedMiniGames() + 1);
            MiniGameId unlocked = definition.getUnlocksAfterCompletion();
            if (unlocked != null) {
                MiniGameProgress next = progressFor(user, unlocked);
                if (next.unlockMiniGame()) {
                    MiniGameDefinition nextDefinition = catalog.find(unlocked);
                    news.miniGameUnlocked(user, nextDefinition.getDisplayName());
                }
            }
        }
        return true;
    }

    private void ensureDefaults(User user) {
        user.applyDefaults();
        progressFor(user, MiniGameId.VASE_BREAKER).unlockMiniGame();
    }

    private MiniGameProgress progressFor(User user, MiniGameId id) {
        MiniGameProgress progress = user.getMiniGameProgress().computeIfAbsent(
                id.getToken(), ignored -> new MiniGameProgress());
        progress.applyDefaults();
        return progress;
    }
}
