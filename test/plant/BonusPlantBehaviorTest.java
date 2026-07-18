package plant;

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

class BonusPlantBehaviorTest {

    @Test
    void peaPodStacksToFiveAndFiresOncePerHead() {
        GameEngine engine = engine();
        Position position = new Position(2, 0);
        Plant peaPod = engine.plant(PlantType.PEA_POD, 1, position);
        engine.tick(5.0);
        Plant stacked = engine.plant(PlantType.PEA_POD, 1, position);

        assertTrue(peaPod == stacked);
        assertEquals(2, peaPod.getState("PEA_POD_HEADS", Integer.class, 0));
        assertEquals(1, engine.getGameMap().getPlants().size());

        Zombie zombie = engine.spawnZombie(ZombieType.NORMAL, 2, 3.0);
        engine.tick(1.5);
        assertEquals(150, zombie.getHealth());
    }

    @Test
    void caulipowerElectricBlueberryAndCattailUseBoardWideTargets() {
        GameEngine cauliEngine = engine();
        Plant cauli = cauliEngine.plant(PlantType.CAULIPOWER, 1, new Position(0, 0));
        Zombie target = cauliEngine.spawnZombie(ZombieType.NORMAL, 4, 7.0);
        cauliEngine.tick(12.0);
        assertTrue(target.isHypnotized());

        GameEngine electricEngine = engine();
        electricEngine.plant(PlantType.ELECTRIC_BLUEBERRY, 1, new Position(0, 0));
        electricEngine.spawnZombie(ZombieType.GARGANTUAR, 4, 7.0);
        electricEngine.tick(12.0);
        electricEngine.tick(0.5);
        assertFalse(electricEngine.getZombies().stream()
                .anyMatch(zombie -> zombie.getType() == ZombieType.GARGANTUAR));

        GameEngine cattailEngine = engine();
        cattailEngine.plant(PlantType.CAT_TAIL, 1, new Position(0, 0));
        Zombie crossLane = cattailEngine.spawnZombie(ZombieType.NORMAL, 4, 7.0);
        cattailEngine.tick(1.5);
        assertEquals(175, crossLane.getHealth());
        assertFalse(cauli.isDead());
    }

    @Test
    void starfruitAndGooPeashooterUseTheirCanonicalProjectileRules() {
        GameEngine starEngine = engine();
        starEngine.plant(PlantType.STARFRUIT, 1, new Position(2, 2));
        Zombie forward = starEngine.spawnZombie(ZombieType.NORMAL, 2, 5.0);
        Zombie upperBack = starEngine.spawnZombie(ZombieType.NORMAL, 1, 0.5);
        starEngine.tick(1.5);
        assertEquals(170, forward.getHealth());
        assertEquals(170, upperBack.getHealth());

        GameEngine gooEngine = engine();
        gooEngine.plant(PlantType.GOO_PEASHOOTER, 1, new Position(2, 0));
        Zombie bucket = gooEngine.spawnZombie(ZombieType.BUCKETHEAD, 2, 3.0);
        gooEngine.tick(1.5);
        assertEquals(170, bucket.getHealth());
        assertEquals(1100, bucket.getArmor());
        gooEngine.tick(1.0);
        assertEquals(165, bucket.getHealth());
    }

    @Test
    void grapeshotExplodesAndLaunchesTemporaryBouncingGrapes() {
        GameEngine engine = engine();
        Zombie gargantuar = engine.spawnZombie(ZombieType.GARGANTUAR, 2, 2.5);
        engine.plant(PlantType.GRAPESHOT, 1, new Position(2, 2));

        assertEquals(1800, gargantuar.getHealth());
        assertEquals(8, engine.getProjectiles().size());
        assertTrue(engine.getGameMap().getPlants().isEmpty());
        engine.tick(5.0);
        assertTrue(engine.getProjectiles().isEmpty());
    }

    @Test
    void chomperWasabiAndKiwibeastUseDistinctMeleeRules() {
        GameEngine chomperEngine = engine();
        Plant chomper = chomperEngine.plant(PlantType.CHOMPER, 1, new Position(2, 2));
        chomperEngine.spawnZombie(ZombieType.NORMAL, 2, 2.5);
        chomperEngine.tick(0.25);
        assertTrue(chomperEngine.getZombies().isEmpty());
        assertTrue(chomper.getState("DIGEST_UNTIL", Double.class, 0.0) > 39.0);

        GameEngine wasabiEngine = engine();
        wasabiEngine.plant(PlantType.WASABI_WHIP, 1, new Position(2, 2));
        Zombie front = wasabiEngine.spawnZombie(ZombieType.NORMAL, 2, 3.0);
        Zombie back = wasabiEngine.spawnZombie(ZombieType.NORMAL, 2, 1.9);
        wasabiEngine.tick(2.0);
        assertEquals(150, front.getHealth());
        assertEquals(150, back.getHealth());

        GameEngine kiwiEngine = engine();
        Plant kiwi = kiwiEngine.plant(PlantType.KIWIBEAST, 1, new Position(2, 2));
        kiwiEngine.tick(24.0);
        assertEquals(2, kiwi.getState("KIWIBEAST_STAGE", Integer.class, 0));
        Zombie nearby = kiwiEngine.spawnZombie(ZombieType.NORMAL, 2, 3.0);
        kiwiEngine.tick(2.0);
        assertTrue(nearby.getHealth() < 175);
    }

    @Test
    void sweetPotatoAndHypnoShroomControlZombieAllegianceAndRows() {
        GameEngine sweetEngine = engine();
        Plant sweet = sweetEngine.plant(PlantType.SWEET_POTATO, 1, new Position(2, 3));
        Zombie adjacentLane = sweetEngine.spawnZombie(ZombieType.NORMAL, 1, 3.5);
        sweetEngine.tick(0.1);
        assertEquals(2, adjacentLane.getRow());
        sweet.receiveDamage(500, sweetEngine, adjacentLane);
        sweet.usePlantFood(sweetEngine);
        assertEquals(sweet.getMaxHp(), sweet.getHp());

        GameEngine hypnoEngine = engine();
        Plant hypno = hypnoEngine.plant(PlantType.HYPNO_SHROOM, 1, new Position(2, 2));
        Zombie eater = hypnoEngine.spawnZombie(ZombieType.NORMAL, 2, 2.5);
        hypnoEngine.tick(0.1);
        assertTrue(eater.isHypnotized());
        assertTrue(hypno.isDead() || !hypnoEngine.getGameMap().getPlants().contains(hypno));

        GameEngine foodEngine = engine();
        Plant foodHypno = foodEngine.plant(PlantType.HYPNO_SHROOM, 1, new Position(2, 2));
        Zombie victim = foodEngine.spawnZombie(ZombieType.NORMAL, 2, 3.0);
        foodHypno.usePlantFood(foodEngine);
        victim.receiveDamage(1, DamageType.TRUE, foodEngine);
        assertTrue(foodEngine.getZombies().stream()
                .anyMatch(zombie -> zombie.getType() == ZombieType.GARGANTUAR
                        && zombie.isHypnotized()));
    }

    private GameEngine engine() {
        GameEngine engine = new GameEngine(
                PlantRegistry.getDefault(), new GameMap(), new Random(0));
        engine.setSun(20_000);
        return engine;
    }
}
