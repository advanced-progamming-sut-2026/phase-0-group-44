package zombie;

import model.GameEngine;
import model.Position;
import model.enums.DamageType;
import model.enums.ObstacleType;
import model.enums.PlantType;
import model.enums.TerrainType;
import model.enums.ZombieType;
import model.inGame.plant.Plant;
import model.inGame.projectile.NormalEffect;
import model.inGame.projectile.ProjectileFactory;
import model.inGame.zombie.Zombie;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ZombieChapterBehaviorTest {

    @Test
    void raStealsGroundSunAndReturnsAllOnDeath() {
        GameEngine engine = new GameEngine();
        engine.addGroundSun(new Position(0, 4), 50);
        Zombie ra = engine.spawnZombie(ZombieType.RA_ZOMBIE, 0, 5.0);

        engine.tick(1.0);
        assertTrue(engine.getGroundSuns().isEmpty());

        ra.receiveDamage(Integer.MAX_VALUE, DamageType.TRUE, engine);
        engine.tick(0.1);
        assertEquals(100, engine.getSun());
    }

    @Test
    void explorerTorchCanBeExtinguishedAndRelit() {
        GameEngine engine = new GameEngine();
        Plant first = place(engine, PlantType.PEASHOOTER, 0, 4);
        Zombie explorer = engine.spawnZombie(ZombieType.EXPLORER, 0, 4.5);

        explorer.onIceHit(engine);
        engine.tick(0.1);
        assertFalse(first.isDead());

        explorer.onFireHit(engine);
        engine.tick(0.1);
        assertTrue(first.isDead() || !engine.getGameMap().getPlants().contains(first));
    }

    @Test
    void tombraiserCreatesTwoGravesOnRandomValidTiles() {
        GameEngine engine = new GameEngine();
        engine.spawnZombie(ZombieType.TOMBRAISER, 0, 8.0);

        engine.tick(8.0);

        int graves = 0;
        for (int row = 0; row < engine.getGameMap().getRows(); row++) {
            for (int column = 0; column < engine.getGameMap().getColumns(); column++) {
                if (engine.getGameMap().getTile(row, column).getObstacle() == ObstacleType.GRAVE) {
                    graves++;
                }
            }
        }
        assertEquals(2, graves);
    }

    @Test
    void dodoFliesOverDefendersAndTrapsButNotTallNut() {
        GameEngine engine = new GameEngine();
        Plant wallNut = place(engine, PlantType.WALL_NUT, 0, 4);
        Zombie dodo = engine.spawnZombie(ZombieType.DODO_RIDER, 0, 4.5);
        engine.tick(0.1);
        assertTrue(dodo.getX() < 3.6);
        assertEquals(wallNut.getMaxHp(), wallNut.getHp());

        engine = new GameEngine();
        Plant tallNut = place(engine, PlantType.TALL_NUT, 0, 4);
        engine.spawnZombie(ZombieType.DODO_RIDER, 0, 4.5);
        engine.tick(0.1);
        assertTrue(tallNut.getHp() < tallNut.getMaxHp());
    }

    @Test
    void hunterFreezesAfterThreeHitsAndFireMeltsTheIce() {
        GameEngine engine = new GameEngine();
        Plant target = place(engine, PlantType.PEASHOOTER, 0, 2);
        engine.spawnZombie(ZombieType.HUNTER, 0, 8.0);

        engine.tick(9.0);

        assertTrue(target.getBooleanState("FROZEN"));
        assertEquals(ObstacleType.ICE,
                engine.getGameMap().getTile(new Position(0, 2)).getObstacle());
        engine.damageObstacleAt(0, 2, 1, DamageType.FIRE);
        assertFalse(target.getBooleanState("FROZEN"));
    }

    @Test
    void snorkelRejectsDirectFireUnderwaterButAcceptsLobbers() {
        GameEngine engine = new GameEngine();
        engine.getGameMap().setTerrain(new Position(0, 4), TerrainType.WATER);
        Zombie snorkel = engine.spawnZombie(ZombieType.SNORKEL, 0, 4.5);
        Plant directSource = place(engine, PlantType.WALL_NUT, 0, 0);
        Plant lobberSource = place(engine, PlantType.TALL_NUT, 0, 1);
        int health = snorkel.getHealth();

        engine.tick(0.1);
        assertTrue(snorkel.getBooleanState("SUBMERGED"));
        engine.spawnProjectile(new ProjectileFactory().direct(
                directSource, 0, 1, 20, new NormalEffect(), 9.0));
        engine.tick(1.0);
        assertEquals(health, snorkel.getHealth());

        engine.spawnProjectile(new ProjectileFactory().lobbed(
                lobberSource, snorkel, 40, new NormalEffect()));
        engine.tick(2.0);
        assertEquals(health - 40, snorkel.getHealth());
    }

    @Test
    void octopusDisablesPlantAndCreatesDirectProjectileBlocker() {
        GameEngine engine = new GameEngine();
        Plant target = place(engine, PlantType.PEASHOOTER, 0, 2);
        engine.spawnZombie(ZombieType.OCTOPUS_ZOMBIE, 0, 8.0);

        engine.tick(5.0);

        assertTrue(target.getBooleanState("OCTOPUSED"));
        assertEquals(ObstacleType.OCTOPUS,
                engine.getGameMap().getTile(new Position(0, 2)).getObstacle());
        engine.damageObstacleAt(0, 2, Integer.MAX_VALUE, DamageType.NORMAL);
        assertFalse(target.getBooleanState("OCTOPUSED"));
    }

    private Plant place(GameEngine engine, PlantType type, int row, int column) {
        Plant plant = engine.getPlantFactory().create(type, 1);
        engine.placePlantForFree(plant, new Position(row, column));
        return plant;
    }
}
