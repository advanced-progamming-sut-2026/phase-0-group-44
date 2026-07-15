package model.sim;

import model.sim.sun.Sun;
import model.sim.sun.SunProducer;
import model.enums.PlantType;
import model.enums.TerrainType;
import model.sim.board.Board;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * The mutable field the simulation operates on: board size, the running sun
 * balance, the suns currently in play, the sun producers, and the entities that
 * area effects can damage.
 *
 * <p>Board dimensions default to 5 rows by 9 columns. No level configuration
 * supplying other dimensions is available yet, so this default is a placeholder;
 * a level can override it once level configs exist.</p>
 */
public class SimulationWorld {

    public static final int DEFAULT_ROWS = 5;
    public static final int DEFAULT_COLUMNS = 9;

    private final int rows;
    private final int columns;

    private int sunBalance;
    private final List<Sun> suns = new ArrayList<>();
    private final List<SunProducer> producers = new ArrayList<>();
    private final List<Damageable> zombies = new ArrayList<>();
    private final List<Damageable> plants = new ArrayList<>();

    /** Maximum plant food a player can hold. */
    public static final int MAX_PLANT_FOOD = 3;

    private final Board board;
    private int plantFood;
    private int currentWave;
    private final boolean[] lawnMowerUsed;
    private final Map<PlantType, Integer> cooldownRemaining = new EnumMap<>(PlantType.class);
    private boolean cooldownsDisabled;

    public SimulationWorld() {
        this(DEFAULT_ROWS, DEFAULT_COLUMNS);
    }

    public SimulationWorld(int rows, int columns) {
        this.rows = rows;
        this.columns = columns;
        this.board = new Board(rows, columns, TerrainType.NORMAL_EGYPT);
        this.lawnMowerUsed = new boolean[rows];
    }

    public Board getBoard() {
        return board;
    }

    public int getPlantFood() {
        return plantFood;
    }

    /** Adds one plant food, capped at {@link #MAX_PLANT_FOOD}; returns true if stored. */
    public boolean addPlantFood() {
        if (plantFood >= MAX_PLANT_FOOD) {
            return false;
        }

        plantFood++;

        return true;
    }

    public boolean spendPlantFood() {
        if (plantFood <= 0) {
            return false;
        }

        plantFood--;

        return true;
    }

    public int getCurrentWave() {
        return currentWave;
    }

    public void setCurrentWave(int currentWave) {
        this.currentWave = currentWave;
    }

    public boolean isLawnMowerUsed(int row) {
        return row >= 0 && row < lawnMowerUsed.length && lawnMowerUsed[row];
    }

    public void useLawnMower(int row) {
        if (row >= 0 && row < lawnMowerUsed.length) {
            lawnMowerUsed[row] = true;
        }
    }

    public int getCooldownRemaining(PlantType type) {
        return cooldownRemaining.getOrDefault(type, 0);
    }

    public void startCooldown(PlantType type, int ticks) {
        cooldownRemaining.put(type, ticks);
    }

    public boolean isOnCooldown(PlantType type) {
        return !cooldownsDisabled && getCooldownRemaining(type) > 0;
    }

    public void tickCooldowns() {
        for (Map.Entry<PlantType, Integer> entry : cooldownRemaining.entrySet()) {
            if (entry.getValue() > 0) {
                entry.setValue(entry.getValue() - 1);
            }
        }
    }

    /** {@code cheat remove-cooldown}: lifts all cooldown restrictions for the session. */
    public void disableCooldowns() {
        cooldownsDisabled = true;
        cooldownRemaining.clear();
    }

    public boolean areCooldownsDisabled() {
        return cooldownsDisabled;
    }

    public int getRows() {
        return rows;
    }

    public int getColumns() {
        return columns;
    }

    public boolean isInsideBoard(int x, int y) {
        return x >= 0 && x < columns && y >= 0 && y < rows;
    }

    public int getSunBalance() {
        return sunBalance;
    }

    public void addSun(int amount) {
        sunBalance += amount;
    }

    public List<Sun> getSuns() {
        return suns;
    }

    public List<SunProducer> getProducers() {
        return producers;
    }

    public void addProducer(SunProducer producer) {
        producers.add(producer);
    }

    public List<Damageable> getZombies() {
        return zombies;
    }

    public void addZombie(Damageable zombie) {
        zombies.add(zombie);
    }

    public List<Damageable> getPlants() {
        return plants;
    }

    public void addPlant(Damageable plant) {
        plants.add(plant);
    }
}
