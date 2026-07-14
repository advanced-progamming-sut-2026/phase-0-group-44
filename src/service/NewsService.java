package service;

import model.News;
import model.enums.NewsType;
import model.user.User;

/**
 * Creates the news entries the player sees when new content opens up.
 *
 * <p>Each entry starts unread; read state is flipped by the news menu and
 * persisted by {@link UserService}. This service is the one place that knows how
 * an unlock becomes a news item, so gameplay code only has to call it.</p>
 */
public class NewsService {

    private final UserService userService;

    public NewsService(UserService userService) {
        this.userService = userService;
    }

    public News plantUnlocked(User user, String plantName) {
        return add(user, new News(
                "New plant unlocked",
                "You unlocked the plant " + plantName + ".",
                NewsType.PLANT_UNLOCKED
        ));
    }

    /** Created only once a zombie has actually been encountered. */
    public News zombieEncountered(User user, String zombieName) {
        return add(user, new News(
                "New zombie discovered",
                "You encountered the zombie " + zombieName + ".",
                NewsType.ZOMBIE_UNLOCKED
        ));
    }

    public News levelUnlocked(User user, String levelName) {
        return add(user, new News(
                "New level unlocked",
                "You unlocked " + levelName + ".",
                NewsType.LEVEL_UNLOCKED
        ));
    }

    public News miniGameUnlocked(User user, String miniGameName) {
        return add(user, new News(
                "New mini-game unlocked",
                "You unlocked the mini-game " + miniGameName + ".",
                NewsType.MINIGAME_UNLOCKED
        ));
    }

    private News add(User user, News news) {
        user.getNewsList().add(news);
        userService.updateUser(user);

        return news;
    }
}
