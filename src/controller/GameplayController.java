package controller;

import model.GameEngine;
import model.Result;
import model.events.DomainEventType;
import model.Store;
import model.enums.MenuName;
import model.enums.PlantCategory;
import model.enums.PlantTag;
import model.enums.PlantType;
import model.enums.ZombieType;
import model.inGame.GameOutcome;
import model.inGame.GameSession;
import model.inGame.plant.PlantDefinition;
import model.inGame.plant.PlantRegistry;
import model.inGame.zombie.Zombie;
import model.level.LevelSelectionRules;
import model.sim.Simulation;
import model.sim.zombie.ZombieDeath;
import model.level.SpecialLevelType;
import model.inGame.zombie.ZombieDefinition;
import model.inGame.zombie.ZombieRegistry;
import model.user.User;
import service.DomainEventPublisher;
import service.GameConclusionService;
import service.RewardService;
import java.util.Set;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;

/**
 * The gameplay command context entered after {@code start game}: driving the
 * clock and the sun/board/zombie commands against the active {@link Simulation}.
 */
public class GameplayController {
    private final Simulation simulation;
    private final BoardController board;
    private final RewardService rewardService;
    private final GameConclusionService conclusionService;
    private final GameSession session;
    private final User user;
    private final DomainEventPublisher events;

    public GameplayController(Simulation simulation) {
        this(simulation, null);
    }

    public GameplayController(Simulation simulation, BoardController board) {
        this(simulation, board, null, null, null, null);
    }

    public GameplayController(
            Simulation simulation,
            BoardController board,
            RewardService rewardService,
            GameConclusionService conclusionService,
            GameSession session,
            User user
    ) {
        this(simulation, board, rewardService, conclusionService, session, user, null);
    }

    public GameplayController(
            Simulation simulation,
            BoardController board,
            RewardService rewardService,
            GameConclusionService conclusionService,
            GameSession session,
            User user,
            DomainEventPublisher events
    ) {
        this.simulation = simulation;
        this.board = board;
        this.rewardService = rewardService;
        this.conclusionService = conclusionService;
        this.session = session;
        this.user = user;
        this.events = events;
    }

    public BoardController getBoard() {
        return board;
    }

    public Simulation getSimulation() {
        return simulation;
    }

    /** Handles {@code advance time -t <count> ticks}. */
    public Result<List<String>> advanceTime(int ticks) {
        Result<List<String>> result = new Result<>();

        if (ticks <= 0) {
            result.appendToMessage("tick count must be a positive integer");
            return result;
        }

        int producedBefore = simulation.getWorld().getProducedSunTotal();
        List<String> simulationEvents = simulation.advance(ticks);
        List<String> messages = new ArrayList<>(simulationEvents);
        int produced = simulation.getWorld().getProducedSunTotal() - producedBefore;
        if (produced > 0 && events != null && user != null) {
            events.publish(DomainEventType.SUN_PRODUCED, user, produced, Map.of());
        }

        messages.addAll(drainRewards());
        messages.addAll(resolveOutcome());

        result.setStatus(true);
        result.setData(messages);

        if (messages.isEmpty()) {
            result.appendToMessage("advanced " + ticks + " tick(s)");
            return result;
        }

        for (int i = 0; i < messages.size(); i++) {
            result.appendToMessage(messages.get(i));

            if (i < messages.size() - 1) {
                result.appendToMessage("\n");
            }
        }

        return result;
    }


    /** Handles {@code start zombie waves} for Plant What You Get levels. */
    public Result<String> startZombieWaves() {
        Result<String> result = new Result<>();
        GameEngine world = simulation.getWorld(); // ← فقط تایپ
        if (world.getAdventureState() == null
                || world.getAdventureState().getConfig().getSpecialType() != SpecialLevelType.PLANT_WHAT_YOU_GET) {
            result.appendToMessage("this level does not use delayed zombie waves");
            return result;
        }
        if (world.areWavesStarted()) {
            result.appendToMessage("zombie waves have already started");
            return result;
        }
        world.setWavesStarted(true);
        result.setStatus(true);
        result.setData("started");
        result.appendToMessage("zombie waves started");
        return result;
    }

