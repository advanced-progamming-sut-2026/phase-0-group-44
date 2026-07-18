package model.miniGame;

import model.Result;
import model.enums.PlantType;
import model.enums.ZombieType;
import model.inGame.plant.PlantDefinition;
import model.inGame.plant.PlantRegistry;
import model.inGame.zombie.ZombieDefinition;
import model.inGame.zombie.ZombieEffectType;
import model.inGame.zombie.ZombieRegistry;
import model.sim.GameOutcome;
import model.sim.SimulationSystem;
import model.sim.SimulationWorld;
import model.sim.TickContext;
import model.sim.board.DefaultPlantSpecSource;
import model.sim.board.PlantInstance;
import model.sim.board.PlantSpec;
import model.sim.board.Tile;
import model.sim.zombie.ZombieInstance;
import model.sim.zombie.ZombieSpec;
import util.RandomSource;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Attempt-local Beghouled match board, upgrades, endless spawning, and craters. */
public final class BeghouledState {
    public static final int SUN_UNIT = 50;

    private final BeghouledLevelRules rules;
    private final RandomSource random;
    private final PlantRegistry plantRegistry = PlantRegistry.getDefault();
    private final ZombieRegistry zombieRegistry = ZombieRegistry.getDefault();
    private final DefaultPlantSpecSource plantSpecs = new DefaultPlantSpecSource(plantRegistry);
    private final Map<PlantType, BeghouledUpgrade> upgrades;
    private final Map<PlantType, PlantType> purchasedConversions = new LinkedHashMap<>();
    private final Map<String, Long> nextPlantAttackTick = new HashMap<>();
    private final PlantType[][] grid;
    private final int[][] health;
    private final boolean[][] craters;
    private int completedMatches;
    private int resetCount;
    private int spawnIndex;
    private long nextZombieSpawnTick = 1;
    private boolean cleanupPhase;

    public BeghouledState(
            BeghouledLevelRules rules,
            RandomSource random,
            SimulationWorld world,
            Map<PlantType, BeghouledUpgrade> upgrades
    ) {
        if (rules == null || random == null || world == null || upgrades == null) {
            throw new IllegalArgumentException("Beghouled state dependencies are required.");
        }
        this.rules = rules;
        this.random = random;
        this.upgrades = Map.copyOf(upgrades);
        this.grid = new PlantType[world.getRows()][world.getColumns()];
        this.health = new int[world.getRows()][world.getColumns()];
        this.craters = new boolean[world.getRows()][world.getColumns()];
        resetBoard(world, false);
    }

    public static int rewardUnits(int matchSize, boolean cascade) {
        if (matchSize < 3) return 0;
        int normal = Math.min(3, matchSize - 2);
        return normal + (cascade ? 1 : 0);
    }

    public BeghouledLevelRules getRules() { return rules; }
    public int getCompletedMatches() { return completedMatches; }
    public int getResetCount() { return resetCount; }
    public boolean isCleanupPhase() { return cleanupPhase; }
    public boolean isCrater(int x, int y) {
        return inside(x, y) && craters[y][x];
    }
    public PlantType getPlantAt(int x, int y) {
        return inside(x, y) ? grid[y][x] : null;
    }
    public Map<PlantType, PlantType> getPurchasedConversions() {
        return Collections.unmodifiableMap(purchasedConversions);
    }

    public Result<String> swap(
            MiniGameSession session,
            int firstX,
            int firstY,
            int secondX,
            int secondY
    ) {
        Result<String> result = new Result<>();
        if (!inside(firstX, firstY) || !inside(secondX, secondY)) {
            result.appendToMessage("invalid tile");
            return result;
        }
        if (Math.abs(firstX - secondX) + Math.abs(firstY - secondY) != 1) {
            result.appendToMessage("plants must be orthogonally adjacent");
            return result;
        }
        if (craters[firstY][firstX] || craters[secondY][secondX]
                || grid[firstY][firstX] == null || grid[secondY][secondX] == null) {
            result.appendToMessage("a crater or empty tile cannot be swapped");
            return result;
        }

        swapCells(firstX, firstY, secondX, secondY);
        if (!hasMatchAt(firstX, firstY) && !hasMatchAt(secondX, secondY)) {
            swapCells(firstX, firstY, secondX, secondY);
            result.appendToMessage("swap rejected because it creates no size-3 match");
            return result;
        }

        SimulationWorld world = session.getSimulation().getWorld();
        syncWorld(world);
        Resolution resolution = resolveMatches(world, false);
        if (!hasLegalMove()) {
            resetBoard(world, true);
            resolution = resolution.withReset();
        }
        result.setStatus(true);
        result.setData(String.valueOf(resolution.matches()));
        result.appendToMessage("swap accepted; resolved " + resolution.matches()
                + " match(es), gained " + resolution.sunGained() + " sun"
                + (resolution.reset() ? "; board reset because no legal move remained" : ""));
        return result;
    }

