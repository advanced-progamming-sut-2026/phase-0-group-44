package plant;

import model.GameEngine;
import model.Position;
import model.Tile;
import model.enums.ObstacleType;
import model.enums.PlantType;
import model.enums.TerrainType;
import model.inGame.GameMap;
import model.inGame.plant.Plant;
import model.inGame.plant.PlantFactory;
import model.inGame.plant.PlantRegistry;
import model.inGame.zombie.Zombie;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlantBoardSupportTest {
    private GameEngine engine() {
        GameEngine engine = new GameEngine(PlantRegistry.getDefault(), new GameMap(), new Random(3));
        engine.setSun(20_000);
        return engine;
    }

    @Test
    void lilyPadAndPumpkinUseIndependentBoardLayers() {
        GameEngine engine = engine();
        Position water = new Position(2, 2);
        engine.getGameMap().setTerrain(water, TerrainType.WATER);
        engine.plant(PlantType.LILY_PAD, 1, water);
        engine.tick(5.0);
        engine.plant(PlantType.PEASHOOTER, 1, water);
        engine.tick(5.0);
        engine.plant(PlantType.PUMPKIN, 1, water);

        Tile tile = engine.getGameMap().getTile(water);
        assertEquals(PlantType.LILY_PAD, tile.getSupportPlant().getType());
        assertEquals(PlantType.PEASHOOTER, tile.getPrimaryPlant().getType());
        assertEquals(PlantType.PUMPKIN, tile.getArmorPlant().getType());
    }

    @Test
    void torchwoodTransformsPeasAndMagnetRemovesMetalArmor() {
        GameEngine engine = engine();
        engine.plant(PlantType.PEASHOOTER, 1, new Position(2, 1));
        engine.plant(PlantType.TORCHWOOD, 1, new Position(2, 3));
        Zombie zombie = new Zombie("target", 2, 5.0, 100);
        engine.addZombie(zombie);
        engine.tick(1.5);
        engine.tick(1.0);
        assertEquals(60, zombie.getHealth());

        GameEngine magnetEngine = engine();
        magnetEngine.plant(PlantType.MAGNET_SHROOM, 1, new Position(2, 2));
        Zombie armored = new Zombie("bucket", 1, 5.0, 100, 80, true);
        magnetEngine.addZombie(armored);
        magnetEngine.tick(10.0);
        assertEquals(0, armored.getArmor());
    }

    @Test
    void defendersApplyReactiveEffects() {
        GameEngine engine = engine();
        Position endurianPosition = new Position(2, 2);
        engine.plant(PlantType.ENDURIAN, 1, endurianPosition);
        Zombie attacker = new Zombie("attacker", 2, 2.0, 100);
        engine.addZombie(attacker);
        engine.damagePlantAt(endurianPosition, 50, attacker);
        assertEquals(80, attacker.getHealth());

        GameEngine garlicEngine = engine();
        Position garlicPosition = new Position(2, 2);
        garlicEngine.plant(PlantType.GARLIC, 1, garlicPosition);
        Zombie eater = new Zombie("eater", 2, 2.0, 100);
        garlicEngine.addZombie(eater);
        garlicEngine.damagePlantAt(garlicPosition, 10, eater);
        assertEquals(1, eater.getRow());
    }

    @Test
    void instantPlantsAndMintsApplyThenExpire() {
        GameEngine engine = engine();
        int before = engine.getSun();
        Plant goldBloom = engine.plant(PlantType.GOLD_BLOOM, 1, new Position(1, 1));
        assertTrue(goldBloom.isDead());
        assertEquals(before + 375, engine.getSun());

        engine.tick(5.0);
        Plant sunflower = engine.plant(PlantType.SUNFLOWER, 1, new Position(2, 2));
        engine.tick(5.0);
        Plant mint = engine.plant(PlantType.ENLIGHTEN_MINT, 1, new Position(3, 2));
        assertTrue(mint.isDead());
        assertTrue(engine.hasFamilyBoost(sunflower.getCategory()));
    }

    @Test
    void hotPotatoAndGraveBusterUseObstacleRules() {
        GameEngine engine = engine();
        Position ice = new Position(1, 2);
        engine.getGameMap().setObstacle(ice, ObstacleType.ICE);
        engine.plant(PlantType.HOT_POTATO, 1, ice);
        assertEquals(ObstacleType.NONE, engine.getGameMap().getTile(ice).getObstacle());

        Position grave = new Position(3, 2);
        engine.getGameMap().setObstacle(grave, ObstacleType.GRAVE);
        engine.tick(5.0);
        engine.plant(PlantType.GRAVE_BUSTER, 1, grave);
        engine.tick(4.0);
        assertEquals(ObstacleType.NONE, engine.getGameMap().getTile(grave).getObstacle());
    }
}
