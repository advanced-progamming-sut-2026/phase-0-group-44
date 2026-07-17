package controller;

import model.Result;
import model.events.DomainEventBus;
import model.Store;
import model.miniGame.MiniGameCatalog;
import model.miniGame.MiniGameSession;
import model.miniGame.MiniGameSessionFactory;
import model.miniGame.MiniGameStatus;
import model.sim.GameOutcome;
import model.user.User;
import service.DomainEventPublisher;
import service.MiniGameService;
import service.NewsService;
import service.UserService;
import repository.JsonUserRepository;
import util.SeededRandomSource;

import java.time.Clock;
import java.util.List;

/** Thin command-facing adapter over the persisted minigame lifecycle service. */
public class MiniGameController {
    private final MiniGameService service;

    /** Compatibility constructor retained for older controller tests. */
    public MiniGameController() {
        this(standaloneService());
    }

    /** Compatibility constructor retained from the quest framework patch. */
    public MiniGameController(DomainEventPublisher events, UserService users) {
        this(events, users, new NewsService(users));
    }

    public MiniGameController(
            DomainEventPublisher events,
            UserService users,
            NewsService news
    ) {
        this(new MiniGameService(
                MiniGameCatalog.mandatoryDefaults(),
                new MiniGameSessionFactory(new SeededRandomSource()),
                users,
                news,
                events
        ));
    }

    public MiniGameController(MiniGameService service) {
        if (service == null) {
            throw new IllegalArgumentException("Minigame service is required.");
        }
        this.service = service;
    }

    public MiniGameService getService() {
        return service;
    }

    public List<MiniGameStatus> list(User user) {
        return service.list(user);
    }

    public Result<MiniGameSession> select(User user, String name, int level) {
        return service.select(user, name, level);
    }

    public Result<MiniGameSession> start(User user) {
        return service.start(user);
    }

    public Result<List<String>> advance(User user, int ticks) {
        return service.advance(user, ticks);
    }

    public Result<String> executeStrategyCommand(User user, String input) {
        return service.executeStrategyCommand(user, input);
    }

    public Result<MiniGameSession> finish(User user, GameOutcome outcome) {
        return service.finish(user, outcome);
    }

    public Result<MiniGameSession> forfeit(User user) {
        return service.forfeit(user);
    }

    public Result<String> status(User user) {
        Result<String> result = new Result<>();
        if (user == null) {
            result.appendToMessage("no user is logged in");
            return result;
        }
        MiniGameSession active = Store.getActiveMiniGameSession();
        if (active == null) {
            result.appendToMessage("no minigame is selected");
            return result;
        }
        String text = active.getDefinition().getDisplayName()
                + " level " + active.getConfig().getLevelNumber()
                + " — " + active.getState().name().toLowerCase()
                + " — tick " + active.getSimulation().getCurrentTick()
                + "/" + active.getConfig().getTimeLimitTicks();
        result.setStatus(true);
        result.setData(text);
        result.appendToMessage(text);
        return result;
    }

    /**
     * Compatibility adapter for the quest-era stub. A result is accepted only
     * for the currently selected canonical attempt, so arbitrary names cannot
     * inflate persisted statistics.
     */
    public Result<String> complete(User user, String minigameName, boolean won) {
        Result<String> result = new Result<>();
        MiniGameSession active = Store.getActiveMiniGameSession();
        if (active == null || minigameName == null
                || !active.getDefinition().getId().equals(
                        model.miniGame.MiniGameId.fromToken(minigameName))) {
            result.appendToMessage("select and start a canonical minigame level first");
            return result;
        }
        Result<MiniGameSession> settled = finish(
                user, won ? GameOutcome.WON : GameOutcome.LOST);
        result.setStatus(settled.getStatus());
        result.setData(active.getDefinition().getDisplayName());
        result.appendToMessage(settled.getMessage());
        return result;
    }

    private static MiniGameService standaloneService() {
        UserService users = new UserService(
                new JsonUserRepository(), Clock.systemDefaultZone());
        return new MiniGameService(
                MiniGameCatalog.mandatoryDefaults(),
                new MiniGameSessionFactory(new SeededRandomSource()),
                users,
                new NewsService(users),
                new DomainEventPublisher(new DomainEventBus(), users)
        );
    }
}