    public Result<String> upgrade(MiniGameSession session, String token) {
        Result<String> result = new Result<>();
        PlantType source = parsePlant(token);
        BeghouledUpgrade upgrade = source == null ? null : upgrades.get(source);
        if (upgrade == null) {
            result.appendToMessage("no Beghouled upgrade begins with that plant");
            return result;
        }
        boolean present = false;
        for (PlantType[] row : grid) {
            for (PlantType type : row) {
                present |= type == source;
            }
        }
        if (!present) {
            result.appendToMessage(source.name() + " is not present on the board");
            return result;
        }
        SimulationWorld world = session.getSimulation().getWorld();
        if (world.getSunBalance() < upgrade.cost()) {
            result.appendToMessage("not enough sun; upgrade costs " + upgrade.cost());
            return result;
        }
        world.addSun(-upgrade.cost());
        purchasedConversions.put(upgrade.from(), upgrade.to());
        PlantSpec targetSpec = requireSpec(upgrade.to());
        for (int y = 0; y < grid.length; y++) {
            for (int x = 0; x < grid[y].length; x++) {
                if (grid[y][x] == upgrade.from()) {
                    grid[y][x] = upgrade.to();
                    health[y][x] = Math.min(health[y][x], targetSpec.getHp());
                }
            }
        }
        syncWorld(world);
        Resolution resolution = resolveMatches(world, false);
        if (!hasLegalMove()) {
            resetBoard(world, true);
        }
        result.setStatus(true);
        result.setData(upgrade.to().name());
        result.appendToMessage("upgraded " + upgrade.from().name() + " to "
                + upgrade.to().name() + " for " + upgrade.cost() + " sun"
                + (resolution.matches() > 0
                ? "; conversion resolved " + resolution.matches() + " match(es)" : ""));
        return result;
    }

    public SimulationSystem preCombatSystem() {
        return context -> {
            if (!context.getWorld().isRunning()) return;
            long completedTick = context.getCurrentTick() + 1;
            if (!cleanupPhase && completedTick >= nextZombieSpawnTick) {
                spawnZombie(context);
                nextZombieSpawnTick = completedTick + rules.getZombieSpawnIntervalTicks();
            }
            attackWithPlants(context);
            if (!cleanupPhase && completedMatches >= rules.getTargetMatches()) {
                cleanupPhase = true;
                context.emit("Beghouled match target reached; zombie spawning stopped.");
            }
        };
    }

    /** Runs after shared zombie combat so eaten plants become permanent craters. */
    public SimulationSystem postCombatSystem() {
        return context -> {
            SimulationWorld world = context.getWorld();
            for (int y = 0; y < grid.length; y++) {
                for (int x = 0; x < grid[y].length; x++) {
                    if (craters[y][x] || grid[y][x] == null) continue;
                    Tile tile = world.getBoard().tileAt(x, y);
                    PlantInstance plant = tile.getStackedPlant() != null
                            ? tile.getStackedPlant() : tile.getSupportPlant();
                    if (plant == null) {
                        craters[y][x] = true;
                        grid[y][x] = null;
                        health[y][x] = 0;
                        nextPlantAttackTick.remove(key(x, y));
                        context.emit("A crater formed at (" + x + ", " + y + ").");
                    } else {
                        health[y][x] = plant.getHp();
                    }
                }
            }
        };
    }

    public GameOutcome evaluate(SimulationWorld world) {
        if (world.getOutcome() == GameOutcome.LOST) return GameOutcome.LOST;
        if (cleanupPhase && world.getZombieInstances().isEmpty()) {
            world.setOutcome(GameOutcome.WON);
            return GameOutcome.WON;
        }
        return GameOutcome.RUNNING;
    }

