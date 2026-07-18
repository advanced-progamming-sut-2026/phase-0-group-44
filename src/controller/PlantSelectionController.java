package controller;

import model.Result;
import model.events.DomainEventType;
import model.Store;
import model.enums.PlantType;
import model.inGame.GameSession;
import model.inGame.PlantSelection;
import model.sim.Simulation;
import model.sim.SimulationWorld;
import model.sim.adventure.AdventureInitializer;
import model.sim.adventure.AdventureRuleSystem;
import model.sim.board.DefaultPlantSpecSource;
import model.sim.zombie.ChapterZombieSpecSource;
import model.sim.zombie.ZombieSpecSource;
import model.inGame.plant.PlantDefinition;
import model.inGame.plant.PlantRepository;
import model.level.Level;
import model.level.LevelSelectionRules;
import model.user.Settings;
import model.user.User;
import service.DomainEventPublisher;
import service.UserService;
import util.RandomSource;
import util.SeededRandomSource;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
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
    private final RandomSource randomSource;
    private final DomainEventPublisher events;
    private model.sim.zombie.ZombieSpecSource zombieSpecSource;

    private Level level;
    private PlantSelection selection;

    public PlantSelectionController(PlantRepository plantRepository, UserService userService) {
        this(plantRepository, userService, new SeededRandomSource(), null);
    }

    public PlantSelectionController(
            PlantRepository plantRepository,
            UserService userService,
            RandomSource randomSource
    ) {
        this(plantRepository, userService, randomSource, null);
    }

    public PlantSelectionController(
            PlantRepository plantRepository,
            UserService userService,
            RandomSource randomSource,
            DomainEventPublisher events
    ) {
        this.plantRepository = plantRepository;
        this.userService = userService;
        this.randomSource = randomSource;
        this.events = events;
    }

    /** Opens the selection screen for a level; called when a level is entered. */
    public Result<String> begin(Level level) {
        Result<String> result = new Result<>();

        if (level == null) {
            result.appendToMessage("no level to select for");
            return result;
        }
        if (level.isBossDeferred()) {
            result.appendToMessage("boss gameplay is deferred to Phase 2");
            return result;
        }

        this.level = level;
        this.selection = new PlantSelection();
        for (PlantType forced : level.getSelectionRules().getForcedPlants()) {
            this.selection.add(forced);
        }
        Store.setCurrentMenu(model.enums.MenuName.PLANT_SELECTION);

        result.setStatus(true);
        result.setData(level.getName());
        result.appendToMessage("selecting plants for " + level.getName());

        return result;
    }

    /**
     * Opens a level from the chapter command and applies the documented
     * automatic-start rule. If the player owns fewer allowed plants than the
     * available slots, every such plant is selected and gameplay starts. A
     * level that bypasses selection also starts immediately.
     */
    public Result<GameSession> beginForPlayer(Level requestedLevel) {
        Result<GameSession> result = new Result<>();
        User user = Store.getLoggedInUser();
        if (user == null) {
            result.appendToMessage("no user is logged in");
            return result;
        }

        Result<String> beginResult = begin(requestedLevel);
        if (!beginResult.getStatus()) {
            result.appendToMessage(beginResult.getMessage());
            return result;
        }

        LevelSelectionRules rules = requestedLevel.getSelectionRules();
        if (rules.isSelectionBypassed()) {
            return startGame();
        }

        List<PlantType> available = availablePlantTypes(user);
        if (!available.isEmpty() && available.size() < rules.getCapacity()) {
            for (PlantType type : available) {
                selection.add(type);
            }
            return startGame();
        }

        result.setStatus(true);
        result.appendToMessage(beginResult.getMessage());
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
        if (level.getSelectionRules().isForced(type)) {
            result.appendToMessage("this plant is forced by the level");
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
        User user = validateStart(result);
        if (user == null) {
            return result;
        }

        GameSession session = createSession(user);
        Store.setActiveSession(session);
        Store.setActiveSimulation(createSimulation(user));
        Store.setCurrentMenu(model.enums.MenuName.GAMEPLAY);
        publishLevelStarted(user, session);

        result.setStatus(true);
        result.setData(session);
        result.appendToMessage("game started for " + level.getName());
        return result;
    }

    public void setZombieSpecSource(model.sim.zombie.ZombieSpecSource zombieSpecSource) {
        this.zombieSpecSource = zombieSpecSource;
    }

    public PlantSelection getSelection() {
        return selection;
    }

    /**
     * Whether the sky drops suns for this level. Level configs that disable sky
     * suns are not available yet, so this defaults to enabled.
     */
    private boolean skySunEnabled() {
        return level == null || level.getAdventureConfig() == null
                || level.getAdventureConfig().isSkySunEnabled();
    }

    private List<PlantType> availablePlantTypes(User user) {
        List<PlantType> available = new ArrayList<>();
        for (PlantDefinition definition : plantRepository.findAll()) {
            PlantType type = definition.getType();
            if (isSelectable(user, type)) {
                available.add(type);
            }
        }
        for (PlantType forced : level.getSelectionRules().getForcedPlants()) {
            if (!available.contains(forced)) {
                available.add(forced);
            }
        }
        return available;
    }

    private User validateStart(Result<GameSession> result) {
        if (level == null || selection == null) {
            result.appendToMessage("plant selection has not been started");
            return null;
        }
        User user = Store.getLoggedInUser();
        if (user == null) {
            result.appendToMessage("no user is logged in");
            return null;
        }
        LevelSelectionRules rules = level.getSelectionRules();
        if (!rules.isSelectionBypassed() && selection.isEmpty()) {
            result.appendToMessage("select at least one plant");
            return null;
        }
        if (selection.size() > rules.getCapacity()) {
            result.appendToMessage("too many plants selected");
            return null;
        }
        for (PlantType type : selection.getChosen()) {
            if (!isSelectable(user, type)) {
                result.appendToMessage("a selected plant is no longer available: " + type.name());
                return null;
            }
        }
        return user;
    }

    private GameSession createSession(User user) {
        return new GameSession(
                level,
                selection,
                difficultyOf(user),
                pendingGreenhouseBoosts(user)
        );
    }

    private Simulation createSimulation(User user) {
        SimulationWorld world = new SimulationWorld();
        DefaultPlantSpecSource plantSpecs = new DefaultPlantSpecSource(plantRepository);
        AdventureInitializer.initialize(
                world,
                level.getAdventureConfig(),
                plantSpecs,
                zombieSpecSource,
                user,
                randomSource);
        Simulation simulation = new Simulation(randomSource, world, skySunEnabled());
        registerSystems(simulation);
        return simulation;
    }

    private void registerSystems(Simulation simulation) {
        ZombieSpecSource levelZombies = zombieSpecSource == null ? null
                : new ChapterZombieSpecSource(zombieSpecSource, level.getName());
        if (levelZombies != null) {
            simulation.register(new model.sim.wave.WaveSystem(
                    level.getWaveConfig(), levelZombies));
        }
        if (level.getAdventureConfig() != null) {
            simulation.register(new AdventureRuleSystem(levelZombies));
        }
        if (levelZombies != null) {
            simulation.register(new model.sim.zombie.ZombieSpecialSystem());
            simulation.register(new model.sim.zombie.ZombieCombatSystem());
        }
    }

    private void publishLevelStarted(User user, GameSession session) {
        if (events == null) {
            return;
        }
        events.publish(DomainEventType.LEVEL_STARTED, user, Map.of(
                "level", level.getName(),
                "difficulty", String.valueOf(session.getDifficulty()),
                "chapter", level.getWorld() == null ? "" : level.getWorld().getDisplayName()
        ));
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
        return (user.getCollection().hasPlant(type) || level.getSelectionRules().isForced(type))
                && level.getSelectionRules().allows(type);
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