    public Result<List<String>> zombiesInfo() {
        Result<List<String>> result = new Result<>();
        List<String> lines = new ArrayList<>();
        for (Zombie zombie : simulation.getWorld().getZombies()) {
            if (!zombie.isDead()) {
                lines.add(zombie.infoText());
            }
        }
        result.setStatus(true);
        result.setData(lines);
        result.appendToMessage(lines.isEmpty() ? "no zombies on the map" : String.join("\n\n", lines));
        return result;
    }

    public Result<Zombie> spawnZombie(String typeToken, int x, int y) {
        Result<Zombie> result = new Result<>();
        ZombieType type = ZombieType.fromToken(typeToken);
        ZombieRegistry registry = ZombieRegistry.getDefault();
        ZombieDefinition definition = type == null ? registry.findByName(typeToken) : registry.findByType(type);
        if (definition == null) {
            result.appendToMessage("unknown zombie type");
            return result;
        }
        GameEngine world = simulation.getWorld();
        if (y < 0 || y >= world.getGameMap().getRows() || x < 0 || x > world.getGameMap().getColumns()) {
            result.appendToMessage("invalid zombie spawn tile");
            return result;
        }
        String levelName = session == null || session.getLevel() == null ? null : session.getLevel().getName();
        if (definition.getChapter() != model.inGame.zombie.ZombieChapter.COMMON
                && (levelName == null || !definition.getChapter().isAllowedIn(levelName))) {
            result.appendToMessage("this zombie is restricted to " + definition.getChapter());
            return result;
        }
        Zombie zombie = world.spawnZombie(definition.getType(), y, x);// (row, x)
        if (rewardService != null && rewardService.rollGlowing()) {
            zombie.putState("GLOWING", true);
        }
        if (user != null) {
            user.getCollection().markZombieAsSeen(definition.getType());
        }
        result.setStatus(true);
        result.setData(zombie);
        result.appendToMessage("spawned " + definition.getName() + " at (" + x + ", " + y + ")");
        return result;
    }

    public Result<List<String>> releaseNuke() {
        Result<List<String>> result = new Result<>();
        List<String> messages = simulation.getWorld().killAllZombies();
        messages.addAll(drainRewards());
        result.setStatus(true);
        result.setData(messages);
        result.appendToMessage(messages.isEmpty() ? "no zombies to nuke" : String.join("\n", messages));
        return result;
    }

    private List<String> drainRewards() {
        List<String> messages = new ArrayList<>();
        GameEngine world = simulation.getWorld(); // ← SimulationWorld بود، حالا GameEngine

        for (ZombieDeath death : world.getPendingDeaths()) {
            if (death.isCheatKill() || user == null) {
                continue;
            }
            if (events != null) {
                Map<String, String> attributes = new LinkedHashMap<>();
                attributes.put("tileX", String.valueOf(death.getTileX()));
                attributes.put("row", String.valueOf(death.getRow()));
                attributes.put("mowerReady", String.valueOf(!world.isLawnMowerUsed(death.getRow())));
                if (session != null && session.getLevel() != null
                        && session.getLevel().getWorld() != null) {
                    attributes.put("chapter", session.getLevel().getWorld().getDisplayName());
                }
                events.publish(DomainEventType.ZOMBIE_KILLED, user, attributes);
            }
            if (rewardService != null) {
                messages.addAll(rewardService.onZombieDeath(death.isGlowing(), user, world));
            }
        }

        world.getPendingDeaths().clear();
        return messages;
    }

