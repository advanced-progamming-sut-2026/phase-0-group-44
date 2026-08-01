package model.scored;

import controller.MiniGameBoardController;
import model.Result;
import model.enums.PlantType;
import model.inGame.PlantSelection;
import model.inGame.plant.PlantDefinition;
import model.inGame.plant.PlantRegistry;
import model.inGame.zombie.ZombieRegistry;
import model.sim.GameOutcome;
import model.sim.MiniGameSimulation;
import model.sim.SimulationWorld;
import model.sim.TickContext;
import model.sim.board.DefaultPlantSpecSource;
import model.sim.board.PlantInstance;
import model.sim.zombie.ZombieCombatSystem;
import model.sim.zombie.ZombieInstance;
import model.sim.zombie.ZombieSpec;
import model.sim.zombie.ZombieSpecialSystem;
import model.user.User;
import util.RandomSource;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** One daily scored-game attempt using the shared board, clock, sun, and combat systems. */
public final class ScoredGameSession {
    public enum State { READY, RUNNING, WON, LOST, FORFEITED }

    private final String ownerUsername;
    private final LocalDate date;
    private final DailyZombieSequence sequence;
    private final MiniGameSimulation simulation;
    private final PlantSelection selection;
    private final MiniGameBoardController board;
    private final ScoredGameScore score = new ScoredGameScore();
    private final PlantRegistry plants = PlantRegistry.getDefault();
    private final ZombieRegistry zombies = ZombieRegistry.getDefault();
    private final Map<PlantInstance, Long> nextAttack = new IdentityHashMap<>();
    private final Map<ZombieInstance, Long> spawnTicks = new IdentityHashMap<>();
    private final Map<ZombieInstance, Boolean> scoredDeaths = new IdentityHashMap<>();
    private int nextSpawn;
    private int deathsThisTick;
    private State state = State.READY;
    private boolean settled;

    public ScoredGameSession(User user, LocalDate date, RandomSource random) {
        if (user == null || user.getUsername() == null || date == null || random == null) {
            throw new IllegalArgumentException("A scored-game session requires a user, date, and RNG.");
        }
        this.ownerUsername = user.getUsername();
        this.date = date;
        this.sequence = new DailyZombieSequenceGenerator().generate(date);
        SimulationWorld world = new SimulationWorld();
        world.addSun(50);
        this.simulation = new MiniGameSimulation(random, world, true);
        this.selection = selectionFor(user);
        this.board = new MiniGameBoardController(world, selection, new DefaultPlantSpecSource(plants));
    }

    public String getOwnerUsername() { return ownerUsername; }
    public MiniGameSimulation getSimulation() { return simulation; }
    public PlantSelection getSelection() { return selection; }
    public ScoredGameScore getScore() { return score; }
    public State getState() { return state; }
    public boolean isSettled() { return settled; }
    public boolean markSettled() { if (settled || !isTerminal()) return false; settled = true; return true; }
    public boolean isTerminal() { return state == State.WON || state == State.LOST || state == State.FORFEITED; }

    public Result<String> start() {
        Result<String> result = new Result<>();
        if (state != State.READY) {
            result.appendToMessage("the scored game has already started or finished");
            return result;
        }
        simulation.register(this::spawnAndAttack);
        simulation.register(new ZombieSpecialSystem());
        simulation.register(new ZombieCombatSystem());
        simulation.register(this::scoreAndEvaluate);
        state = State.RUNNING;
        result.setStatus(true);
        result.setData(date.toString());
        result.appendToMessage("started daily scored game for " + date
                + "; selected plants: " + selection.getChosen());
        return result;
    }

