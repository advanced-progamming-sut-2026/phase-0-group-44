package model;

import model.enums.ObstacleType;
import model.enums.TerrainType;
import model.enums.PlantType;
import model.inGame.plant.Plant;

import java.util.ArrayList;
import java.util.List;

public class Tile {
    private final Position position;
    private TerrainType terrain;
    private ObstacleType obstacle = ObstacleType.NONE;
    private int obstacleHealth;
    private String obstaclePayload = "";
    private Plant supportPlant;
    private Plant primaryPlant;
    private Plant armorPlant;

    public Tile(Position position, TerrainType terrain) {
        this.position = position;
        this.terrain = terrain;
    }

    public Position getPosition() {
        return position;
    }

    public TerrainType getTerrain() {
        return terrain;
    }

    public void setTerrain(TerrainType terrain) {
        this.terrain = terrain;
    }

    public ObstacleType getObstacle() {
        return obstacle;
    }

    public void setObstacle(ObstacleType obstacle) {
        setObstacle(obstacle, defaultObstacleHealth(obstacle), "");
    }

    public void setObstacle(ObstacleType obstacle, int health, String payload) {
        this.obstacle = obstacle == null ? ObstacleType.NONE : obstacle;
        this.obstacleHealth = this.obstacle == ObstacleType.NONE ? 0 : Math.max(0, health);
        this.obstaclePayload = payload == null ? "" : payload;
    }

    private int defaultObstacleHealth(ObstacleType obstacle) {
        if (obstacle == ObstacleType.GRAVE) {
            return 700;
        }
        if (obstacle == ObstacleType.ICE || obstacle == ObstacleType.OCTOPUS) {
            return 600;
        }
        return obstacle == ObstacleType.NONE ? 0 : 1100;
    }

    public int getObstacleHealth() {
        return obstacleHealth;
    }

    public String getObstaclePayload() {
        return obstaclePayload;
    }

    public int damageObstacle(int amount) {
        if (amount <= 0 || obstacle == ObstacleType.NONE) {
            return 0;
        }
        int dealt = Math.min(obstacleHealth, amount);
        obstacleHealth -= dealt;
        return dealt;
    }

    public boolean isObstacleDestroyed() {
        return obstacle != ObstacleType.NONE && obstacleHealth <= 0;
    }

    public boolean blocksPlanting() {
        return obstacle == ObstacleType.GRAVE || obstacle == ObstacleType.CRATER
                || obstacle == ObstacleType.PROJECTILE_BLOCKER
                || obstacle == ObstacleType.BARREL;
    }

    public boolean blocksDirectProjectiles() {
        return obstacle == ObstacleType.GRAVE || obstacle == ObstacleType.PROJECTILE_BLOCKER
                || obstacle == ObstacleType.ICE || obstacle == ObstacleType.BARREL
                || obstacle == ObstacleType.OCTOPUS;
    }

    public Plant getSupportPlant() {
        return supportPlant;
    }

    public Plant getPrimaryPlant() {
        return primaryPlant;
    }

    public Plant getArmorPlant() {
        return armorPlant;
    }

    public List<Plant> getPlants() {
        List<Plant> result = new ArrayList<>(3);
        if (supportPlant != null) {
            result.add(supportPlant);
        }
        if (primaryPlant != null) {
            result.add(primaryPlant);
        }
        if (armorPlant != null) {
            result.add(armorPlant);
        }
        return result;
    }

    public void placeSupport(Plant plant) {
        supportPlant = plant;
    }

    public void placePrimary(Plant plant) {
        primaryPlant = plant;
    }

    public void placeArmor(Plant plant) {
        armorPlant = plant;
    }

    public void remove(Plant plant) {
        if (supportPlant == plant) {
            supportPlant = null;
        }
        if (primaryPlant == plant) {
            primaryPlant = null;
        }
        if (armorPlant == plant) {
            armorPlant = null;
        }
    }

    public boolean hasAnyPlant() {
        return supportPlant != null || primaryPlant != null || armorPlant != null;
    }

    public boolean hasLilyPad() {
        return supportPlant != null && supportPlant.getType() == PlantType.LILY_PAD;
    }
}
