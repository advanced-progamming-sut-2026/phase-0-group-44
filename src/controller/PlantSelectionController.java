package controller;

import model.Result;
import model.Store;
import model.enums.PlantType;
import model.inGame.GameSession;
import model.inGame.PlantSelection;
import model.inGame.plant.PlantDefinition;
import model.inGame.plant.PlantRepository;
import model.level.Level;
import model.level.LevelSelectionRules;
import model.user.Settings;
import model.user.User;
import service.UserService;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * The pre-level plant-selection flow: pick plants within the level's rules,
 * optionally pay to boost one, then start a fresh game session.
 *
 * <p>The controller holds the in-progress selection for the level it was begun
 * with. Diamonds are charged only on success, and stored greenhouse boosts are
 * recorded for consumption on first use inside the session.</p>
 */
public class PlantSelectionController {

    public static final int DIAMOND_BOOST_COST = 2;

    private final PlantRepository plantRepository;
    private final UserService userService;

    private Level level;
    private PlantSelection selection;

    public PlantSelectionController(PlantRepository plantRepository, UserService userService) {
        this.plantRepository = plantRepository;
        this.userService = userService;
    }

    /** Opens the selection screen for a level; called when a level is entered. */
    public Result<String> begin(Level level) {
        Result<String> result = new Result<>();

        if (level == null) {
            result.appendToMessage("no level to select for");
            return result;
        }

        this.level = level;
        this.selection = new PlantSelection();
        Store.setCurrentMenu(model.enums.MenuName.PLANT_SELECTION);

        result.setStatus(true);
        result.setData(level.getName());
        result.appendToMessage("selecting plants for " + level.getName());

        return result;
    }

    /** Handles {@code show all plants}. */
    public Result<List<PlantDefinition>> showAllPlants() {
        Result<List<PlantDefinition>> result = new Result<>();
        List<PlantDefinition> all = new ArrayList<>(plantRepository.findAll());

        result.setStatus(true);
        result.setData(all);

        if (all.isEmpty()) {
            result.appendToMessage("no plants defined");
            return result;
        }

        appendDefinitions(result, all);

        return result;
    }

    /** Handles {@code show available plants}: owned plants this level allows. */
    public Result<List<PlantDefinition>> showAvailablePlants() {
        Result<List<PlantDefinition>> result = new Result<>();

        if (notInSelection(result)) {
            return result;
        }

        User user = Store.getLoggedInUser();
        List<PlantDefinition> available = new ArrayList<>();

        for (PlantDefinition definition : plantRepository.findAll()) {
            if (isSelectable(user, definition.getType())) {
                available.add(definition);
            }
        }

        result.setStatus(true);
        result.setData(available);

        if (available.isEmpty()) {
            result.appendToMessage("no available plants");
            return result;
        }

        appendDefinitions(result, available);

        return result;
    }

    /** Handles {@code add plant -t <type>}. */
    public Result<String> addPlant(String typeToken) {
        Result<String> result = new Result<>();

        if (notInSelection(result)) {
            return result;
        }

        PlantType type = parseType(typeToken);

        if (type == null) {
            result.appendToMessage("no plant of type \"" + typeToken + "\"");
            return result;
        }

        User user = Store.getLoggedInUser();

        if (!user.getCollection().hasPlant(type)) {
            result.appendToMessage("you do not own this plant");
            return result;
        }

        if (!level.getSelectionRules().allows(type)) {
            result.appendToMessage("this level does not allow this plant");
            return result;
        }

        if (selection.contains(type)) {
            result.appendToMessage("this plant is already selected");
            return result;
        }

        if (selection.size() >= level.getSelectionRules().getCapacity()) {
            result.appendToMessage("all selection slots are full");
            return result;
        }

        selection.add(type);

        result.setStatus(true);
        result.setData(type.name());
        result.appendToMessage("added " + type.name());

        return result;
    }

    /** Handles {@code remove plant -t <type>}. */
    public Result<String> removePlant(String typeToken) {
        Result<String> result = new Result<>();

        if (notInSelection(result)) {
            return result;
        }

        PlantType type = parseType(typeToken);

        if (type == null) {
            result.appendToMessage("no plant of type \"" + typeToken + "\"");
            return result;
        }

        if (!selection.contains(type)) {
            result.appendToMessage("this plant is not selected");
            return result;
        }

        selection.remove(type);

        result.setStatus(true);
        result.setData(type.name());
        result.appendToMessage("removed " + type.name());

        return result;
    }

