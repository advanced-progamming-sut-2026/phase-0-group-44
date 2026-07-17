package controller;

import model.Result;
import model.events.DomainEventType;
import model.user.User;
import service.DomainEventPublisher;
import service.UserService;

import java.util.Map;

/** Publishes minigame completion without coupling minigames to quest definitions. */
public class MiniGameController {
    private final DomainEventPublisher events;
    private final UserService users;

    public MiniGameController() {
        this(null, null);
    }

    public MiniGameController(DomainEventPublisher events, UserService users) {
        this.events = events;
        this.users = users;
    }

    public Result<String> complete(User user, String minigameName, boolean won) {
        Result<String> result = new Result<>();
        if (user == null || minigameName == null || minigameName.isBlank()) {
            result.appendToMessage("minigame completion is missing a user or name");
            return result;
        }
        if (won) {
            user.setCompletedMiniGames(user.getCompletedMiniGames() + 1);
        }
        if (events != null) {
            events.publish(DomainEventType.MINIGAME_COMPLETED, user, Map.of(
                    "minigame", minigameName.trim(),
                    "won", String.valueOf(won)
            ));
        }
        if (users != null) {
            users.updateUser(user);
        }
        result.setStatus(true);
        result.setData(minigameName.trim());
        result.appendToMessage("minigame completion recorded");
        return result;
    }
}