    private List<String> resolveOutcome() {
        List<String> messages = new ArrayList<>();
        GameOutcome outcome = simulation.getWorld().getOutcome();

        if (outcome == GameOutcome.WON) {
            publishLevelCompleted(true);
            if (conclusionService != null) {
                messages.addAll(conclusionService.onWin(user, session));
            }
            Store.setCurrentMenu(MenuName.GAME);
            Store.setActiveSimulation(null);
        } else if (outcome == GameOutcome.LOST) {
            publishLevelCompleted(false);
            if (conclusionService != null) {
                conclusionService.onLoss(user, session);
            }
            Store.setCurrentMenu(MenuName.GAME);
            Store.setActiveSimulation(null);
        }
        return messages;
    }
    private void publishLevelCompleted(boolean won) {
        if (events == null || user == null) {
            return;
        }
        GameEngine world = simulation.getWorld();
        Map<String, String> attributes = new LinkedHashMap<>();
        attributes.put("won", String.valueOf(won));
        attributes.put("sunBalance", String.valueOf(world.getSunBalance()));
        attributes.put("plantLossCount", String.valueOf(world.getPlantLossCount()));
        attributes.put("emptyRows", emptyRows(world));
        attributes.put("emptyColumns", emptyColumns(world));
        attributes.put("boardSymmetric", String.valueOf(isBoardSymmetric(world)));
        if (session != null) {
            attributes.put("difficulty", String.valueOf(session.getDifficulty()));
            if (session.getLevel() != null) {
                attributes.put("level", session.getLevel().getName());
                if (session.getLevel().getWorld() != null) {
                    attributes.put("chapter", session.getLevel().getWorld().getDisplayName());
                }
            }
            addUsedPlantAttributes(attributes, session.getUsedPlants());
        }
        events.publish(DomainEventType.LEVEL_COMPLETED, user, attributes);
    }
    private boolean isBoardSymmetric(GameEngine world) {
        var map = world.getGameMap();
        int rows = map.getRows();
        for (int row = 0; row < rows / 2; row++) {
            int mirrorRow = rows - 1 - row;
            for (int column = 0; column < map.getColumns(); column++) {
                if (!sameFootprint(map.getTile(row, column), map.getTile(mirrorRow, column))) {
                    return false;
                }
            }
        }
        return true;
    }
    private boolean sameFootprint(model.Tile a, model.Tile b) {
        return samePlantType(a.getSupportPlant(), b.getSupportPlant())
                && samePlantType(a.getPrimaryPlant(), b.getPrimaryPlant())
                && samePlantType(a.getArmorPlant(), b.getArmorPlant());
    }
    private boolean samePlantType(model.inGame.plant.Plant a, model.inGame.plant.Plant b) {
        model.enums.PlantType typeA = a == null ? null : a.getEffectiveType();
        model.enums.PlantType typeB = b == null ? null : b.getEffectiveType();
        return typeA == typeB;
    }
    private void addUsedPlantAttributes(Map<String, String> attributes, Set<PlantType> used) {
        Set<String> families = new LinkedHashSet<>();
        boolean allNight = !used.isEmpty();
        boolean allSun = !used.isEmpty();
        for (PlantType type : used) {
            PlantDefinition definition = PlantRegistry.getDefault().findByType(type);
            if (definition == null) {
                allNight = false;
                allSun = false;
                continue;
            }
            PlantCategory category = definition.getBehaviorCategory();
            if (category != null) {
                families.add(category.name());
            }
            allNight &= definition.hasTag(PlantTag.NIGHT) || definition.hasTag(PlantTag.SHROOM);
            allSun &= category == PlantCategory.SUN_PRODUCER || definition.hasTag(PlantTag.SUN);
        }
        attributes.put("usedPlantFamilies", String.join(",", families));
        attributes.put("plantsUsedCount", String.valueOf(used.size()));
        attributes.put("allUsedPlantsNight", String.valueOf(allNight));
        attributes.put("allUsedPlantsSunProducers", String.valueOf(allSun));
    }
    private String emptyRows(GameEngine world) {
        StringJoiner rows = new StringJoiner(",");
        for (int row = 0; row < world.getGameMap().getRows(); row++) {
            boolean empty = true;
            for (int column = 0; column < world.getGameMap().getColumns(); column++) {
                if (world.getGameMap().getTile(row, column).hasAnyPlant()) {
                    empty = false;
                    break;
                }
            }
            if (empty) {
                rows.add(String.valueOf(row));
            }
        }
        return rows.toString();
    }
    private String emptyColumns(GameEngine world) {
        StringJoiner columns = new StringJoiner(",");
        for (int column = 0; column < world.getGameMap().getColumns(); column++) {
            boolean empty = true;
            for (int row = 0; row < world.getGameMap().getRows(); row++) {
                if (world.getGameMap().getTile(row, column).hasAnyPlant()) {
                    empty = false;
                    break;
                }
            }
            if (empty) {
                columns.add(String.valueOf(column));
            }
        }
        return columns.toString();
    }
    public Result<Integer> cheatCollectAllSuns() {
        Result<Integer> result = new Result<>();
        int gained = simulation.cheatCollectAllSuns();
        result.setStatus(true);
        result.setData(simulation.getSunAmount());
        result.appendToMessage("collected " + gained + " sun from the board; total "
                + simulation.getSunAmount());
        return result;
    }