    public Result<List<String>> advance(int ticks) {
        Result<List<String>> result = new Result<>();
        if (state != State.RUNNING) {
            result.appendToMessage("no scored game is running");
            return result;
        }
        if (ticks <= 0) {
            result.appendToMessage("tick count must be positive");
            return result;
        }
        List<String> messages = simulation.advance(ticks);
        updateStateFromWorld();
        result.setStatus(true);
        result.setData(List.copyOf(messages));
        result.appendToMessage(messages.isEmpty() ? "advanced " + ticks + " tick(s)"
                : String.join("\n", messages));
        return result;
    }

    public Result<String> plant(String token, int x, int y) {
        if (state != State.RUNNING) return failure("no scored game is running");
        PlantType type;
        try {
            type = PlantType.fromCanonicalName(token);
        } catch (RuntimeException exception) {
            return failure("unknown plant");
        }
        return board.plantPlant(type, x, y);
    }

    public Result<String> collectSun(int x, int y) {
        Result<String> result = new Result<>();
        if (state != State.RUNNING) return failure("no scored game is running");
        var outcome = simulation.collectSun(x, y);
        if (!outcome.isCollected()) {
            result.appendToMessage("no sun at that tile");
            return result;
        }
        result.setStatus(true);
        result.setData(String.valueOf(simulation.getSunAmount()));
        String action = outcome.isExploded()
                ? "radioactive sun exploded"
                : "collected " + outcome.getGained() + " sun";
        result.appendToMessage(action + "; sun: " + simulation.getSunAmount());
        return result;
    }

    public Result<String> forfeit() {
        Result<String> result = new Result<>();
        if (state != State.RUNNING) {
            result.appendToMessage("no scored game is running");
            return result;
        }
        simulation.getWorld().setOutcome(GameOutcome.LOST);
        state = State.FORFEITED;
        finalizeScore(GameOutcome.LOST);
        result.setStatus(true);
        result.appendToMessage("scored game forfeited; final Mew Point: " + score.total());
        return result;
    }

    public String status() {
        StringBuilder output = new StringBuilder();
        output.append("daily scored game ").append(date).append('\n')
                .append("state: ").append(state).append('\n')
                .append("spawned: ").append(nextSpawn).append('/').append(sequence.spawns().size()).append('\n')
                .append("sun: ").append(simulation.getSunAmount()).append('\n')
                .append("Mew Point: ").append(score.total());
        for (ScorePattern pattern : ScorePattern.values()) {
            output.append('\n').append(pattern.getLabel()).append(": ")
                    .append(score.getPoints(pattern));
        }
        return output.toString();
    }

    private void spawnAndAttack(TickContext context) {
        if (!context.getWorld().isRunning()) return;
        long tick = context.getCurrentTick() + 1;
        deathsThisTick = 0;
        while (nextSpawn < sequence.spawns().size()
                && sequence.spawns().get(nextSpawn).spawnTick() <= tick) {
            DailyZombieSpawn spawn = sequence.spawns().get(nextSpawn++);
            ZombieSpec spec = ZombieSpec.fromDefinition(zombies.requireMandatory(spawn.type()));
            ZombieInstance zombie = new ZombieInstance(spec,
                    context.getWorld().getColumns() - 0.01, spawn.row());
            context.getWorld().addZombie(zombie);
            spawnTicks.put(zombie, tick);
            context.emit("Scored-game zombie entered: " + spawn.type() + " row " + spawn.row() + ".");
        }
        attackWithPlants(context, tick);
    }

    private void attackWithPlants(TickContext context, long tick) {
        List<PlantInstance> livePlants = new ArrayList<>();
        for (var damageable : context.getWorld().getPlants()) {
            if (damageable instanceof PlantInstance plant && !plant.isDead() && plant.isActive()) {
                livePlants.add(plant);
            }
        }
        for (PlantInstance plant : livePlants) {
            long ready = nextAttack.getOrDefault(plant, 0L);
            if (tick < ready) continue;
            PlantDefinition definition = plants.require(plant.getType());
            int interval = Math.max(1, (int) Math.round(definition.getActionInterval() * TickContext.TICKS_PER_SECOND));
            nextAttack.put(plant, tick + interval);
            int damage = Math.max(0, definition.getDamage());
            if (damage <= 0) continue;
            List<ZombieInstance> targets = targetsFor(plant, context.getWorld().getZombieInstances());
            int killedByShot = 0;
            for (ZombieInstance zombie : targets) {
                if (zombie.isDead()) continue;
                zombie.takeDamage(damage);
                if (zombie.isDead()) {
                    killedByShot++;
                    scoreDeath(zombie, tick);
                }
            }
            score.recordProjectileKillCount(killedByShot);
        }
    }

