package controller;

import model.Result;
import model.Store;
import model.enums.PlantType;
import model.enums.TerrainType;
import model.inGame.PlantSelection;
import model.sim.Damageable;
import model.sim.SimulationWorld;
import model.sim.board.Board;
import model.sim.board.PlantSpec;
import model.sim.board.PlantSpecSource;
import model.sim.board.Tile;
import model.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import repository.JsonUserRepository;
import service.RewardService;
import service.UserService;
import util.RandomSource;

import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Board, terrain, planting, cooldowns, plant food, drops and status commands. */
class BoardMechanicsTest {

    private static final Clock CLOCK =
            Clock.fixed(Instant.parse("2026-07-14T09:30:00Z"), ZoneOffset.UTC);

    @TempDir
    Path tempDir;

    private SimulationWorld world;
    private PlantSelection selection;
    private FakeSpecSource specs;
    private BoardController board;

    @BeforeEach
    void setUp() {
        world = new SimulationWorld();
        selection = new PlantSelection();
        specs = new FakeSpecSource();
        board = new BoardController(world, selection, specs);
    }

    private void select(PlantType type) {
        selection.add(type);
    }

    // ---------- board & coordinates ----------

    @Test
    void defaultBoardIsFiveByNineAndValidatesCoordinates() {
        Board b = world.getBoard();
        assertEquals(5, b.getRows());
        assertEquals(9, b.getColumns());

        assertTrue(b.isValidTile(0, 0));
        assertTrue(b.isValidTile(8, 4));
        assertFalse(b.isValidTile(9, 0));
        assertFalse(b.isValidTile(0, 5));
        assertFalse(b.isValidTile(-1, 0));

        assertTrue(b.isValidZombitePosition(8.5, 2));
        assertFalse(b.isValidZombitePosition(9.0, 2));
    }

    // ---------- planting validation order ----------

    @Test
    void plantingRequiresThePlantToBeSelected() {
        specs.put(PlantSpec.builder(PlantType.PEASHOOTER).sunCost(100).hp(300).build());
        world.addSun(500);

        Result<String> result = board.plantPlant(PlantType.PEASHOOTER, 0, 0);

        assertFalse(result.getStatus());
        assertEquals("this plant is not selected for the level", result.getMessage());
        assertEquals(500, world.getSunBalance(), "no sun deducted");
    }

    @Test
    void plantingRejectsAnInvalidTileWithoutDeductingSun() {
        select(PlantType.PEASHOOTER);
        specs.put(PlantSpec.builder(PlantType.PEASHOOTER).sunCost(100).hp(300).build());
        world.addSun(500);

        Result<String> result = board.plantPlant(PlantType.PEASHOOTER, 20, 20);

        assertFalse(result.getStatus());
        assertEquals("invalid tile", result.getMessage());
        assertEquals(500, world.getSunBalance());
    }

    @Test
    void sunIsDeductedOnlyAfterEveryCheckPasses() {
        select(PlantType.PEASHOOTER);
        specs.put(PlantSpec.builder(PlantType.PEASHOOTER).sunCost(100).rechargeTicks(50).hp(300).build());
        world.addSun(120);

        Result<String> ok = board.plantPlant(PlantType.PEASHOOTER, 3, 2);
        assertTrue(ok.getStatus());
        assertEquals(20, world.getSunBalance(), "cost deducted");
        assertEquals(PlantType.PEASHOOTER, world.getBoard().tileAt(3, 2).getSupportPlant().getType());
    }

    @Test
    void plantingRejectsWhenSunIsInsufficientAndKeepsTheBalance() {
        select(PlantType.PEASHOOTER);
        specs.put(PlantSpec.builder(PlantType.PEASHOOTER).sunCost(100).hp(300).build());
        world.addSun(50);

        Result<String> result = board.plantPlant(PlantType.PEASHOOTER, 1, 1);

        assertFalse(result.getStatus());
        assertEquals("not enough sun", result.getMessage());
        assertEquals(50, world.getSunBalance());
        assertNull(world.getBoard().tileAt(1, 1).getSupportPlant());
    }

    // ---------- cooldown ----------

