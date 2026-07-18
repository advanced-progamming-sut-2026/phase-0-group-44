package controller;

import model.Result;
import model.Store;
import model.enums.PlantType;
import model.inGame.plant.PlantDefinition;
import model.inGame.plant.PlantRepository;
import model.miniGame.GreenHouse;
import model.miniGame.GreenhouseSlot;
import model.user.User;
import service.UserService;
import util.RandomSource;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Handles the persistent greenhouse commands. */
public class GreenhouseController {
    public static final int MARIGOLD_REWARD_COINS = 500;

    private final PlantRepository plantRepository;
    private final UserService userService;
    private final RandomSource random;

    public GreenhouseController(
            PlantRepository plantRepository,
            UserService userService,
            RandomSource random
    ) {
        if (plantRepository == null || userService == null || random == null) {
            throw new IllegalArgumentException(
                    "Plant repository, user service, and random source are required."
            );
        }
        this.plantRepository = plantRepository;
        this.userService = userService;
        this.random = random;
    }

    /** Handles {@code show greenhouse}. */
    public Result<String> showGreenhouse() {
        Result<String> result = new Result<>();
        User user = requireUser(result);
        if (user == null) {
            return result;
        }

        Instant now = userService.getClock().instant();
        StringBuilder output = new StringBuilder();
        GreenHouse greenHouse = user.getGreenHouse();
        greenHouse.applyDefaults();

        for (GreenhouseSlot slot : greenHouse.getSlots()) {
            if (output.length() > 0) {
                output.append('\n');
            }
            output.append(formatSlot(slot, now));
        }

        result.setStatus(true);
        result.setData(output.toString());
        result.appendToMessage(output.toString());
        return result;
    }

    /** Handles {@code plant pot at (<x>, <y>)}. */
    public Result<String> plantPot(int x, int y) {
        Result<String> result = new Result<>();
        User user = requireUser(result);
        if (user == null) {
            return result;
        }

        GreenhouseSlot slot = validatePlantingSlot(user.getGreenHouse(), x, y, result);
        if (slot == null) {
            return result;
        }

        Instant now = userService.getClock().instant();
        String plantedName;
        if (random.nextDouble() < 0.5) {
            slot.plantMarigold(now.getEpochSecond());
            plantedName = "Marigold";
        } else {
            List<PlantDefinition> eligible = eligibleUnlockedPlants(user);
            if (eligible.isEmpty()) {
                result.appendToMessage(
                        "no unlocked plant with a plant-food effect is available"
                );
                return result;
            }
            PlantDefinition selected = eligible.get(random.nextInt(eligible.size()));
            slot.plant(selected.getType(), now.getEpochSecond());
            plantedName = selected.getName();
        }

        userService.updateUser(user);
        result.setStatus(true);
        result.setData(plantedName);
        result.appendToMessage(
                "planted " + plantedName + " at (" + x + ", " + y + ")"
        );
        return result;
    }

    /** Handles {@code collect (<x>, <y>)}. */
    public Result<String> collect(int x, int y) {
        Result<String> result = new Result<>();
        User user = requireUser(result);
        if (user == null) {
            return result;
        }

        GreenhouseSlot slot = validateOccupiedSlot(user.getGreenHouse(), x, y, result);
        if (slot == null) {
            return result;
        }

        Instant now = userService.getClock().instant();
        if (!slot.isReady(now)) {
            result.appendToMessage("the plant is not ready yet");
            return result;
        }

        String message;
        if (slot.isMarigold()) {
            user.setCoins(user.getCoins() + MARIGOLD_REWARD_COINS);
            message = "collected Marigold; coins: " + user.getCoins();
        } else {
            PlantType type = slot.getPlantType();
            boolean added = user.addStoredPlantBoost(type);
            String name = displayName(type);
            message = added
                    ? "collected " + name + "; stored one boost"
                    : "collected " + name + "; boost already stored";
        }

        slot.clear();
        userService.updateUser(user);
        result.setStatus(true);
        result.setData(message);
        result.appendToMessage(message);
        return result;
    }