    private List<ZombieInstance> targetsFor(PlantInstance plant, List<ZombieInstance> candidates) {
        List<Integer> rows = new ArrayList<>();
        rows.add(plant.getTileY());
        if (plant.getType() == PlantType.THREEPEATER) {
            if (plant.getTileY() > 0) rows.add(plant.getTileY() - 1);
            if (plant.getTileY() < 4) rows.add(plant.getTileY() + 1);
        }
        List<ZombieInstance> result = new ArrayList<>();
        for (int row : rows) {
            ZombieInstance nearest = null;
            for (ZombieInstance zombie : candidates) {
                if (zombie.isDead() || zombie.getRow() != row || zombie.getX() < plant.getTileX()) continue;
                if (nearest == null || zombie.getX() < nearest.getX()) nearest = zombie;
            }
            if (nearest != null) result.add(nearest);
        }
        return result;
    }

    private void scoreAndEvaluate(TickContext context) {
        long tick = context.getCurrentTick() + 1;
        for (ZombieInstance zombie : new ArrayList<>(spawnTicks.keySet())) {
            if (zombie.isDead() && !scoredDeaths.containsKey(zombie)) {
                scoreDeath(zombie, tick);
            }
        }
        score.recordSimultaneousKillCount(deathsThisTick);
        if (context.getWorld().getOutcome() == GameOutcome.LOST) {
            state = State.LOST;
            finalizeScore(GameOutcome.LOST);
            return;
        }
        if (nextSpawn >= sequence.spawns().size()
                && context.getWorld().getZombieInstances().isEmpty()) {
            context.getWorld().setOutcome(GameOutcome.WON);
            state = State.WON;
            finalizeScore(GameOutcome.WON);
            context.emit("Scored game complete; final Mew Point: " + score.total());
        }
    }

    private void scoreDeath(ZombieInstance zombie, long tick) {
        if (scoredDeaths.put(zombie, Boolean.TRUE) != null) return;
        score.recordQuickKill(spawnTicks.getOrDefault(zombie, tick), tick);
        deathsThisTick++;
    }

    private void updateStateFromWorld() {
        GameOutcome outcome = simulation.getWorld().getOutcome();
        if (outcome == GameOutcome.WON) state = State.WON;
        else if (outcome == GameOutcome.LOST && state != State.FORFEITED) state = State.LOST;
    }

    private void finalizeScore(GameOutcome outcome) {
        boolean mowerUsed = false;
        for (int row = 0; row < simulation.getWorld().getRows(); row++) {
            mowerUsed |= simulation.getWorld().isLawnMowerUsed(row);
        }
        score.finalizeScore(outcome, simulation.getSunAmount(),
                simulation.getWorld().getPlantLossCount(), mowerUsed);
    }

    private PlantSelection selectionFor(User user) {
        user.applyDefaults();
        PlantSelection result = new PlantSelection();
        for (PlantDefinition definition : plants.findMandatory()) {
            if (user.getCollection().hasPlant(definition.getType())) {
                result.add(definition.getType());
                if (result.size() == 8) break;
            }
        }
        if (result.isEmpty()) {
            result.add(PlantType.PEASHOOTER);
        }
        return result;
    }

    private Result<String> failure(String message) {
        Result<String> result = new Result<>();
        result.appendToMessage(message);
        return result;
    }
}