    @Test
    void rechargeBlocksAReplantUntilItElapsesAndTheCheatLiftsIt() {
        select(PlantType.PEASHOOTER);
        specs.put(PlantSpec.builder(PlantType.PEASHOOTER).sunCost(50).rechargeTicks(30).hp(300).build());
        world.addSun(1000);

        assertTrue(board.plantPlant(PlantType.PEASHOOTER, 0, 0).getStatus());
        assertTrue(world.isOnCooldown(PlantType.PEASHOOTER));

        Result<String> blocked = board.plantPlant(PlantType.PEASHOOTER, 1, 0);
        assertFalse(blocked.getStatus());
        assertEquals("this plant is still recharging", blocked.getMessage());

        board.cheatRemoveCooldown();
        assertFalse(world.isOnCooldown(PlantType.PEASHOOTER));
        assertTrue(board.plantPlant(PlantType.PEASHOOTER, 1, 0).getStatus());
    }

    // ---------- terrain ----------

    @Test
    void gravestoneIsUnplantableAndBecomesGroundWhenDestroyed() {
        select(PlantType.PEASHOOTER);
        specs.put(PlantSpec.builder(PlantType.PEASHOOTER).sunCost(0).hp(300).build());
        world.getBoard().tileAt(2, 2).setTerrain(TerrainType.GRAVESTONE);

        Result<String> result = board.plantPlant(PlantType.PEASHOOTER, 2, 2);
        assertFalse(result.getStatus());
        assertEquals("this tile does not permit that plant", result.getMessage());

        Tile grave = world.getBoard().tileAt(2, 2);
        assertEquals(700, grave.getTerrainHealth());
        grave.damageTerrain(700, TerrainType.NORMAL_EGYPT);
        assertEquals(TerrainType.NORMAL_EGYPT, grave.getTerrain());
    }

    @Test
    void waterAcceptsOnlyWaterCapableOrSupportAndThenAStackedPlant() {
        world.getBoard().tileAt(4, 2).setTerrain(TerrainType.WATER);

        select(PlantType.PEASHOOTER);
        specs.put(PlantSpec.builder(PlantType.PEASHOOTER).sunCost(0).hp(300).build());
        assertFalse(board.plantPlant(PlantType.PEASHOOTER, 4, 2).getStatus(),
                "a non-water plant cannot go directly on water");

        select(PlantType.WALL_NUT);
        specs.put(PlantSpec.builder(PlantType.WALL_NUT).sunCost(0).hp(4000)
                .providesSupport(true).waterCapable(true).build());
        assertTrue(board.plantPlant(PlantType.WALL_NUT, 4, 2).getStatus(),
                "a support/water plant can");

        specs.put(PlantSpec.builder(PlantType.PEASHOOTER).sunCost(0).hp(300)
                .stacksOnSupport(true).build());
        assertTrue(board.plantPlant(PlantType.PEASHOOTER, 4, 2).getStatus(),
                "another plant stacks on the support");

        Tile tile = world.getBoard().tileAt(4, 2);
        assertEquals(PlantType.WALL_NUT, tile.getSupportPlant().getType());
        assertEquals(PlantType.PEASHOOTER, tile.getStackedPlant().getType());
    }

    @Test
    void frozenTilesAreUnplantableWhileFrozenAndFireMelts() {
        Tile tile = world.getBoard().tileAt(1, 1);
        tile.freeze();
        assertTrue(tile.isFrozen());
        assertEquals(600, tile.getIceHealth());

        select(PlantType.PEASHOOTER);
        specs.put(PlantSpec.builder(PlantType.PEASHOOTER).sunCost(0).hp(300).build());
        assertFalse(board.plantPlant(PlantType.PEASHOOTER, 1, 1).getStatus());

        tile.meltIce(600);
        assertFalse(tile.isFrozen());
        assertTrue(board.plantPlant(PlantType.PEASHOOTER, 1, 1).getStatus());
    }

    @Test
    void slipperyTerrainCarriesItsRowDirection() {
        assertEquals(-1, TerrainType.SLIPPERY_UP.slipperyRowDelta());
        assertEquals(1, TerrainType.SLIPPERY_DOWN.slipperyRowDelta());
        assertTrue(TerrainType.SLIPPERY_UP.isSlippery());
    }

    // ---------- pluck ----------