    /** Handles {@code boost plant -t <type>}: 2 diamonds, only charged on success. */
    public Result<String> boostPlant(String typeToken) {
        Result<String> result = new Result<>();

        if (notInSelection(result)) {
            return result;
        }

        PlantType type = parseType(typeToken);

        if (type == null) {
            result.appendToMessage("no plant of type \"" + typeToken + "\"");
            return result;
        }

        if (!selection.contains(type)) {
            result.appendToMessage("select the plant before boosting it");
            return result;
        }

        if (selection.isDiamondBoosted(type)) {
            result.appendToMessage("this plant is already boosted");
            return result;
        }

        User user = Store.getLoggedInUser();

        if (user.getGems() < DIAMOND_BOOST_COST) {
            result.appendToMessage("not enough diamonds");
            return result;
        }

        user.setGems(user.getGems() - DIAMOND_BOOST_COST);
        selection.markDiamondBoosted(type);
        userService.updateUser(user);

        result.setStatus(true);
        result.setData(type.name());
        result.appendToMessage("boosted " + type.name());

        return result;
    }

    /** Handles {@code start game}: validate, snapshot a fresh session, enter gameplay. */
    public Result<GameSession> startGame() {
        Result<GameSession> result = new Result<>();

        if (level == null) {
            result.appendToMessage("plant selection has not been started");
            return result;
        }

        User user = Store.getLoggedInUser();

        if (user == null) {
            result.appendToMessage("no user is logged in");
            return result;
        }

        LevelSelectionRules rules = level.getSelectionRules();

        if (!rules.isSelectionBypassed() && selection.isEmpty()) {
            result.appendToMessage("select at least one plant");
            return result;
        }

        if (selection.size() > rules.getCapacity()) {
            result.appendToMessage("too many plants selected");
            return result;
        }

        for (PlantType type : selection.getChosen()) {
            if (!isSelectable(user, type)) {
                result.appendToMessage("a selected plant is no longer available: " + type.name());
                return result;
            }
        }

        GameSession session = new GameSession(
                level,
                selection,
                difficultyOf(user),
                pendingGreenhouseBoosts(user)
        );

        Store.setActiveSession(session);
        Store.setCurrentMenu(model.enums.MenuName.GAMEPLAY);

        result.setStatus(true);
        result.setData(session);
        result.appendToMessage("game started for " + level.getName());

        return result;
    }

    public PlantSelection getSelection() {
        return selection;
    }

    private Set<PlantType> pendingGreenhouseBoosts(User user) {
        Set<PlantType> pending = new LinkedHashSet<>();

        for (PlantType type : selection.getChosen()) {
            Integer stored = user.getPlantBoosts().get(type);

            if (stored != null && stored > 0) {
                pending.add(type);
            }
        }

        return pending;
    }

    private int difficultyOf(User user) {
        Settings settings = user.getSettings();

        return settings == null ? new Settings().getDifficultyLevel() : settings.getDifficultyLevel();
    }

    private boolean isSelectable(User user, PlantType type) {
        return user.getCollection().hasPlant(type) && level.getSelectionRules().allows(type);
    }

    private PlantType parseType(String token) {
        if (token == null) {
            return null;
        }

        PlantDefinition byName = plantRepository.findByName(token.trim());

        if (byName != null) {
            return byName.getType();
        }

        String normalized = token.trim().toUpperCase().replace("-", "_").replace(" ", "_");

        for (PlantType type : PlantType.values()) {
            if (type.name().equals(normalized)) {
                return type;
            }
        }

        return null;
    }

    private boolean notInSelection(Result<?> result) {
        if (level == null || selection == null) {
            result.appendToMessage("plant selection has not been started");
            return true;
        }

        if (Store.getLoggedInUser() == null) {
            result.appendToMessage("no user is logged in");
            return true;
        }

        return false;
    }

    private void appendDefinitions(
            Result<List<PlantDefinition>> result,
            List<PlantDefinition> definitions
    ) {
        for (int i = 0; i < definitions.size(); i++) {
            result.appendToMessage(definitions.get(i).getDisplayText());

            if (i < definitions.size() - 1) {
                result.appendToMessage("\n\n");
            }
        }
    }
}
