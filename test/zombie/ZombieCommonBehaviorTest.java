package zombie;

import model.GameEngine;
import model.Position;
import model.enums.DamageType;
import model.enums.ObstacleType;
import model.enums.PlantType;
import model.enums.ZombieType;
import model.inGame.plant.Plant;
import model.inGame.projectile.NormalEffect;
import model.inGame.projectile.ProjectileFactory;
import model.inGame.zombie.Zombie;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ZombieCommonBehaviorTest {

    @Test
    void gargantuarOneHitsAndThrowsOneImpAtHalfBaseHealth() {
        GameEngine engine = new GameEngine();
        Plant plant = place(engine, PlantType.WALL_NUT, 0, 4);
        Zombie gargantuar = engine.spawnZombie(ZombieType.GARGANTUAR, 0, 4.5);

        engine.tick(0.1);
        assertTrue(plant.isDead() || !engine.getGameMap().getPlants().contains(plant));

        gargantuar.receiveDamage(gargantuar.getMaxHealth() / 2, DamageType.TRUE, engine);
        assertEquals(1, count(engine, ZombieType.IMP));
        Zombie imp = engine.getZombies().stream()
                .filter(zombie -> zombie.getType() == ZombieType.IMP)
                .findFirst().orElseThrow();
        assertEquals(2.5, imp.getX(), 0.0001);

        gargantuar.receiveDamage(1, DamageType.TRUE, engine);
        assertEquals(1, count(engine, ZombieType.IMP));
    }

    @Test
    void impMovesAndEatsFasterThanNormal() {
        GameEngine movementEngine = new GameEngine();
        Zombie normal = movementEngine.spawnZombie(ZombieType.NORMAL, 0, 8.0);
        Zombie imp = movementEngine.spawnZombie(ZombieType.IMP, 1, 8.0);
        movementEngine.tick(1.0);
        assertTrue(8.0 - imp.getX() > 8.0 - normal.getX());

        GameEngine normalAttackEngine = new GameEngine();
        Plant normalTarget = place(normalAttackEngine, PlantType.WALL_NUT, 0, 4);
        normalAttackEngine.spawnZombie(ZombieType.NORMAL, 0, 4.5);
        normalAttackEngine.tick(0.1);

        GameEngine impAttackEngine = new GameEngine();
        Plant impTarget = place(impAttackEngine, PlantType.WALL_NUT, 0, 4);
        impAttackEngine.spawnZombie(ZombieType.IMP, 0, 4.5);
        impAttackEngine.tick(0.1);
        assertTrue(impTarget.getHp() < normalTarget.getHp());
    }

    @Test
    void allStarDestroysFirstCollisionThenBecomesVerySlow() {
        GameEngine engine = new GameEngine();
        Plant plant = place(engine, PlantType.WALL_NUT, 0, 4);
        Zombie allStar = engine.spawnZombie(ZombieType.ALL_STAR, 0, 4.5);
        double chargeSpeed = allStar.getSpeedTilesPerSecond();

        engine.tick(0.1);

        assertTrue(plant.isDead() || !engine.getGameMap().getPlants().contains(plant));
        assertFalse(allStar.getBooleanState("CHARGING"));
        assertTrue(allStar.getSpeedTilesPerSecond() < chargeSpeed / 5.0);
    }

    @Test
    void parasolRejectsLobbedProjectiles() {
        GameEngine engine = new GameEngine();
        Plant cabbage = place(engine, PlantType.CABBAGE_PULT, 0, 0);
        Zombie parasol = engine.spawnZombie(ZombieType.PARASOL_ZOMBIE, 0, 4.5);
        int health = parasol.getHealth();

        engine.spawnProjectile(new ProjectileFactory().lobbed(
                cabbage, parasol, 40, new NormalEffect()));
        engine.tick(2.0);

        assertEquals(health, parasol.getHealth());
    }

    @Test
    void turquoiseStealsForFiveSecondsLasersAndDropsHalf() {
        GameEngine engine = new GameEngine();
        engine.setSun(500);
        Plant victim = place(engine, PlantType.PEASHOOTER, 0, 4);
        Zombie turquoise = engine.spawnZombie(ZombieType.TURQUOISE_ZOMBIE, 0, 8.0);

        engine.tick(5.0);

        assertEquals(375, engine.getSun());
        assertTrue(victim.isDead() || !engine.getGameMap().getPlants().contains(victim));

        turquoise.receiveDamage(Integer.MAX_VALUE, DamageType.TRUE, engine);
        engine.tick(0.1);
        assertEquals(437, engine.getSun());
    }

    @Test
    void prospectorReversesAfterTenSecondsUnlessIceExtinguishesDynamite() {
        GameEngine engine = new GameEngine();
        Zombie reversing = engine.spawnZombie(ZombieType.PROSPECTOR, 0, 8.0);
        engine.tick(10.0);
        assertEquals(1, reversing.getDirection());
        assertTrue(reversing.getBooleanState("REVERSED"));

        engine = new GameEngine();
        Zombie extinguished = engine.spawnZombie(ZombieType.PROSPECTOR, 0, 8.0);
        extinguished.onIceHit(engine);
        engine.tick(11.0);
        assertEquals(-1, extinguished.getDirection());
        assertFalse(extinguished.getBooleanState("REVERSED"));
    }

    @Test
    void pianistDestroysPlantsAndMovesOtherZombiesToAdjacentRows() {
        GameEngine engine = new GameEngine();
        Plant victim = place(engine, PlantType.WALL_NUT, 2, 4);
        Zombie pianist = engine.spawnZombie(ZombieType.PIANIST, 2, 4.5);
        Zombie other = engine.spawnZombie(ZombieType.NORMAL, 2, 8.0);

        engine.tick(4.0);

        assertTrue(victim.isDead() || !engine.getGameMap().getPlants().contains(victim));
        assertEquals(1, Math.abs(other.getRow() - 2));
        assertFalse(pianist.isDead());
    }

    @Test
    void newspaperRagesWhenItsPaperBreaks() {
        GameEngine engine = new GameEngine();
        Zombie newspaper = engine.spawnZombie(ZombieType.NEWSPAPER_ZOMBIE, 0, 8.0);
        double speed = newspaper.getSpeedTilesPerSecond();
        int eatDamage = newspaper.getEatDamagePerSecond();

        newspaper.receiveDamage(190, DamageType.NORMAL, engine);

        assertTrue(newspaper.getBooleanState("ENRAGED"));
        assertTrue(newspaper.getSpeedTilesPerSecond() > speed);
        assertTrue(newspaper.getEatDamagePerSecond() > eatDamage);
    }

    @Test
    void breakingBarrelReleasesTwoImpsAndPoisonDeathLeavesBlocker() {
        GameEngine engine = new GameEngine();
        Zombie barrel = engine.spawnZombie(ZombieType.BARREL_ROLLER, 0, 5.0);
        barrel.receiveDamage(1100, DamageType.NORMAL, engine);
        assertEquals(2, count(engine, ZombieType.IMP));

        engine = new GameEngine();
        barrel = engine.spawnZombie(ZombieType.BARREL_ROLLER, 0, 5.0);
        barrel.receiveDamage(Integer.MAX_VALUE, DamageType.POISON, engine);
        engine.tick(0.1);
        assertEquals(ObstacleType.BARREL,
                engine.getGameMap().getTile(new Position(0, 5)).getObstacle());
    }

    private Plant place(GameEngine engine, PlantType type, int row, int column) {
        Plant plant = engine.getPlantFactory().create(type, 1);
        engine.placePlantForFree(plant, new Position(row, column));
        return plant;
    }

    private long count(GameEngine engine, ZombieType type) {
        return engine.getZombies().stream()
                .filter(zombie -> zombie.getType() == type)
                .count();
    }
}