    @Test
    void pluckRemovesTheStackedPlantFirstThenTheSupport() {
        world.getBoard().tileAt(0, 0).setTerrain(TerrainType.NORMAL_EGYPT);
        select(PlantType.WALL_NUT);
        specs.put(PlantSpec.builder(PlantType.WALL_NUT).sunCost(0).hp(4000).providesSupport(true).build());
        board.plantPlant(PlantType.WALL_NUT, 0, 0);
        select(PlantType.PEASHOOTER);
        specs.put(PlantSpec.builder(PlantType.PEASHOOTER).sunCost(0).hp(300).stacksOnSupport(true).build());
        board.plantPlant(PlantType.PEASHOOTER, 0, 0);

        Result<String> first = board.pluckPlant(0, 0);
        assertTrue(first.getStatus());
        assertEquals("PEASHOOTER", first.getData());
        assertNull(world.getBoard().tileAt(0, 0).getStackedPlant());
        assertEquals(PlantType.WALL_NUT, world.getBoard().tileAt(0, 0).getSupportPlant().getType());

        Result<String> second = board.pluckPlant(0, 0);
        assertTrue(second.getStatus());
        assertEquals("WALL_NUT", second.getData());
        assertFalse(world.getBoard().tileAt(0, 0).hasAnyPlant());

        assertFalse(board.pluckPlant(0, 0).getStatus(), "nothing left to pluck");
    }

    // ---------- plant food ----------

    @Test
    void cheatAddPlantFoodCapsAtThree() {
        assertEquals("1", board.cheatAddPlantFood().getData());
        assertEquals("2", board.cheatAddPlantFood().getData());
        assertEquals("3", board.cheatAddPlantFood().getData());

        Result<String> capped = board.cheatAddPlantFood();
        assertTrue(capped.getMessage().contains("maximum"));
        assertEquals(3, world.getPlantFood());
    }

    @Test
    void feedRequiresAPlantWithAnEffectAndPlantFoodAvailable() {
        select(PlantType.SUNFLOWER);
        specs.put(PlantSpec.builder(PlantType.SUNFLOWER).sunCost(0).hp(300)
                .hasPlantFoodEffect(true).build());
        board.plantPlant(PlantType.SUNFLOWER, 2, 2);

        assertFalse(board.feedPlant(2, 2).getStatus(), "no plant food yet");
        board.cheatAddPlantFood();
        assertTrue(board.feedPlant(2, 2).getStatus());
        assertEquals(0, world.getPlantFood(), "one plant food spent");

        select(PlantType.WALL_NUT);
        specs.put(PlantSpec.builder(PlantType.WALL_NUT).sunCost(0).hp(4000).build());
        board.plantPlant(PlantType.WALL_NUT, 3, 3);
        board.cheatAddPlantFood();
        assertFalse(board.feedPlant(3, 3).getStatus(), "no plant-food effect");
    }

    @Test
    void aGlowingZombieDeathGrantsPlantFoodWithTheExactMessage() {
        UserService userService = new UserService(
                new JsonUserRepository(tempDir.resolve("u.json")), CLOCK);
        User user = new User();
        user.setUsername("k");
        user.applyDefaults();
        userService.addUser(user);

        // RNG: drop roll >= 0.10 so no item drop interferes.
        ScriptedRandom random = new ScriptedRandom().queueDouble(0.99);
        RewardService rewards = new RewardService(userService, random);

        List<String> messages = rewards.onZombieDeath(true, user, world);

        assertEquals(1, messages.size());
        assertEquals("The glowing zombie dropeed a plant food; you have 1 plant foods now.",
                messages.get(0));
        assertEquals(1, world.getPlantFood());
    }

    // ---------- drops ----------

    @Test
    void deathDropsUseEqualWeightsAndTheExactMessages() {
        UserService userService = new UserService(
                new JsonUserRepository(tempDir.resolve("u.json")), CLOCK);
        User user = new User();
        user.setUsername("k");
        user.applyDefaults();
        userService.addUser(user);

        // diamond: drop roll < 0.10, then item index 0.
        ScriptedRandom diamond = new ScriptedRandom().queueDouble(0.05).queueInt(0);
        List<String> d = new RewardService(userService, diamond).onZombieDeath(false, user, world);
        assertEquals("A zombie dropeed a diamond; you have 1 diamonds now.", d.get(0));

        ScriptedRandom coins = new ScriptedRandom().queueDouble(0.05).queueInt(1);
        List<String> c = new RewardService(userService, coins).onZombieDeath(false, user, world);
        assertEquals("A zombie dropeed a coin; you have 50 coins now.", c.get(0));

        ScriptedRandom pot = new ScriptedRandom().queueDouble(0.05).queueInt(2);
        List<String> p = new RewardService(userService, pot).onZombieDeath(false, user, world);
        assertEquals("A zombie dropeed a pot; you have 1 pots now.", p.get(0));

        ScriptedRandom none = new ScriptedRandom().queueDouble(0.5);
        assertTrue(new RewardService(userService, none).onZombieDeath(false, user, world).isEmpty());
    }

