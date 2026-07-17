package minigame;

import model.enums.PlantType;
import model.enums.ZombieType;
import model.inGame.plant.PlantRegistry;
import model.inGame.zombie.ZombieRegistry;
import model.miniGame.BowlingBall;
import model.miniGame.BowlingPlantType;
import model.miniGame.BowlingWallnut;
import model.miniGame.ConveyorPacket;
import model.miniGame.MiniGameCatalog;
import model.miniGame.MiniGameDefinition;
import model.miniGame.MiniGameId;
import model.miniGame.MiniGameLifecycleState;
import model.miniGame.MiniGameSession;
import model.miniGame.MiniGameSessionFactory;
import model.miniGame.WallNutBowlingLevelRules;
import model.miniGame.WallNutBowlingState;
import model.sim.zombie.ZombieInstance;
import model.sim.zombie.ZombieSpec;
import org.junit.jupiter.api.Test;
import util.SeededRandomSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WallNutBowlingTest {

    @Test
    void threeLevelsIncreaseBowlingPressureThroughExplicitConfiguration() {
        WallNutBowlingLevelRules first = BowlingWallnut.rulesFor(1);
        WallNutBowlingLevelRules second = BowlingWallnut.rulesFor(2);
        WallNutBowlingLevelRules third = BowlingWallnut.rulesFor(3);

        assertTrue(second.isHarderThan(first));
        assertTrue(third.isHarderThan(second));
        assertEquals(8, first.getZombieCount());
        assertEquals(12, second.getZombieCount());
        assertEquals(16, third.getZombieCount());
        assertEquals(3, first.getInitialPackets());
        assertEquals(2, second.getInitialPackets());
        assertEquals(1, third.getInitialPackets());
        assertEquals(2, first.getRedLineColumn());
        assertEquals(1, third.getRedLineColumn());

        MiniGameDefinition definition = MiniGameCatalog.mandatoryDefaults()
                .find(MiniGameId.BOWLING_WALLNUT);
        for (int level = 1; level <= 3; level++) {
            assertEquals(0, definition.getLevel(level).getStartingResources());
            assertFalse(definition.getLevel(level).isSkySunEnabled());
        }
    }

    @Test
    void conveyorRedLineAndCommandsReplacePlantSelectionAndSun() {
        MiniGameSession session = session(1);
        WallNutBowlingState state = state(session);

        assertEquals(0, session.getSimulation().getWorld().getSunBalance());
        assertEquals(List.of(
                        BowlingPlantType.BOWLING_WALL_NUT,
                        BowlingPlantType.EXPLODE_O_NUT,
                        BowlingPlantType.GIANT_WALL_NUT),
                state.getConveyor().getAvailablePackets().stream()
                        .map(ConveyorPacket::getPlantType).toList());
        assertFalse(session.executeStrategyCommand(
                "plant plant -t wall-nut -l (2, 2)").getStatus());

        assertFalse(session.executeStrategyCommand(
                "plant conveyor -i 1 -l (3, 2)").getStatus());
        assertTrue(state.getConveyor().find(1).isAvailable());
        assertTrue(session.executeStrategyCommand(
                "plant conveyor -i 1 -l (2, 2)").getStatus());
        assertFalse(session.executeStrategyCommand(
                "plant conveyor -i 1 -l (2, 2)").getStatus());

        int before = state.getConveyor().getAvailableCount();
        session.advance(state.getRules().getConveyorIntervalTicks());
        assertEquals(before + 1, state.getConveyor().getAvailableCount());
        assertTrue(session.executeStrategyCommand("show conveyor").getMessage()
                .contains("legal launch columns: 0-2"));
    }

    @Test
    void bowlingWallnutUsesNormalZombieHealthAndTurns45Then90Degrees() {
        MiniGameSession session = session(1);
        WallNutBowlingState state = state(session);
        session.getSimulation().getWorld().getZombies().clear();
        ZombieInstance first = durableZombie(3.10, 2, 1000);
        ZombieInstance second = durableZombie(3.35, 2, 1000);
        session.getSimulation().getWorld().addZombie(first);
        session.getSimulation().getWorld().addZombie(second);

        assertEquals(ZombieRegistry.getDefault().requireMandatory(ZombieType.NORMAL).getHealth(),
                state.getNormalZombieCollisionDamage());
        assertTrue(session.executeStrategyCommand(
                "plant conveyor -i 1 -l (2, 2)").getStatus());
        BowlingBall ball = state.getBalls().get(0);

        session.advance(1);
        assertEquals(1000 - state.getNormalZombieCollisionDamage(), first.getHp());
        assertEquals(45.0, ball.getHeadingDegrees());
        assertEquals(1, ball.getDeflectionCount());
        assertTrue(ball.isActive());

        session.advance(1);
        assertEquals(1000 - state.getNormalZombieCollisionDamage(), second.getHp());
        assertEquals(-45.0, ball.getHeadingDegrees());
        assertEquals(2, ball.getDeflectionCount());
        assertTrue(ball.isActive());
    }

    @Test
    void topBoundaryUsesTheSameLaterCollision90DegreeTurn() {
        MiniGameSession session = session(1);
        WallNutBowlingState state = state(session);
        session.getSimulation().getWorld().getZombies().clear();
        session.getSimulation().getWorld().addZombie(durableZombie(3.10, 4, 1000));
        assertTrue(session.executeStrategyCommand(
                "plant conveyor -i 1 -l (2, 4)").getStatus());
        BowlingBall ball = state.getBalls().get(0);

        session.advance(1);
        assertEquals(-45.0, ball.getHeadingDegrees());
        StringBuilder messages = new StringBuilder();
        for (int tick = 0; tick < 20 && ball.getDeflectionCount() < 2; tick++) {
            messages.append(session.advance(1).getMessage()).append('\n');
        }

        assertEquals(2, ball.getDeflectionCount());
        assertEquals(45.0, ball.getHeadingDegrees());
        assertTrue(messages.toString().contains("top boundary"));
    }

    @Test
    void explodeONutUsesCherryBombDamageAcrossExactlyThreeByThreeTiles() {
        MiniGameSession session = session(1);
        WallNutBowlingState state = state(session);
        session.getSimulation().getWorld().getZombies().clear();
        ZombieInstance center = canonicalZombie(ZombieType.GARGANTUAR, 3.10, 2);
        ZombieInstance adjacent = durableZombie(3.10, 1, 2500);
        ZombieInstance outside = durableZombie(3.10, 4, 2500);
        session.getSimulation().getWorld().addZombie(center);
        session.getSimulation().getWorld().addZombie(adjacent);
        session.getSimulation().getWorld().addZombie(outside);

        assertEquals(PlantRegistry.getDefault().requireMandatory(PlantType.CHERRY_BOMB).getDamage(),
                state.getExplosionDamage());
        assertTrue(session.executeStrategyCommand(
                "plant conveyor -i 2 -l (2, 2)").getStatus());
        BowlingBall ball = state.getBalls().get(0);
        String output = session.advance(1).getMessage();

        assertEquals(3600 - state.getExplosionDamage(), center.getHp());
        assertEquals(2500 - state.getExplosionDamage(), adjacent.getHp());
        assertEquals(2500, outside.getHp());
        assertFalse(ball.isActive());
        assertTrue(output.contains("3x3 area"));
    }

    @Test
    void giantWallnutCrushesEveryContactAndKeepsMovingStraight() {
        MiniGameSession session = session(1);
        WallNutBowlingState state = state(session);
        session.getSimulation().getWorld().getZombies().clear();
        ZombieInstance first = canonicalZombie(ZombieType.GARGANTUAR, 3.10, 2);
        ZombieInstance second = canonicalZombie(ZombieType.KNIGHT, 3.35, 2);
        session.getSimulation().getWorld().addZombie(first);
        session.getSimulation().getWorld().addZombie(second);

        assertTrue(session.executeStrategyCommand(
                "plant conveyor -i 3 -l (2, 2)").getStatus());
        BowlingBall ball = state.getBalls().get(0);
        session.advance(1);
        double afterContact = ball.getX();

        assertTrue(first.isDead());
        assertTrue(second.isDead());
        assertTrue(ball.isActive());
        assertEquals(0.0, ball.getHeadingDegrees());
        session.advance(1);
        assertTrue(ball.getX() > afterContact);
        assertTrue(ball.isActive());
    }

    @Test
    void standardLawnMowerAndBrainLossRulesRemainShared() {
        MiniGameSession session = session(1);
        session.getSimulation().getWorld().getZombies().clear();
        session.getSimulation().getWorld().useLawnMower(0);
        ZombieInstance breach = new ZombieInstance(
                ZombieSpec.builder("breach")
                        .health(1000)
                        .speedTilesPerSecond(2.0)
                        .eatDamagePerSecond(0)
                        .build(), 0.01, 0);
        session.getSimulation().getWorld().addZombie(breach);

        String output = session.advance(1).getMessage();

        assertEquals(MiniGameLifecycleState.LOST, session.getState());
        assertTrue(output.contains("ate your brain"));
    }

    private MiniGameSession session(int level) {
        MiniGameDefinition definition = MiniGameCatalog.mandatoryDefaults()
                .find(MiniGameId.BOWLING_WALLNUT);
        MiniGameSession session = new MiniGameSessionFactory(new SeededRandomSource(7))
                .create("bowler", definition, definition.getLevel(level));
        assertTrue(session.start().getStatus());
        return session;
    }

    private WallNutBowlingState state(MiniGameSession session) {
        return session.getStrategyState(WallNutBowlingState.class);
    }

    private ZombieInstance durableZombie(double x, int row, int health) {
        return new ZombieInstance(ZombieSpec.builder("durable")
                .health(health)
                .speedTilesPerSecond(0)
                .eatDamagePerSecond(0)
                .build(), x, row);
    }

    private ZombieInstance canonicalZombie(ZombieType type, double x, int row) {
        return new ZombieInstance(ZombieSpec.fromDefinition(
                ZombieRegistry.getDefault().requireMandatory(type)), x, row);
    }
}
