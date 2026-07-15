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
    public static final int ICE_HEALTH = 600;

    /** Gravestone health. */
    public static final int GRAVESTONE_HEALTH = 700;

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

    public void setGraveReward(GraveReward graveReward) {
        if (!isGravestone()) {
            throw new IllegalStateException("Only graves may contain rewards.");
        }
        this.graveReward = graveReward == null ? GraveReward.NONE : graveReward;
    }

    public GraveReward consumeGraveReward() {
        GraveReward result = graveReward;
        graveReward = GraveReward.NONE;
        return result;
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

    public int getProjectileBlockerHealth() {
        return frozen ? iceHealth : projectileBlockerHealth;
    }

    public String getProjectileBlockerKind() {
        return frozen ? "ice" : projectileBlockerKind;
    }

    public void setProjectileBlocker(String kind, int health) {
        projectileBlockerKind = kind == null ? "" : kind;
        projectileBlockerHealth = Math.max(0, health);
    }

    public int damageProjectileBlocker(int damage) {
        if (frozen) {
            int before = iceHealth;
            meltIce(damage);
            return Math.min(before, Math.max(0, damage));
        }
        int dealt = Math.min(projectileBlockerHealth, Math.max(0, damage));
        projectileBlockerHealth -= dealt;
        if (projectileBlockerHealth <= 0) {
            projectileBlockerHealth = 0;
            projectileBlockerKind = "";
        }
        return dealt;
    }

    public PlantInstance getSupportPlant() {
        return supportPlant;
    }

    public PlantInstance getStackedPlant() {
        return stackedPlant;
    }

    public boolean hasAnyPlant() {
        return supportPlant != null || stackedPlant != null;
    }

    public void setSupportPlant(PlantInstance plant) {
        this.supportPlant = plant;
    }

    public void setStackedPlant(PlantInstance plant) {
        this.stackedPlant = plant;
    }

    public void clearPlants() {
        this.supportPlant = null;
        this.stackedPlant = null;
    }
}
