package controller;

import model.GameEngine;
import model.Result;
import model.events.DomainEventType;
import model.Store;
import model.enums.PlantType;
import model.inGame.GameSession;
import model.inGame.PlantSelection;
import model.sim.Simulation;
import model.sim.adventure.AdventureInitializer;
import model.sim.adventure.AdventureRuleSystem;
import model.sim.wave.WaveSystem;
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
import model.enums.PlantCategory;
import java.util.Set;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
public class PlantSelectionController {

    public static final int DIAMOND_BOOST_COST = 2;
    private final PlantRepository plantRepository;
    private final UserService userService;
    private final RandomSource randomSource;
    private final DomainEventPublisher events;
    private ZombieSpecSource zombieSpecSource;

    private Level level;
    private PlantSelection selection;
    private boolean cheatCapacityBypassed;

    public PlantSelectionController(PlantRepository plantRepository, UserService userService) {
        this(plantRepository, userService, new SeededRandomSource(), null);
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
        this.cheatCapacityBypassed = false;
        for (PlantType forced : level.getSelectionRules().getForcedPlants()) {
            this.selection.add(forced);
        }
        Store.setCurrentMenu(model.enums.MenuName.PLANT_SELECTION);

        result.setStatus(true);
        result.setData(level.getName());
        result.appendToMessage("selecting plants for " + level.getName());

        return result;
    }

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

        /*
         * Phase 2 requires an actual plant-selection screen for every level whose
         * selection is not explicitly bypassed (for example Conveyor Belt).
         *
         * The old Phase-1 convenience path auto-selected every available plant
         * and immediately started the game when the player owned fewer plants
         * than the level capacity. That made Locked Plants — and potentially
         * ordinary levels on smaller collections — skip the selection UI.
         */
        result.setStatus(true);
        result.appendToMessage(beginResult.getMessage());
        return result;
    }
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

    public Result<List<String>> showLockedPlants() {
        Result<List<String>> result = new Result<>();

        if (notInSelection(result)) {
            return result;
        }

        User user = Store.getLoggedInUser();
        List<String> locked = new ArrayList<>();

        for (PlantDefinition definition : plantRepository.findAll()) {
            PlantType type = definition.getType();
            if (!user.getCollection().hasPlant(type) || level.getSelectionRules().isForced(type)) {
                continue;
            }

            if (!level.getSelectionRules().allows(type)) {
                locked.add(type.name() + " - " + lockReason(definition));
            }
        }

        result.setStatus(true);
        result.setData(locked);

        if (locked.isEmpty()) {
            result.appendToMessage("no locked plants for this level");
            return result;
        }

        for (int i = 0; i < locked.size(); i++) {
            result.appendToMessage(locked.get(i));
            if (i < locked.size() - 1) {
                result.appendToMessage("\n");
            }
        }

        return result;
    }

    private String lockReason(PlantDefinition definition) {
        LevelSelectionRules rules = level.getSelectionRules();
        Set<PlantCategory> excludedCategories = rules.getExcludedCategories();
        Set<PlantType> allowedPlants = rules.getAllowedPlants();

        if (excludedCategories.contains(definition.getCategory())) {
            return "locked: the entire " + definition.getCategory().name() + " family is unavailable in this level";
        }
        if (!allowedPlants.isEmpty() && !allowedPlants.contains(definition.getType())) {
            return "locked: this level restricts selection to a fixed plant list";
        }
        return "locked by level rules";
    }
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

    /** Cheat: selects every plant this level allows, ignoring the normal selection
     *  capacity cap. Marks the bypass so {@link #startGame()} doesn't reject the
     *  oversized roster; you'll place them onto the board a handful at a time. */
    public Result<String> cheatSelectAllPlants() {
        Result<String> result = new Result<>();

        if (notInSelection(result)) {
            return result;
        }

        User user = Store.getLoggedInUser();
        int added = 0;
        for (PlantType type : availablePlantTypes(user)) {
            if (selection.add(type)) {
                added++;
            }
        }
        cheatCapacityBypassed = true;

        result.setStatus(true);
        result.appendToMessage("selected all " + selection.size() + " available plants ("
                + added + " newly added), capacity cap bypassed for this level");
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
        Store.setActiveSimulation(new Simulation(createSimulation(user)));
        Store.setCurrentMenu(model.enums.MenuName.GAMEPLAY);
        publishLevelStarted(user, session);

        result.setStatus(true);
        result.setData(session);
        result.appendToMessage("game started for " + level.getName());
        return result;
    }

    public void setZombieSpecSource(ZombieSpecSource zombieSpecSource) {
        this.zombieSpecSource = zombieSpecSource;
    }

    public PlantSelection getSelection() {
        return selection;
    }

    /** Phase-2 UI helpers for level-specific plant-selection presentation. */
    public boolean isLockedPlantsLevel() {
        return level != null && level.getAdventureConfig() != null
                && level.getAdventureConfig().getSpecialType()
                == model.level.SpecialLevelType.LOCKED_PLANTS;
    }

    public boolean isForcedPlant(PlantType type) {
        return level != null && type != null && level.getSelectionRules().isForced(type);
    }

    public Set<PlantType> getForcedPlants() {
        return level == null ? Set.of() : level.getSelectionRules().getForcedPlants();
    }

    public Set<PlantCategory> getExcludedCategories() {
        return level == null ? Set.of() : level.getSelectionRules().getExcludedCategories();
    }

    public int getSelectionCapacity() {
        return level == null ? LevelSelectionRules.DEFAULT_CAPACITY
                : level.getSelectionRules().getCapacity();
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
        if (!cheatCapacityBypassed && selection.size() > rules.getCapacity()) {
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

    private GameEngine createSimulation(User user) {
        GameEngine engine = new GameEngine();// یا سازنده‌ای که random/seed می‌گیره، اگه لازمه
        System.out.println(level.getAdventureConfig().getName());
        System.out.println(level.getAdventureConfig().getSpecialType());
        AdventureInitializer.initialize(engine, level.getAdventureConfig(), user, randomSource);
        engine.setSkySunEnabled(skySunEnabled());
        registerSystems(engine);
        return engine;
    }

    private void registerSystems(GameEngine engine) {
        ZombieSpecSource levelZombies = zombieSpecSource == null ? null
                : new ChapterZombieSpecSource(zombieSpecSource, level.getName());
        if (levelZombies != null) {
            engine.setWaveSystem(new WaveSystem(level.getWaveConfig(), levelZombies));
        }
        if (level.getAdventureConfig() != null) {
            engine.setAdventureRuleSystem(new AdventureRuleSystem());
        }
        // ZombieSpecialSystem و ZombieCombatSystem دیگر لازم نیستند:
        // در دنیای A، حرکت/حمله/مرگ‌های ویژه از قبل داخل CompositeZombieBehavior هستند.
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
