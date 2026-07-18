package zombie;

import model.GameEngine;
import model.Position;
import model.enums.DamageType;
import model.enums.PlantType;
import model.enums.ZombieType;
import model.inGame.GameMap;
import model.inGame.plant.Plant;
import model.inGame.plant.PlantRegistry;
import model.inGame.zombie.Zombie;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BonusZombieBehaviorTest {

    @Test
    void arcadeAndTroglobiteCrushWhileTheirPushedArmorSurvives() {
        for (ZombieType type : new ZombieType[]{ZombieType.ARCADE_ZOMBIE, ZombieType.TROGLOBITE}) {
            GameEngine engine = engine();
            Plant plant = engine.plant(PlantType.WALL_NUT, 1, new Position(2, 2));
            engine.spawnZombie(type, 2, 2.5);
            engine.tick(0.1);
            assertTrue(plant.isDead() || !engine.getGameMap().getPlants().contains(plant), type.name());
        }
    }

    @Test
    void fishermanPullsPlantsAndDestroysAnAdjacentCatch() {
        GameEngine engine = engine();
        Plant plant = engine.plant(PlantType.PEASHOOTER, 1, new Position(2, 2));
        engine.spawnZombie(ZombieType.FISHERMAN, 2, 8.5);
        engine.tick(5.0);
        assertEquals(3, plant.getPosition().getColumn());

        engine.movePlant(plant, new Position(2, 7));
        engine.tick(5.0);
        assertTrue(plant.isDead() || !engine.getGameMap().getPlants().contains(plant));
    }

    @Test
    void jesterReflectsDirectProjectilesAndTemporarilySpeedsUp() {
        GameEngine engine = engine();
        Plant shooter = engine.plant(PlantType.PEASHOOTER, 1, new Position(2, 0));
        Zombie jester = engine.spawnZombie(ZombieType.JESTER, 2, 2.0);
        engine.tick(1.5);

        assertEquals(420, jester.getHealth());
        assertEquals(280, shooter.getHp());
        assertTrue(jester.getBooleanState("SPINNING"));
        double spinningSpeed = jester.getSpeedTilesPerSecond();
        shooter.expire(engine);
        engine.tick(2.0);
        assertFalse(jester.getBooleanState("SPINNING"));
        assertTrue(jester.getSpeedTilesPerSecond() < spinningSpeed);
    }

    @Test
    void wizardTransformsPlantsUntilThatWizardDies() {
        GameEngine engine = engine();
        Plant plant = engine.plant(PlantType.PEASHOOTER, 1, new Position(2, 1));
        Zombie wizard = engine.spawnZombie(ZombieType.WIZARD, 2, 8.0);
        engine.tick(5.0);
        assertTrue(plant.getBooleanState("TRANSFORMED"));

        wizard.receiveDamage(Integer.MAX_VALUE, DamageType.TRUE, engine);
        engine.tick(0.1);
        assertFalse(plant.getBooleanState("TRANSFORMED"));
    }

    @Test
    void kingPromotesNearbyNormalZombieWithKnightArmor() {
        GameEngine engine = engine();
        engine.spawnZombie(ZombieType.KING, 2, 8.0);
        Zombie normal = engine.spawnZombie(ZombieType.NORMAL, 2, 6.0);
        engine.tick(5.0);

        assertTrue(normal.getBooleanState("KING_PROMOTED"));
        assertEquals(3200, normal.getArmor());
        assertTrue(normal.hasMetalArmor());
    }

    private GameEngine engine() {
        GameEngine engine = new GameEngine(
                PlantRegistry.getDefault(), new GameMap(), new Random(0));
        engine.setSun(20_000);
        return engine;
    }
}