    @Test
    void glowingChanceIsFivePercentThroughTheInjectedRng() {
        RewardService below = new RewardService(null, new ScriptedRandom().queueDouble(0.04));
        RewardService above = new RewardService(null, new ScriptedRandom().queueDouble(0.05));

        assertTrue(below.rollGlowing());
        assertFalse(above.rollGlowing());
    }

    // ---------- status ----------

    @Test
    void showMapIncludesWavePlantFoodSunAndAMowerStatePerRow() {
        world.setCurrentWave(2);
        world.addSun(75);
        world.addPlantFood();
        world.useLawnMower(1);

        String map = board.showMap().getData();

        assertTrue(map.contains("wave: 2"));
        assertTrue(map.contains("plant food: 1"));
        assertTrue(map.contains("sun: 75"));
        assertTrue(map.contains("row 0 [mower ready]"));
        assertTrue(map.contains("row 1 [mower used]"));
        assertEquals(5, map.split("\\[mower ").length - 1, "one mower state per row");
    }

    @Test
    void showMapShowsDecimalZombiePositions() {
        world.addZombie(new FakeZombie(6, 2));
        String map = board.showMap().getData();
        assertTrue(map.contains("zombies:"));
        assertTrue(map.contains("(6, 2)"));
    }

    @Test
    void showPlantsStatusReportsCostAndCooldown() {
        select(PlantType.PEASHOOTER);
        specs.put(PlantSpec.builder(PlantType.PEASHOOTER).sunCost(100).rechargeTicks(50).hp(300).build());
        world.addSun(100);
        board.plantPlant(PlantType.PEASHOOTER, 0, 0);

        String status = board.showPlantsStatus().getData();

        assertTrue(status.contains("PEASHOOTER"));
        assertTrue(status.contains("sun cost 100"));
        assertTrue(status.contains("recharging"));
    }

    @Test
    void showTileStatusReportsTerrainAndPlantHealth() {
        select(PlantType.WALL_NUT);
        specs.put(PlantSpec.builder(PlantType.WALL_NUT).sunCost(0).hp(4000).build());
        board.plantPlant(PlantType.WALL_NUT, 5, 3);

        String status = board.showTileStatus(5, 3).getData();

        assertTrue(status.contains("tile (5, 3)"));
        assertTrue(status.contains("NORMAL_EGYPT"));
        assertTrue(status.contains("WALL_NUT"));
        assertTrue(status.contains("health 4000"));

        assertFalse(board.showTileStatus(99, 99).getStatus());
    }

    // ---------- fakes ----------

    private static final class FakeSpecSource implements PlantSpecSource {
        private final Map<PlantType, PlantSpec> specs = new EnumMap<>(PlantType.class);

        void put(PlantSpec spec) {
            specs.put(spec.getType(), spec);
        }

        @Override
        public PlantSpec specOf(PlantType type) {
            return specs.get(type);
        }
    }

    private static final class FakeZombie implements Damageable {
        private final int x;
        private final int y;

        FakeZombie(int x, int y) {
            this.x = x;
            this.y = y;
        }

        @Override
        public int getTileX() {
            return x;
        }

        @Override
        public int getTileY() {
            return y;
        }

        @Override
        public void takeDamage(int amount) {
        }

        @Override
        public boolean isDead() {
            return false;
        }
    }

    private static final class ScriptedRandom implements RandomSource {
        private final Deque<Double> doubles = new ArrayDeque<>();
        private final Deque<Integer> ints = new ArrayDeque<>();

        ScriptedRandom queueDouble(double v) {
            doubles.add(v);
            return this;
        }

        ScriptedRandom queueInt(int v) {
            ints.add(v);
            return this;
        }

        @Override
        public int nextInt(int bound) {
            return ints.isEmpty() ? 0 : ints.poll();
        }

        @Override
        public double nextDouble() {
            return doubles.isEmpty() ? 0.0 : doubles.poll();
        }
    }
}
