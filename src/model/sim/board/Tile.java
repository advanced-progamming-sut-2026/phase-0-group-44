package model.sim.board;

import model.enums.TerrainType;
import model.sim.adventure.GraveReward;

/**
 * One board cell: its terrain (which can change during play), any stateful
 * terrain health (gravestone, ice), a frozen entity it may hold, and the plants
 * placed on it (a support plus at most one plant stacked on it).
 */
public class Tile {

    /** Ice health while a tile is frozen. */
    public static final int ICE_HEALTH = 400;

    /** Gravestone health. */
    public static final int GRAVESTONE_HEALTH = 200;

    private final int row;
    private final int column;
    private TerrainType terrain;
    private int terrainHealth;
    private boolean frozen;
    private int iceHealth;
    private int projectileBlockerHealth;
    private String projectileBlockerKind = "";
    private GraveReward graveReward = GraveReward.NONE;

    private PlantInstance supportPlant;
    private PlantInstance stackedPlant;


    public Tile(int row, int column, TerrainType terrain) {
        this.row = row;
        this.column = column;
        setTerrain(terrain);
    }

    public int getRow() {
        return row;
    }

    public int getColumn() {
        return column;
    }

    public TerrainType getTerrain() {
        return terrain;
    }

    public final void setTerrain(TerrainType terrain) {
        this.terrain = terrain;

        if (terrain == TerrainType.GRAVESTONE || terrain == TerrainType.DARK_AGES_GRAVESTONE) {
            this.terrainHealth = Tile.GRAVESTONE_HEALTH;
        } else {
            this.terrainHealth = 0;
            this.graveReward = GraveReward.NONE;
        }
    }

    public int getTerrainHealth() {
        return terrainHealth;
    }

    public boolean isGravestone() {
        return terrain == TerrainType.GRAVESTONE || terrain == TerrainType.DARK_AGES_GRAVESTONE;
    }

    /** Damages a gravestone; when destroyed it becomes normal ground. */
    public void damageTerrain(int amount, TerrainType groundWhenDestroyed) {
        if (!isGravestone()) {
            return;
        }

        terrainHealth -= amount;

        if (terrainHealth <= 0) {
            setTerrain(groundWhenDestroyed);
        }
    }


    public GraveReward getGraveReward() {
        return graveReward;
    }

    public boolean isFrozen() {
        return frozen;
    }

    public int getIceHealth() {
        return iceHealth;
    }

    public void freeze() {
        this.frozen = true;
        this.iceHealth = ICE_HEALTH;
    }

    /** Applies melt damage to the ice; releases the tile when the ice is gone. */
    public void meltIce(int amount) {
        if (!frozen) {
            return;
        }

        iceHealth -= amount;

        if (iceHealth <= 0) {
            frozen = false;
            iceHealth = 0;
        }
    }


    public boolean hasProjectileBlocker() {
        return frozen || projectileBlockerHealth > 0;
    }

    public void setProjectileBlocker(String kind, int health) {
        projectileBlockerKind = kind == null ? "" : kind;
        projectileBlockerHealth = Math.max(0, health);
    }

    public boolean hasAnyPlant() {
        return supportPlant != null || stackedPlant != null;
    }


    public void clearPlants() {
        this.supportPlant = null;
        this.stackedPlant = null;
    }

    public PlantInstance getSupportPlant() { return supportPlant; }
    public PlantInstance getStackedPlant() { return stackedPlant; }
    public void setSupportPlant(PlantInstance plant) { this.supportPlant = plant; }
    public void setStackedPlant(PlantInstance plant) { this.stackedPlant = plant; }
}
