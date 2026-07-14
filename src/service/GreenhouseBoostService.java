package service;

import model.enums.PlantType;
import model.inGame.GameSession;
import model.user.User;

/**
 * Ties a plant's first use in a session to its stored greenhouse boost.
 *
 * <p>Gameplay calls {@link #consumeOnFirstUse} when a plant is planted. The
 * stored boost is spent from the user (and persisted) only on that first use,
 * and at most once per session.</p>
 */
public class GreenhouseBoostService {

    private final UserService userService;

    public GreenhouseBoostService(UserService userService) {
        this.userService = userService;
    }

    /**
     * @return true if this first use consumed a stored greenhouse boost, meaning
     *         the plant should be planted already boosted
     */
    public boolean consumeOnFirstUse(GameSession session, User user, PlantType type) {
        if (!session.useAndConsumeGreenhouseBoost(type)) {
            return false;
        }

        user.consumeStoredPlantBoost(type);
        userService.updateUser(user);

        return true;
    }
}
