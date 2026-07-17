package controller;

import model.Result;
import model.events.DomainEventType;
import model.Store;
import model.enums.MenuName;
import model.enums.PlantCategory;
import model.enums.PlantTag;
import model.enums.PlantType;
import model.enums.ZombieType;
import model.inGame.GameSession;
import model.inGame.plant.PlantDefinition;
import model.inGame.plant.PlantRegistry;
import model.sim.GameOutcome;
import model.sim.Simulation;
import model.sim.SimulationWorld;
import model.sim.sun.SunCollector;
import model.sim.zombie.ZombieDeath;
import model.sim.zombie.ZombieInstance;
import model.sim.zombie.ZombieSpec;
import model.level.SpecialLevelType;
import model.inGame.zombie.ZombieDefinition;
import model.inGame.zombie.ZombieRegistry;
import model.user.User;
import service.DomainEventPublisher;
import service.GameConclusionService;
import service.RewardService;

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

    /** Winning line printed when the player clears every wave. */
    public static final String WIN_MESSAGE =
            "Dear humanz, zis is not done yet; we will come back to eat your brainz, humanz.";

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
        SimulationWorld world = simulation.getWorld();
        if (world.getAdventureState() == null
                || world.getAdventureState().getConfig().getSpecialType()
                != SpecialLevelType.PLANT_WHAT_YOU_GET) {
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

    /** Handles {@code zombies info}. */
    public Result<List<String>> zombiesInfo() {
        Result<List<String>> result = new Result<>();
        List<String> lines = new ArrayList<>();
        for (ZombieInstance zombie : simulation.getWorld().getZombieInstances()) {
            if (!zombie.isDead()) {
                lines.add(zombie.infoText());
            }
        }
        result.setStatus(true);
        result.setData(lines);
        result.appendToMessage(lines.isEmpty() ? "no zombies on the map" : String.join("\n\n", lines));
        return result;
    }

    /** Handles {@code cheat spawn-zombie -t <type> -l <x, y>}. */
    public Result<ZombieInstance> spawnZombie(String typeToken, int x, int y) {
        Result<ZombieInstance> result = new Result<>();
        ZombieType type = ZombieType.fromToken(typeToken);
        ZombieRegistry registry = ZombieRegistry.getDefault();
        ZombieDefinition definition = type == null ? registry.findByName(typeToken)
                : registry.findByType(type);
        if (definition == null) {
            result.appendToMessage("unknown zombie type");
            return result;
        }
        if (definition.isBonus()) {
            result.appendToMessage("blue/bonus zombies are not available in the mandatory phase");
            return result;
        }
        SimulationWorld world = simulation.getWorld();
        if (!world.getBoard().isValidZombitePosition(x, y)) {
            result.appendToMessage("invalid zombie spawn tile");
            return result;
        }
        String levelName = session == null || session.getLevel() == null
                ? null : session.getLevel().getName();
        if (definition.getChapter() != model.inGame.zombie.ZombieChapter.COMMON
                && (levelName == null || !definition.getChapter().isAllowedIn(levelName))) {
            result.appendToMessage("this zombie is restricted to " + definition.getChapter());
            return result;
        }
        ZombieInstance zombie = new ZombieInstance(ZombieSpec.fromDefinition(definition), x, y);
        world.addZombie(zombie);
        if (user != null) {
            user.getCollection().markZombieAsSeen(definition.getType());
        }
        result.setStatus(true);
        result.setData(zombie);
        result.appendToMessage("spawned " + definition.getName() + " at (" + x + ", " + y + ")");
        return result;
    }

    /** Handles {@code release the nuke}: kill every zombie, with normal cleanup. */
    public Result<List<String>> releaseNuke() {
        Result<List<String>> result = new Result<>();
        SimulationWorld world = simulation.getWorld();
        List<String> messages = new ArrayList<>();

        for (ZombieInstance zombie : world.getZombieInstances()) {
            if (zombie.isDead()) {
                continue;
            }

            zombie.kill();
            model.sim.zombie.ZombieSpecialSystem.onDeath(zombie, world, messages::add);
            messages.add("Zombie of type " + zombie.getSpec().getName()
                    + " is dead at (" + zombie.getTileX() + ", " + zombie.getRow() + ")");
            world.recordDeath(new ZombieDeath(
                    zombie.getSpec(), zombie.getTileX(), zombie.getRow(),
                    zombie.isGlowing(), true));
        }

        world.getZombies().removeIf(z -> z instanceof ZombieInstance && ((ZombieInstance) z).isDead());

        messages.addAll(drainRewards());

        result.setStatus(true);
        result.setData(messages);
        result.appendToMessage(messages.isEmpty()
                ? "no zombies to nuke" : String.join("\n", messages));

        return result;
    }

    /**
     * Applies rewards for zombies that died. Cheat kills (the nuke) are cleaned
     * up like any death but grant no rewards — a deliberate choice, since no
     * repository test or asset says a cheat should farm drops, and letting a
     * cheat produce coins/diamonds/plant food would be exploitable.
     */
    private List<String> drainRewards() {
        List<String> messages = new ArrayList<>();
        SimulationWorld world = simulation.getWorld();

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
        SimulationWorld world = simulation.getWorld();
        Map<String, String> attributes = new LinkedHashMap<>();
        attributes.put("won", String.valueOf(won));
        attributes.put("sunBalance", String.valueOf(world.getSunBalance()));
        attributes.put("plantLossCount", String.valueOf(world.getPlantLossCount()));
        attributes.put("emptyRows", emptyRows(world));
        attributes.put("emptyColumns", emptyColumns(world));
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

    private String emptyRows(SimulationWorld world) {
        StringJoiner rows = new StringJoiner(",");
        for (int row = 0; row < world.getRows(); row++) {
            boolean empty = true;
            for (int column = 0; column < world.getColumns(); column++) {
                if (world.getBoard().tileAt(column, row).hasAnyPlant()) {
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

    private String emptyColumns(SimulationWorld world) {
        StringJoiner columns = new StringJoiner(",");
        for (int column = 0; column < world.getColumns(); column++) {
            boolean empty = true;
            for (int row = 0; row < world.getRows(); row++) {
                if (world.getBoard().tileAt(column, row).hasAnyPlant()) {
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

    /** Handles {@code collect sun -l (<x>, <y>)}. */
    public Result<Integer> collectSun(int x, int y) {
        Result<Integer> result = new Result<>();
        SunCollector.Outcome outcome = simulation.collectSun(x, y);

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

        result.appendToMessage("collected " + outcome.getGained()
                + " sun; total " + simulation.getSunAmount());

        return result;
    }

    /** Handles {@code show sun amount}. */
    public Result<Integer> showSunAmount() {
        Result<Integer> result = new Result<>();

        result.setStatus(true);
        result.setData(simulation.getSunAmount());
        result.appendToMessage("sun: " + simulation.getSunAmount());

        return result;
    }

    /** Handles {@code cheat add -n <count> suns}. */
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
}