    /** Handles {@code grow (<x>, <y>)}. */
    public Result<Integer> grow(int x, int y) {
        Result<Integer> result = new Result<>();
        User user = requireUser(result);
        if (user == null) {
            return result;
        }

        GreenhouseSlot slot = validateOccupiedSlot(user.getGreenHouse(), x, y, result);
        if (slot == null) {
            return result;
        }

        Instant now = userService.getClock().instant();
        long remainingSeconds = slot.remainingSeconds(now);
        if (remainingSeconds == 0L) {
            result.appendToMessage("the plant is already ready");
            return result;
        }

        int cost = (int) ((remainingSeconds + 3599L) / 3600L);
        if (user.getGems() < cost) {
            result.appendToMessage("not enough diamonds");
            return result;
        }

        user.setGems(user.getGems() - cost);
        slot.makeReady(now);
        userService.updateUser(user);

        result.setStatus(true);
        result.setData(cost);
        result.appendToMessage(
                "growth completed for " + cost + " diamond"
                        + (cost == 1 ? "" : "s")
                        + "; diamonds: " + user.getGems()
        );
        return result;
    }

    private GreenhouseSlot validatePlantingSlot(
            GreenHouse greenHouse,
            int x,
            int y,
            Result<?> result
    ) {
        greenHouse.applyDefaults();
        if (!greenHouse.isValidCoordinate(x, y)) {
            result.appendToMessage("greenhouse coordinates are out of range");
            return null;
        }
        if (!greenHouse.hasAvailablePot()) {
            result.appendToMessage("no available greenhouse pot");
            return null;
        }

        GreenhouseSlot slot = greenHouse.getSlot(x, y);
        if (slot.isLocked()) {
            result.appendToMessage("greenhouse slot is locked");
            return null;
        }
        if (slot.isOccupied()) {
            result.appendToMessage("greenhouse slot is occupied");
            return null;
        }
        return slot;
    }

    private GreenhouseSlot validateOccupiedSlot(
            GreenHouse greenHouse,
            int x,
            int y,
            Result<?> result
    ) {
        greenHouse.applyDefaults();
        if (!greenHouse.isValidCoordinate(x, y)) {
            result.appendToMessage("greenhouse coordinates are out of range");
            return null;
        }
        GreenhouseSlot slot = greenHouse.getSlot(x, y);
        if (slot.isLocked()) {
            result.appendToMessage("greenhouse slot is locked");
            return null;
        }
        if (!slot.isOccupied()) {
            result.appendToMessage("greenhouse slot is empty");
            return null;
        }
        return slot;
    }

    private List<PlantDefinition> eligibleUnlockedPlants(User user) {
        List<PlantDefinition> eligible = new ArrayList<>();
        for (PlantDefinition definition : plantRepository.findAll()) {
            if (user.getCollection().hasPlant(definition.getType())
                    && hasPlantFoodEffect(definition)) {
                eligible.add(definition);
            }
        }
        return eligible;
    }

    private boolean hasPlantFoodEffect(PlantDefinition definition) {
        String effect = definition.getPlantFoodEffect();
        if (effect == null || effect.isBlank()) {
            return false;
        }
        String normalized = effect.trim().toLowerCase(Locale.ROOT);
        return !normalized.equals("none")
                && !normalized.equals("-")
                && !normalized.contains("ندارد");
    }

    private String formatSlot(GreenhouseSlot slot, Instant now) {
        String prefix = "(" + slot.getX() + ", " + slot.getY() + "): ";
        if (slot.isLocked()) {
            return prefix + "locked";
        }
        if (!slot.isOccupied()) {
            return prefix + "empty";
        }

        String crop = slot.isMarigold()
                ? "Marigold" : displayName(slot.getPlantType());
        if (slot.isReady(now)) {
            return prefix + crop + " ready";
        }
        return prefix + crop + " growing; remaining "
                + formatDuration(slot.remainingSeconds(now));
    }

    private String formatDuration(long seconds) {
        long hours = seconds / 3600L;
        long minutes = (seconds % 3600L) / 60L;
        long remainingSeconds = seconds % 60L;
        return String.format("%02d:%02d:%02d", hours, minutes, remainingSeconds);
    }

    private String displayName(PlantType type) {
        PlantDefinition definition = plantRepository.findByType(type);
        return definition == null ? type.name() : definition.getName();
    }

    private <T> User requireUser(Result<T> result) {
        User user = Store.getLoggedInUser();
        if (user == null) {
            result.appendToMessage("no user is logged in");
        }
        return user;
    }
}
