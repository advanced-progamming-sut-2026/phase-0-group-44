package model.sim;

import model.sim.sun.Sun;
import model.sim.sun.SunProducer;

import java.util.ArrayList;
import java.util.List;

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

    public SimulationWorld() {
        this(DEFAULT_ROWS, DEFAULT_COLUMNS);
    }

    public SimulationWorld(int rows, int columns) {
        this.rows = rows;
        this.columns = columns;
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
