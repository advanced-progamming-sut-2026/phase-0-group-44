package service;

import model.config.ChapterCatalog;
import model.config.GameWorld;
import model.enums.PlantType;
import model.inGame.GameSession;
import model.level.Level;
import model.user.User;
import util.RandomSource;
import util.SeededRandomSource;

import java.util.ArrayList;
import java.util.List;

/** Applies persistent statistics, progression and news when a game ends. */
public class GameConclusionService {

    private final UserService userService;
    private final NewsService newsService;
    private final RandomSource random;

    public GameConclusionService(UserService userService, NewsService newsService) {
        this(userService, newsService, new SeededRandomSource());
    }

    public GameConclusionService(
            UserService userService,
            NewsService newsService,
            RandomSource random
    ) {
        this.userService = userService;
        this.newsService = newsService;
        this.random = random;
    }

    public List<String> onWin(User user, GameSession session) {
        List<String> messages = new ArrayList<>();
        if (user == null) {
            return messages;
        }

        user.setCompletedLevelCount(user.getCompletedLevelCount() + 1);
        user.setGamesPlayed(user.getGamesPlayed() + 1);
        Level level = session == null ? null : session.getLevel();

        if (level != null && level.getWorld() != null) {
            user.setLatestCompletedChapter(level.getWorld().getChapterNumber());
            user.setLatestCompletedLevel(level.getLevelNumber());
            ChapterCatalog.CompletionResult progression =
                    ChapterCatalog.completeLevel(user, level);
            for (String unlocked : progression.getUnlockedLabels()) {
                if (newsService != null) {
                    newsService.levelUnlocked(user, unlocked);
                }
                messages.add(unlocked + " unlocked");
            }
        } else if (level != null) {
            // Compatibility for older callers that used a chapter name as the level name.
            GameWorld current = GameWorld.fromName(level.getName());
            if (current != null) {
                user.setLatestCompletedChapter(current.getChapterNumber());
                GameWorld unlocked = ChapterCatalog.unlockNext(user, current);
                if (unlocked != null) {
                    if (newsService != null) {
                        newsService.levelUnlocked(user, unlocked.getDisplayName());
                    }
                    messages.add(unlocked.getDisplayName() + " unlocked");
                }
            }
        }

        PlantType plantReward = grantRandomPlantReward(user);
        if (plantReward != null) {
            String label = plantReward.name();
            if (newsService != null) {
                newsService.plantUnlocked(user, label);
            }
            messages.add(label + " unlocked as a reward for winning");
        }

        userService.updateUser(user);
        return messages;
    }

    /**
     * Rewards a random plant the player does not already own. Returns null
     * once the player owns every plant in the game.
     */
    private PlantType grantRandomPlantReward(User user) {
        List<PlantType> unowned = new ArrayList<>();
        for (PlantType type : PlantType.values()) {
            if (!user.getCollection().hasPlant(type)) {
                unowned.add(type);
            }
        }
        if (unowned.isEmpty()) {
            return null;
        }

        PlantType reward = unowned.get(random.nextInt(unowned.size()));
        user.getCollection().purchasePlant(reward);
        return reward;
    }

    public void onLoss(User user, GameSession session) {
        if (user == null) {
            return;
        }
        user.setGamesPlayed(user.getGamesPlayed() + 1);
        userService.updateUser(user);
    }
}