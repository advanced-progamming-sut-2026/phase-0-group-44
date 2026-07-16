package minigame;

import model.enums.PlantType;
import model.miniGame.*;
import model.miniGame.framework.*;
import org.junit.jupiter.api.Test;
import util.RandomSource;

import static org.junit.jupiter.api.Assertions.*;

class VasebreakerRulesTest {
    @Test void threeLevelsIncreaseCountDangerAndPacketPressure() {
        VasebreakerLevel one = VasebreakerLevels.level(1), two = VasebreakerLevels.level(2), three = VasebreakerLevels.level(3);
        assertTrue(two.getVases().size() > one.getVases().size());
        assertTrue(three.getVases().size() > two.getVases().size());
        assertTrue(two.getPacketLifetimeTicks() < one.getPacketLifetimeTicks());
        assertTrue(three.getPacketLifetimeTicks() < two.getPacketLifetimeTicks());
        assertTrue(three.getVases().stream().anyMatch(v -> v.getType() == VaseType.GARGANTUAR));
    }

    @Test void normalVasesCanBeEmptyZombieOrOneUsePacket() {
        VaseBreaker game = new VaseBreaker(VasebreakerLevels.level(1), fixed(0));
        assertEquals("vase empty", game.breakVase(0,4,0));
        assertTrue(game.breakVase(1,4,0).startsWith("released normal"));
        assertTrue(game.breakVase(2,4,0).startsWith("dropped packet-1"));
        assertEquals(1, game.getReleasedThreats());
        SeedPacket packet = game.getPackets().iterator().next();
        assertTrue(packet.isAvailable(0));
        game.plantPacket(packet.getId(), 2, 1, 0);
        assertTrue(packet.isUsed());
        assertThrows(IllegalStateException.class, () -> game.plantPacket(packet.getId(), 2, 2, 0));
    }

    @Test void plantVaseAlwaysDropsRandomPlantFromDeterministicSeam() {
        VaseBreaker game = new VaseBreaker(VasebreakerLevels.level(1), fixed(3));
        String result = game.breakVase(3,4,0);
        assertTrue(result.contains("repeater"));
        assertEquals(PlantType.REPEATER, game.getPackets().iterator().next().getPlantType());
    }

    @Test void gargantuarVaseAlwaysReleasesGargantuar() {
        VaseBreaker game = new VaseBreaker(VasebreakerLevels.level(3), fixed(0));
        assertEquals("released gargantuar", game.breakVase(2,4,0));
        assertEquals(1, game.getReleasedThreats());
    }

    @Test void droppedPacketsExpireAtConfiguredTick() {
        VasebreakerLevel level = VasebreakerLevels.level(1);
        VaseBreaker game = new VaseBreaker(level, fixed(0));
        game.breakVase(2,4,10);
        assertEquals(0, game.expirePackets(10 + level.getPacketLifetimeTicks() - 1));
        assertEquals(1, game.expirePackets(10 + level.getPacketLifetimeTicks()));
        assertTrue(game.getPackets().isEmpty());
    }

    @Test void winRequiresEveryVaseAndEveryReleasedThreatResolved() {
        VasebreakerLevel level = new VasebreakerLevel(1, 10, java.util.List.of(
                new Vase("a",0,0,VaseType.NORMAL,model.enums.VaseContentType.EMPTY,null,null),
                new Vase("b",0,1,VaseType.NORMAL,model.enums.VaseContentType.ZOMBIE,null,model.enums.ZombieType.NORMAL)));
        VaseBreaker game = new VaseBreaker(level, fixed(0));
        game.breakVase(0,0,0);
        game.breakVase(0,1,0);
        assertFalse(game.isWon());
        game.resolveThreat();
        assertTrue(game.isWon());
    }

    @Test void standardLawnLossTransitionsSessionToLost() {
        VasebreakerStrategy strategy = new VasebreakerStrategy(1);
        MinigameDefinition definition = new MinigameCatalog().get(MinigameId.VASEBREAKER);
        MinigameSession session = new MinigameSession(definition, definition.level(1), strategy, null, null);
        session.start();
        session.lose();
        assertEquals(MinigameRunState.LOST, session.getState());
    }

    private static RandomSource fixed(int value) {
        return new RandomSource() {
            public int nextInt(int bound) { return Math.floorMod(value, bound); }
            public double nextDouble() { return 0; }
        };
    }
}
