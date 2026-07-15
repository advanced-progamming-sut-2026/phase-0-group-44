package service;

import model.config.ChapterCatalog;
import model.config.GameWorld;
import model.inGame.GameSession;
import model.user.User;

import java.util.ArrayList;
import java.util.List;

/**
 * What happens when a game ends: on a win, record completion, unlock the next
 * content, add news, and persist; on a loss, simply end.
 */
public class GameConclusionService {

    private final UserService userService;
    private final NewsService newsService;

    public GameConclusionService(UserService userService, NewsService newsService) {
        this.userService = userService;
        this.newsService = newsService;
    }

    /** Records a win. Returns any follow-up messages (e.g. unlock news). */
    public List<String> onWin(User user, GameSession session) {
        List<String> messages = new ArrayList<>();

        if (user == null) {
            return messages;
        }

        user.setCompletedLevelCount(user.getCompletedLevelCount() + 1);
        user.setGamesPlayed(user.getGamesPlayed() + 1);

        GameWorld current = session != null
                ? GameWorld.fromName(session.getLevel().getName()) : null;

        if (current != null) {
            user.setLatestCompletedChapter(current.getChapterNumber());
            GameWorld unlocked = ChapterCatalog.unlockNext(user, current);

            if (unlocked != null && newsService != null) {
                newsService.levelUnlocked(user, unlocked.getDisplayName());
                messages.add(unlocked.getDisplayName() + " unlocked");
            }
        }

        userService.updateUser(user);

        return messages;
    }

    /** Records a loss. */
    public void onLoss(User user, GameSession session) {
        if (user == null) {
            return;
        }

        user.setGamesPlayed(user.getGamesPlayed() + 1);
        userService.updateUser(user);
    }
}
