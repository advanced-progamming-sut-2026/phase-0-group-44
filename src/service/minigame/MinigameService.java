package service.minigame;

import model.Store;
import model.inGame.GameSession;
import model.miniGame.framework.*;
import model.miniGame.VasebreakerCommandHandler;
import model.miniGame.VasebreakerStrategy;
import model.sim.Simulation;
import model.user.User;
import service.NewsService;
import service.UserService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MinigameService {
    private final UserService userService;
    private final NewsService newsService;
    private final MinigameCatalog catalog;
    private MinigameSession activeSession;

    public MinigameService(UserService userService, NewsService newsService) {
        this(userService, newsService, new MinigameCatalog());
    }

    public MinigameService(UserService userService, NewsService newsService, MinigameCatalog catalog) {
        if (userService == null || newsService == null || catalog == null) {
            throw new IllegalArgumentException("Minigame dependencies are required.");
        }
        this.userService = userService;
        this.newsService = newsService;
        this.catalog = catalog;
    }

    public List<MinigameDefinition> travelLogPage(User user) {
        requireUser(user);
        List<MinigameDefinition> visible = new ArrayList<>();
        for (MinigameDefinition definition : catalog.all()) {
            MinigameProgress progress = progress(user, definition.getId());
            if (progress.isUnlocked()) visible.add(definition);
        }
        return visible;
    }

    public boolean unlock(User user, MinigameId id) {
        requireUser(user);
        MinigameProgress progress = progress(user, id);
        if (progress.isUnlocked()) return false;
        progress.setUnlocked(true);
        newsService.miniGameUnlocked(user, id.getDisplayName());
        userService.updateUser(user);
        return true;
    }

    public MinigameSession select(User user, MinigameId id, int level, MinigameStrategy strategy) {
        requireUser(user);
        if (strategy == null) throw new IllegalArgumentException("Minigame strategy is required.");
        MinigameProgress progress = progress(user, id);
        if (!progress.isUnlocked()) throw new IllegalStateException("Minigame is locked.");
        if (level > progress.getHighestUnlockedLevel()) throw new IllegalStateException("Minigame level is locked.");
        MinigameDefinition definition = catalog.get(id);
        MinigameStrategy selectedStrategy = strategy;
        VasebreakerStrategy vasebreaker = null;
        if (id == MinigameId.VASEBREAKER) {
            vasebreaker = new VasebreakerStrategy(level);
            selectedStrategy = vasebreaker;
        }
        activeSession = new MinigameSession(definition, definition.level(level), selectedStrategy,
                Store.getActiveSession(), Store.getActiveSimulation());
        if (vasebreaker != null) activeSession.addCommandHandler(new VasebreakerCommandHandler(vasebreaker));
        return activeSession;
    }

    public MinigameSession getActiveSession() { return activeSession; }

    public void start() { requireSession().start(); }

    public void tick() { requireSession().tick(); }

    public void win(User user) {
        MinigameSession session = requireSession();
        session.win();
        MinigameProgress progress = progress(user, session.getDefinition().getId());
        boolean firstCompletion = progress.complete(session.getLevelConfig().getLevel());
        if (firstCompletion) user.setCompletedMiniGames(user.getCompletedMiniGames() + 1);
        userService.updateUser(user);
    }

    public void lose(User user) {
        requireUser(user);
        requireSession().lose();
        userService.updateUser(user);
    }

    public String extensionCommand(String command) { return requireSession().executeExtension(command); }

    private MinigameSession requireSession() {
        if (activeSession == null) throw new IllegalStateException("No minigame selected.");
        return activeSession;
    }

    private MinigameProgress progress(User user, MinigameId id) {
        Map<String, MinigameProgress> all = user.getMinigameProgress();
        return all.computeIfAbsent(id.getToken(), ignored -> new MinigameProgress());
    }

    private void requireUser(User user) {
        if (user == null) throw new IllegalArgumentException("User is required.");
    }
}
