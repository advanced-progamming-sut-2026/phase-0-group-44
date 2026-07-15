package model.inGame.plant;

import model.GameEngine;
import model.inGame.zombie.Zombie;

public interface PlantBehavior {
    default void onPlant(Plant plant, GameEngine engine) {
    }

    default void tick(Plant plant, GameEngine engine, double deltaSeconds) {
    }

    default void onDamaged(Plant plant, GameEngine engine, Zombie attacker, int damageTaken) {
    }

    default void onArmorBroken(Plant plant, GameEngine engine) {
    }

    default void onDeath(Plant plant, GameEngine engine) {
    }

    default void onPlantFood(Plant plant, GameEngine engine) {
    }
}
