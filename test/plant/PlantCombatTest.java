package plant;

import model.GameEngine;
import model.Position;
import model.enums.ObstacleType;
import model.enums.PlantType;
import model.inGame.GameMap;
import model.inGame.plant.Plant;
import model.inGame.plant.PlantRegistry;
import model.inGame.projectile.PoisonEffect;
import model.inGame.projectile.ProjectileFactory;
import model.inGame.zombie.Zombie;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlantCombatTest {
    private GameEngine engine() {
        GameEngine engine = new GameEngine(PlantRegistry.getDefault(), new GameMap(), new Random(2));
        engine.setSun(20_000);
        return engine;
    }

    @Test
    void directProjectileHitsOnlyFirstZombieInLane() {
        GameEngine engine = engine();
        engine.plant(PlantType.PEASHOOTER, 1, new Position(2, 1));
        Zombie first = new Zombie("first", 2, 4.0, 100);
        Zombie second = new Zombie("second", 2, 6.0, 100);
        engine.addZombie(first);
        engine.addZombie(second);

        engine.tick(1.5);
        engine.tick(1.0);

        assertEquals(80, first.getHealth());
        assertEquals(100, second.getHealth());
    }

    @Test
    void lobbedProjectileIgnoresBlockingGrave() {
        GameEngine engine = engine();
        engine.getGameMap().setObstacle(new Position(2, 3), ObstacleType.GRAVE);
        engine.plant(PlantType.CABBAGE_PULT, 1, new Position(2, 1));
        Zombie zombie = new Zombie("target", 2, 5.0, 100);
        engine.addZombie(zombie);

        engine.tick(2.9);
        engine.tick(1.0);

        assertEquals(60, zombie.getHealth());
    }

    @Test
    void piercingProjectileDamagesMultipleAlignedZombies() {
        GameEngine engine = engine();
        engine.plant(PlantType.CACTUS, 1, new Position(1, 1));
        Zombie first = new Zombie("a", 1, 3.0, 100);
        Zombie second = new Zombie("b", 1, 4.0, 100);
        Zombie third = new Zombie("c", 1, 5.0, 100);
        Zombie fourth = new Zombie("d", 1, 6.0, 100);
        engine.addZombie(first);
        engine.addZombie(second);
        engine.addZombie(third);
        engine.addZombie(fourth);

        engine.tick(1.5);
        engine.tick(1.0);

        assertEquals(70, first.getHealth());
        assertEquals(70, second.getHealth());
        assertEquals(70, third.getHealth());
        assertEquals(100, fourth.getHealth());
    }

    @Test
    void fireThawsAndIceSlows() {
        GameEngine fireEngine = engine();
        fireEngine.plant(PlantType.FIRE_PEASHOOTER, 1, new Position(2, 1));
        Zombie frozen = new Zombie("frozen", 2, 4.0, 100);
        frozen.applyFreeze(10.0);
        fireEngine.addZombie(frozen);
        fireEngine.tick(1.5);
        fireEngine.tick(1.0);
        assertFalse(frozen.isFrozen());
        assertEquals(60, frozen.getHealth());

        GameEngine iceEngine = engine();
        iceEngine.plant(PlantType.SNOW_PEA, 1, new Position(2, 1));
        Zombie normal = new Zombie("normal", 2, 4.0, 100);
        iceEngine.addZombie(normal);
        iceEngine.tick(1.5);
        iceEngine.tick(1.0);
        assertTrue(normal.isSlowed());
    }

    @Test
    void poisonBypassesArmorAndDamagesBaseHealth() {
        GameEngine engine = engine();
        Plant source = engine.plant(PlantType.PEASHOOTER, 1, new Position(2, 1));
        Zombie armored = new Zombie("armored", 2, 4.0, 100, 100, true);
        engine.addZombie(armored);
        engine.spawnProjectile(new ProjectileFactory().direct(source, 2, 1, 20,
                new PoisonEffect(5, 2), 20));
        engine.tick(1.0);

        assertEquals(80, armored.getHealth());
        assertEquals(100, armored.getArmor());
    }

    @Test
    void armedPotatoMineTriggersOnContact() {
        GameEngine engine = engine();
        engine.plant(PlantType.POTATO_MINE, 1, new Position(2, 3));
        Zombie zombie = new Zombie("mine target", 2, 3.0, 2000);
        engine.addZombie(zombie);

        engine.tick(14.0);
        assertEquals(2000, zombie.getHealth());
        engine.tick(1.0);
        assertEquals(200, zombie.getHealth());
    }
}