    public String status(MiniGameSession session) {
        StringBuilder output = new StringBuilder();
        output.append("Beghouled level ").append(rules.getLevel()).append('\n')
                .append("matches: ").append(completedMatches).append('/')
                .append(rules.getTargetMatches()).append('\n')
                .append("sun: ").append(session.getSimulation().getWorld().getSunBalance())
                .append("; phase: ").append(cleanupPhase ? "cleanup" : "matching")
                .append("; resets: ").append(resetCount).append('\n');
        for (int y = 0; y < grid.length; y++) {
            output.append("row ").append(y).append(": ");
            for (int x = 0; x < grid[y].length; x++) {
                if (craters[y][x]) output.append("[CRATER]");
                else output.append('[').append(grid[y][x] == null ? "EMPTY" : grid[y][x].name()).append(']');
                if (x + 1 < grid[y].length) output.append(' ');
            }
            output.append('\n');
        }
        output.append("upgrades:");
        for (BeghouledUpgrade upgrade : upgrades.values()) {
            output.append("\n- ").append(upgrade.from()).append(" -> ")
                    .append(upgrade.to()).append(": ").append(upgrade.cost());
        }
        return output.toString();
    }

    public boolean hasLegalMove() {
        for (int y = 0; y < grid.length; y++) {
            for (int x = 0; x < grid[y].length; x++) {
                if (craters[y][x] || grid[y][x] == null) continue;
                if (x + 1 < grid[y].length && !craters[y][x + 1]
                        && grid[y][x + 1] != null && legalTemporarySwap(x, y, x + 1, y)) {
                    return true;
                }
                if (y + 1 < grid.length && !craters[y + 1][x]
                        && grid[y + 1][x] != null && legalTemporarySwap(x, y, x, y + 1)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean legalTemporarySwap(int x1, int y1, int x2, int y2) {
        swapCells(x1, y1, x2, y2);
        boolean legal = hasMatchAt(x1, y1) || hasMatchAt(x2, y2);
        swapCells(x1, y1, x2, y2);
        return legal;
    }

    private Resolution resolveMatches(SimulationWorld world, boolean initialCascade) {
        int totalMatches = 0;
        int totalSun = 0;
        boolean cascade = initialCascade;
        for (int guard = 0; guard < 100; guard++) {
            List<Match> matches = findMatches();
            if (matches.isEmpty()) break;
            Set<Cell> removed = new LinkedHashSet<>();
            int units = 0;
            for (Match match : matches) {
                removed.addAll(match.cells());
                units += rewardUnits(match.cells().size(), cascade);
            }
            for (Cell cell : removed) {
                grid[cell.y()][cell.x()] = null;
                health[cell.y()][cell.x()] = 0;
            }
            completedMatches += matches.size();
            totalMatches += matches.size();
            int gained = units * SUN_UNIT;
            world.addSun(gained);
            world.recordProducedSun(gained);
            totalSun += gained;
            collapseAndRefill();
            syncWorld(world);
            cascade = true;
        }
        return new Resolution(totalMatches, totalSun, false);
    }

    private List<Match> findMatches() {
        List<Match> result = new ArrayList<>();
        for (int y = 0; y < grid.length; y++) {
            int start = 0;
            while (start < grid[y].length) {
                PlantType type = grid[y][start];
                int end = start + 1;
                while (type != null && end < grid[y].length && grid[y][end] == type) end++;
                if (type != null && end - start >= 3) {
                    List<Cell> cells = new ArrayList<>();
                    for (int x = start; x < end; x++) cells.add(new Cell(x, y));
                    result.add(new Match(List.copyOf(cells)));
                }
                start = type == null ? start + 1 : end;
            }
        }
        for (int x = 0; x < grid[0].length; x++) {
            int start = 0;
            while (start < grid.length) {
                PlantType type = grid[start][x];
                int end = start + 1;
                while (type != null && end < grid.length && grid[end][x] == type) end++;
                if (type != null && end - start >= 3) {
                    List<Cell> cells = new ArrayList<>();
                    for (int y = start; y < end; y++) cells.add(new Cell(x, y));
                    result.add(new Match(List.copyOf(cells)));
                }
                start = type == null ? start + 1 : end;
            }
        }
        return result;
    }

    private boolean hasMatchAt(int x, int y) {
        PlantType type = grid[y][x];
        if (type == null) return false;
        int horizontal = 1;
        for (int i = x - 1; i >= 0 && grid[y][i] == type; i--) horizontal++;
        for (int i = x + 1; i < grid[y].length && grid[y][i] == type; i++) horizontal++;
        if (horizontal >= 3) return true;
        int vertical = 1;
        for (int i = y - 1; i >= 0 && grid[i][x] == type; i--) vertical++;
        for (int i = y + 1; i < grid.length && grid[i][x] == type; i++) vertical++;
        return vertical >= 3;
    }

    private void collapseAndRefill() {
        for (int x = 0; x < grid[0].length; x++) {
            List<PlantSnapshot> survivors = new ArrayList<>();
            for (int y = grid.length - 1; y >= 0; y--) {
                if (!craters[y][x] && grid[y][x] != null) {
                    survivors.add(new PlantSnapshot(grid[y][x], health[y][x]));
                }
                if (!craters[y][x]) {
                    grid[y][x] = null;
                    health[y][x] = 0;
                }
            }
            int survivor = 0;
            for (int y = grid.length - 1; y >= 0; y--) {
                if (craters[y][x]) continue;
                if (survivor < survivors.size()) {
                    PlantSnapshot snapshot = survivors.get(survivor++);
                    grid[y][x] = snapshot.type();
                    health[y][x] = snapshot.health();
                } else {
                    PlantType type = randomPlant();
                    grid[y][x] = type;
                    health[y][x] = requireSpec(type).getHp();
                }
            }
        }
    }

    private void resetBoard(SimulationWorld world, boolean countReset) {
        if (countReset) resetCount++;
        for (int attempt = 0; attempt < 100; attempt++) {
            fillWithoutImmediateMatches(attempt);
            forceLegalPattern();
            if (findMatches().isEmpty() && hasLegalMove()) {
                syncWorld(world);
                return;
            }
        }
        throw new IllegalStateException("Could not generate a playable Beghouled board.");
    }

    private void fillWithoutImmediateMatches(int attempt) {
        for (int y = 0; y < grid.length; y++) {
            for (int x = 0; x < grid[y].length; x++) {
                if (craters[y][x]) {
                    grid[y][x] = null;
                    health[y][x] = 0;
                    continue;
                }
                int size = rules.getPlantPool().size();
                int base = Math.floorMod(random.nextInt(size) + x + 2 * y + attempt, size);
                PlantType chosen = null;
                for (int offset = 0; offset < size; offset++) {
                    PlantType candidate = resolveUpgrade(rules.getPlantPool().get((base + offset) % size));
                    boolean horizontal = x >= 2 && grid[y][x - 1] == candidate
                            && grid[y][x - 2] == candidate;
                    boolean vertical = y >= 2 && grid[y - 1][x] == candidate
                            && grid[y - 2][x] == candidate;
                    if (!horizontal && !vertical) {
                        chosen = candidate;
                        break;
                    }
                }
                if (chosen == null) chosen = resolveUpgrade(rules.getPlantPool().get(base));
                grid[y][x] = chosen;
                health[y][x] = requireSpec(chosen).getHp();
            }
        }
    }

    /** Installs A-B-A / C-A-C, where one adjacent swap creates A-A-A. */
    private void forceLegalPattern() {
        if (grid.length < 2 || grid[0].length < 3) return;
        for (int y = 0; y < 2; y++) {
            for (int x = 0; x < 3; x++) {
                if (craters[y][x]) return;
            }
        }
        PlantType a = resolveUpgrade(rules.getPlantPool().get(0));
        PlantType b = resolveUpgrade(rules.getPlantPool().get(1));
        PlantType c = resolveUpgrade(rules.getPlantPool().get(2));
        setFresh(0, 0, a);
        setFresh(1, 0, b);
        setFresh(2, 0, a);
        setFresh(0, 1, c);
        setFresh(1, 1, a);
        setFresh(2, 1, c);
    }

    private void setFresh(int x, int y, PlantType type) {
        grid[y][x] = type;
        health[y][x] = requireSpec(type).getHp();
    }

    private void syncWorld(SimulationWorld world) {
        for (int y = 0; y < grid.length; y++) {
            for (int x = 0; x < grid[y].length; x++) {
                world.getBoard().tileAt(x, y).clearPlants();
            }
        }
        world.getPlants().clear();
        for (int y = 0; y < grid.length; y++) {
            for (int x = 0; x < grid[y].length; x++) {
                PlantType type = grid[y][x];
                if (type == null || craters[y][x]) continue;
                PlantSpec spec = requireSpec(type);
                PlantInstance plant = new PlantInstance(spec, x, y);
                int damage = Math.max(0, spec.getHp() - Math.max(1, health[y][x]));
                if (damage > 0) plant.takeDamage(damage);
                world.getBoard().tileAt(x, y).setStackedPlant(plant);
                world.addPlant(plant);
                nextPlantAttackTick.putIfAbsent(key(x, y), 1L);
            }
        }
    }

    private void attackWithPlants(TickContext context) {
        SimulationWorld world = context.getWorld();
        long completedTick = context.getCurrentTick() + 1;
        for (int y = 0; y < grid.length; y++) {
            for (int x = 0; x < grid[y].length; x++) {
                if (grid[y][x] == null || craters[y][x]) continue;
                Tile tile = world.getBoard().tileAt(x, y);
                PlantInstance plant = tile.getStackedPlant();
                if (plant == null || !plant.isActive()) continue;
                String key = key(x, y);
                long due = nextPlantAttackTick.getOrDefault(key, 1L);
                if (completedTick < due) continue;
                PlantDefinition definition = plantRegistry.requireMandatory(plant.getType());
                int interval = Math.max(1, (int) Math.round(
                        definition.getActionInterval() * TickContext.TICKS_PER_SECOND));
                nextPlantAttackTick.put(key, completedTick + interval);
                int damage = definition.getDamage()
                        * Math.max(1, definition.getDamageProfile().getProjectileCount());
                if (damage <= 0) continue;
                ZombieInstance target = nearestTarget(world, x, y);
                if (target == null) continue;
                target.takeDamage(damage);
                if (plant.getType() == PlantType.SNOW_PEA
                        || plant.getType() == PlantType.WINTER_MELON) {
                    target.applyEffect(ZombieEffectType.CHILLED, 2.0, 1);
                    target.onIceHit();
                }
                context.emit(plant.getType() + " hit " + target.getSpec().getName()
                        + " for " + damage + ".");
            }
        }
    }

    private ZombieInstance nearestTarget(SimulationWorld world, int x, int row) {
        ZombieInstance nearest = null;
        for (ZombieInstance zombie : world.getZombieInstances()) {
            if (zombie.isDead() || zombie.getRow() != row || zombie.getX() < x) continue;
            if (nearest == null || zombie.getX() < nearest.getX()) nearest = zombie;
        }
        return nearest;
    }

    private void spawnZombie(TickContext context) {
        ZombieType type = rules.getZombieSequence().get(
                spawnIndex % rules.getZombieSequence().size());
        int row = spawnIndex % context.getWorld().getRows();
        spawnIndex++;
        ZombieDefinition definition = zombieRegistry.requireMandatory(type);
        ZombieInstance zombie = new ZombieInstance(
                ZombieSpec.fromDefinition(definition),
                context.getWorld().getColumns() - 0.01,
                row);
        context.getWorld().addZombie(zombie);
        context.emit("Beghouled spawned " + definition.getName() + " in row " + row + ".");
    }

    private PlantType randomPlant() {
        PlantType base = rules.getPlantPool().get(random.nextInt(rules.getPlantPool().size()));
        return resolveUpgrade(base);
    }

    private PlantType resolveUpgrade(PlantType type) {
        PlantType current = type;
        Set<PlantType> seen = new LinkedHashSet<>();
        while (purchasedConversions.containsKey(current) && seen.add(current)) {
            current = purchasedConversions.get(current);
        }
        return current;
    }

    private PlantSpec requireSpec(PlantType type) {
        PlantSpec spec = plantSpecs.specOf(type);
        if (spec == null) {
            throw new IllegalStateException("Missing canonical plant data for " + type);
        }
        return spec;
    }

    private PlantType parsePlant(String token) {
        try {
            return PlantType.fromCanonicalName(token);
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private void swapCells(int x1, int y1, int x2, int y2) {
        PlantType type = grid[y1][x1];
        grid[y1][x1] = grid[y2][x2];
        grid[y2][x2] = type;
        int hp = health[y1][x1];
        health[y1][x1] = health[y2][x2];
        health[y2][x2] = hp;
    }

    private boolean inside(int x, int y) {
        return y >= 0 && y < grid.length && x >= 0 && x < grid[y].length;
    }

    private String key(int x, int y) { return x + ":" + y; }

    private record Cell(int x, int y) { }
    private record Match(List<Cell> cells) { }
    private record PlantSnapshot(PlantType type, int health) { }
    private record Resolution(int matches, int sunGained, boolean reset) {
        private Resolution withReset() { return new Resolution(matches, sunGained, true); }
    }
}
