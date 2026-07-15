package service;

import model.miniGame.GreenHouse;
import model.sim.SimulationWorld;
import model.user.User;
import util.RandomSource;

import java.util.ArrayList;
import java.util.List;

/**
 * The rewards a zombie death can produce: a plant food from a glowing zombie,
 * and a 10% item drop.
 *
 * <p>Randomness is injected so both rolls are deterministic in tests. No
 * canonical drop-weighting was found in the project's assets, so the three drop
 * items are equally likely, per the instruction not to invent unequal weights.</p>
 */
public class RewardService {

    /** Chance a spawned zombie glows. */
    public static final double GLOW_CHANCE = 0.05;

    /** Chance a death drops an item. */
    public static final double DROP_CHANCE = 0.10;

    /** Coins granted by a coin drop. */
    public static final int COIN_DROP = 50;

    /** The three equally likely drops. */
    public enum Drop {
        DIAMOND,
        COINS,
        POT
    }

    private final UserService userService;
    private final RandomSource random;

    public RewardService(UserService userService, RandomSource random) {
        this.userService = userService;
        this.random = random;
    }

    /** Rolls whether a spawning zombie glows (5%). */
    public boolean rollGlowing() {
        return random.nextDouble() < GLOW_CHANCE;
    }

    /**
     * Resolves a zombie's death: a glowing zombie grants one plant food (capped),
     * and every death has a 10% chance to drop an item. Returns the exact
     * messages, in order, and persists any change to the user.
     */
    public List<String> onZombieDeath(boolean glowing, User user, SimulationWorld world) {
        List<String> messages = new ArrayList<>();

        if (glowing && world.addPlantFood()) {
            messages.add("The glowing zombie dropeed a plant food; you have "
                    + world.getPlantFood() + " plant foods now.");
        }

        if (random.nextDouble() < DROP_CHANCE) {
            messages.add(applyDrop(rollDrop(), user));
            userService.updateUser(user);
        }

        return messages;
    }

    private Drop rollDrop() {
        int index = random.nextInt(Drop.values().length);

        return Drop.values()[index];
    }

    private String applyDrop(Drop drop, User user) {
        switch (drop) {
            case DIAMOND:
                user.setGems(user.getGems() + 1);
                return "A zombie dropeed a diamond; you have " + user.getGems() + " diamonds now.";

            case COINS:
                user.setCoins(user.getCoins() + COIN_DROP);
                return "A zombie dropeed a coin; you have " + user.getCoins() + " coins now.";

            default:
                GreenHouse greenHouse = user.getGreenHouse();
                greenHouse.addSlot();
                return "A zombie dropeed a pot; you have "
                        + greenHouse.getSlotCount() + " pots now.";
        }
    }
}