    public Result<Integer> collectSun(int x, int y) {
        Result<Integer> result = new Result<>();
        GameEngine.SunCollectionOutcome outcome = simulation.collectSun(x, y); // ← فقط این خط تغییر کرد

        if (!outcome.isCollected()) {
            result.appendToMessage("no sun at (" + x + ", " + y + ")");
            result.setData(simulation.getSunAmount());
            return result;
        }
        result.setStatus(true);
        result.setData(simulation.getSunAmount());
        if (outcome.isExploded()) {
            result.appendToMessage("radioactive sun exploded at (" + x + ", " + y + ")");
            return result;
        }
        result.appendToMessage("collected " + outcome.getGained() + " sun; total " + simulation.getSunAmount());
        return result;
    }
    public Result<Integer> showSunAmount() {
        Result<Integer> result = new Result<>();

        result.setStatus(true);
        result.setData(simulation.getSunAmount());
        result.appendToMessage("sun: " + simulation.getSunAmount());

        return result;
    }
    public Result<Integer> cheatAddSuns(int count) {
        Result<Integer> result = new Result<>();

        if (count <= 0) {
            result.appendToMessage("count must be a positive integer");
            result.setData(simulation.getSunAmount());
            return result;
        }

        simulation.addSun(count);

        result.setStatus(true);
        result.setData(simulation.getSunAmount());
        result.appendToMessage("sun: " + simulation.getSunAmount());

        return result;
    }
    public Result<List<String>> showAvailablePlantsInGame() {
        Result<List<String>> result = new Result<>();
        LevelSelectionRules rules = currentRules(result);
        if (rules == null) {
            return result;
        }
        List<String> lines = new ArrayList<>();
        for (PlantDefinition definition : PlantRegistry.getDefault().findAll()) {
            PlantType type = definition.getType();
            boolean owned = user != null && user.getCollection().hasPlant(type);
            if ((owned || rules.isForced(type)) && rules.allows(type)) {
                lines.add(definition.getDisplayText());
            }
        }
        result.setStatus(true);
        result.setData(lines);
        if (lines.isEmpty()) {
            result.appendToMessage("no available plants");
            return result;
        }
        for (int i = 0; i < lines.size(); i++) {
            result.appendToMessage(lines.get(i));
            if (i < lines.size() - 1) result.appendToMessage("\n\n");
        }
        return result;
    }
    public Result<List<String>> showLockedPlantsInGame() {
        Result<List<String>> result = new Result<>();
        LevelSelectionRules rules = currentRules(result);
        if (rules == null) {
            return result;
        }

        List<String> lines = new ArrayList<>();
        for (PlantDefinition definition : PlantRegistry.getDefault().findAll()) {
            PlantType type = definition.getType();
            boolean owned = user != null && user.getCollection().hasPlant(type);
            if (!owned || rules.isForced(type)) {
                continue;
            }
            if (!rules.allows(type)) {
                lines.add(type.name() + " - " + lockReason(rules, definition));
            }
        }

        result.setStatus(true);
        result.setData(lines);
        if (lines.isEmpty()) {
            result.appendToMessage("no locked plants for this level");
            return result;
        }
        for (int i = 0; i < lines.size(); i++) {
            result.appendToMessage(lines.get(i));
            if (i < lines.size() - 1) result.appendToMessage("\n");
        }
        return result;
    }
    private LevelSelectionRules currentRules(Result<?> result) {
        if (session == null || session.getLevel() == null) {
            result.appendToMessage("no level rules available");
            return null;
        }
        return session.getLevel().getSelectionRules();
    }
    private String lockReason(LevelSelectionRules rules, PlantDefinition definition) {
        Set<model.enums.PlantCategory> excludedCategories = rules.getExcludedCategories();
        Set<PlantType> allowedPlants = rules.getAllowedPlants();

        if (excludedCategories.contains(definition.getCategory())) {
            return "locked: the entire " + definition.getCategory().name() + " family is unavailable in this level";
        }
        if (!allowedPlants.isEmpty() && !allowedPlants.contains(definition.getType())) {
            return "locked: this level restricts selection to a fixed plant list";
        }
        return "locked by level rules";
    }
}
