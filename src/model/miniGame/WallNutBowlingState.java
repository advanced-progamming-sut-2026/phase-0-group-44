package model.miniGame;

import model.Result;
import model.enums.PlantType;
import model.enums.ZombieType;
import model.inGame.plant.PlantRegistry;
import model.inGame.zombie.ZombieDefinition;
import model.inGame.zombie.ZombieRegistry;
import model.sim.GameOutcome;
import model.sim.SimulationSystem;
import model.sim.SimulationWorld;
import model.sim.TickContext;
import model.sim.zombie.ZombieInstance;
import model.sim.zombie.ZombieSpec;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/** Attempt-local Wall-nut Bowling entities, conveyor, spawning, and collisions. */
public final class WallNutBowlingState {
    private static final double COLLISION_RADIUS = 0.45;

    private final WallNutBowlingLevelRules rules;
    private final ConveyorBelt conveyor;
    private final ZombieRegistry zombieRegistry;
    private final int normalZombieCollisionDamage;
    private final int explosionDamage;
    private final List<BowlingBall> balls = new ArrayList<>();
    private int nextBallId = 1;
    private int nextZombieIndex;
    private long nextZombieSpawnTick;

    public WallNutBowlingState(WallNutBowlingLevelRules rules, SimulationWorld world) {
        if (rules == null || world == null || rules.getRedLineColumn() >= world.getColumns()) {
            throw new IllegalArgumentException("Wall-nut Bowling state requires valid rules and a board.");
        }
        this.rules = rules;
        this.conveyor = new ConveyorBelt(
                rules.getConveyorCapacity(),
                rules.getInitialPackets(),
                rules.getConveyorIntervalTicks(),
                rules.getConveyorPattern());
        this.zombieRegistry = ZombieRegistry.getDefault();
        this.normalZombieCollisionDamage = zombieRegistry
                .requireMandatory(ZombieType.NORMAL).getHealth();
        this.explosionDamage = PlantRegistry.getDefault()
                .requireMandatory(PlantType.CHERRY_BOMB).getDamage();

        // The first configured threat is present when play starts; later threats
        // enter on the same deterministic simulation clock as movement.
        spawnNextZombie(world);
        this.nextZombieSpawnTick = rules.getZombieSpawnIntervalTicks();
    }

    public WallNutBowlingLevelRules getRules() {
        return rules;
    }

    public ConveyorBelt getConveyor() {
        return conveyor;
    }

    public List<BowlingBall> getBalls() {
        return Collections.unmodifiableList(balls);
    }

    public int getNormalZombieCollisionDamage() {
        return normalZombieCollisionDamage;
    }

    public int getExplosionDamage() {
        return explosionDamage;
    }

    public int getSpawnedZombieCount() {
        return nextZombieIndex;
    }

    public Result<String> plantFromConveyor(
            MiniGameSession session,
            int packetId,
            int x,
            int y
    ) {
        Result<String> result = new Result<>();
        SimulationWorld world = session.getSimulation().getWorld();
        if (!world.isInsideBoard(x, y)) {
            result.appendToMessage("the requested tile is outside the board");
            return result;
        }
        if (x > rules.getRedLineColumn()) {
            result.appendToMessage("bowling plants may only be placed from column 0 through the red line at column "
                    + rules.getRedLineColumn());
            return result;
        }
        if (activeBallAt(x, y) != null) {
            result.appendToMessage("a bowling plant is already on that launch tile");
            return result;
        }
        ConveyorPacket packet = conveyor.find(packetId);
        if (packet == null) {
            result.appendToMessage("unknown conveyor packet");
            return result;
        }
        if (!packet.isAvailable()) {
            result.appendToMessage("that conveyor packet was already used");
            return result;
        }
        packet = conveyor.take(packetId);
        BowlingBall ball = new BowlingBall(
                nextBallId++, packet.getPlantType(), x + 0.5, y,
                rules.getBallSpeedTilesPerSecond());
        balls.add(ball);
        String message = "planted " + packet.getPlantType().getDisplayName()
                + " from conveyor packet " + packetId + " at (" + x + ", " + y + ")";
        result.setStatus(true);
        result.setData(message);
        result.appendToMessage(message);
        return result;
    }

    public String status(MiniGameSession session) {
        SimulationWorld world = session.getSimulation().getWorld();
        StringBuilder output = new StringBuilder();
        output.append("Wall-nut Bowling level ").append(rules.getLevel()).append('\n')
                .append("red line: column ").append(rules.getRedLineColumn())
                .append("; legal launch columns: 0-").append(rules.getRedLineColumn()).append('\n')
                .append("sun: ").append(world.getSunBalance())
                .append("; sky sun: disabled\n")
                .append("Conveyor:\n");
        List<ConveyorPacket> available = conveyor.getAvailablePackets();
        if (available.isEmpty()) {
            output.append("none\n");
        } else {
            for (ConveyorPacket packet : available) {
                output.append('#').append(packet.getId()).append(' ')
                        .append(packet.getPlantType().getDisplayName()).append('\n');
            }
        }
        output.append("Bowling plants:\n");
        boolean anyBall = false;
        for (BowlingBall ball : balls) {
            if (!ball.isActive()) {
                continue;
            }
            anyBall = true;
            output.append('#').append(ball.getId()).append(' ')
                    .append(ball.getType().getDisplayName()).append(" at (")
                    .append(String.format(Locale.ROOT, "%.2f", ball.getX())).append(", ")
                    .append(String.format(Locale.ROOT, "%.2f", ball.getY())).append(") heading ")
                    .append(String.format(Locale.ROOT, "%.0f", ball.getHeadingDegrees()))
                    .append(" degrees\n");
        }
        if (!anyBall) {
            output.append("none\n");
        }
        output.append("Threats: spawned ").append(getSpawnedZombieCount()).append('/')
                .append(rules.getZombieCount()).append("; live ")
                .append(liveThreatCount(world)).append("; tick ")
                .append(session.getSimulation().getCurrentTick());
        return output.toString();
    }

    public GameOutcome evaluate(SimulationWorld world) {
        if (world.getOutcome() == GameOutcome.LOST) {
            return GameOutcome.LOST;
        }
        if (nextZombieIndex >= rules.getZombieCount() && liveThreatCount(world) == 0) {
            world.setOutcome(GameOutcome.WON);
            return GameOutcome.WON;
        }
        return GameOutcome.RUNNING;
    }

    public SimulationSystem tickSystem() {
        return context -> {
            if (!context.getWorld().isRunning()) {
                return;
            }
            conveyor.tick(context);
            spawnDueZombie(context);
            moveBowlingPlants(context);
        };
    }

    private void spawnDueZombie(TickContext context) {
        long completedTick = context.getCurrentTick() + 1;
        if (nextZombieIndex >= rules.getZombieCount() || completedTick < nextZombieSpawnTick) {
            return;
        }
        ZombieInstance zombie = spawnNextZombie(context.getWorld());
        nextZombieSpawnTick = completedTick + rules.getZombieSpawnIntervalTicks();
        context.emit("bowling threat entered: " + zombie.getSpec().getName()
                + " in row " + zombie.getRow());
    }

    private ZombieInstance spawnNextZombie(SimulationWorld world) {
        ZombieType type = rules.getZombieSequence().get(nextZombieIndex);
        ZombieDefinition definition = zombieRegistry.requireMandatory(type);
        int row = Math.floorMod(nextZombieIndex * 2 + rules.getLevel() - 1, world.getRows());
        ZombieInstance zombie = new ZombieInstance(
                ZombieSpec.fromDefinition(definition), world.getColumns() - 0.01, row);
        world.addZombie(zombie);
        nextZombieIndex++;
        return zombie;
    }

    private void moveBowlingPlants(TickContext context) {
        for (BowlingBall ball : new ArrayList<>(balls)) {
            if (!ball.isActive()) {
                continue;
            }
            moveOne(ball, context);
        }
    }

    private void moveOne(BowlingBall ball, TickContext context) {
        SimulationWorld world = context.getWorld();
        double oldX = ball.getX();
        double oldY = ball.getY();
        double distance = ball.getSpeedTilesPerSecond() / TickContext.TICKS_PER_SECOND;
        double dx = ball.getVerticalDirection() == 0
                ? distance : distance / Math.sqrt(2.0);
        double dy = ball.getVerticalDirection() * distance / Math.sqrt(2.0);
        double newX = oldX + dx;
        double newY = oldY + dy;

        if (ball.getType() == BowlingPlantType.BOWLING_WALL_NUT
                && ball.getVerticalDirection() != 0) {
            double maxY = world.getRows() - 1;
            if (newY < 0) {
                newY = -newY;
                ball.deflectAtBoundary(true);
                context.emit("Bowling Wall-nut " + ball.getId()
                        + " hit the top boundary and turned 90 degrees");
            } else if (newY > maxY) {
                newY = maxY - (newY - maxY);
                ball.deflectAtBoundary(false);
                context.emit("Bowling Wall-nut " + ball.getId()
                        + " hit the bottom boundary and turned 90 degrees");
            }
        }

        ball.setPosition(newX, newY);
        switch (ball.getType()) {
            case BOWLING_WALL_NUT -> collideBowlingWallNut(ball, oldX, oldY, context);
            case EXPLODE_O_NUT -> collideExplodeONut(ball, oldX, oldY, context);
            case GIANT_WALL_NUT -> collideGiantWallNut(ball, oldX, oldY, context);
        }
        if (ball.getX() >= world.getColumns()) {
            ball.deactivate();
            context.emit(ball.getType().getDisplayName() + " " + ball.getId()
                    + " left the lawn");
        }
    }

    private void collideBowlingWallNut(
            BowlingBall ball,
            double oldX,
            double oldY,
            TickContext context
    ) {
        ZombieInstance target = firstCollision(ball, oldX, oldY, context.getWorld());
        if (target == null) {
            return;
        }
        target.takeDamage(normalZombieCollisionDamage);
        ball.markContact(target);
        double before = ball.getHeadingDegrees();
        ball.deflectAfterContact(context.getWorld().getRows());
        double turn = Math.abs(ball.getHeadingDegrees() - before);
        context.emit("Bowling Wall-nut " + ball.getId() + " hit "
                + target.getSpec().getName() + " for " + normalZombieCollisionDamage
                + " and turned " + String.format(Locale.ROOT, "%.0f", turn) + " degrees");
    }

    private void collideExplodeONut(
            BowlingBall ball,
            double oldX,
            double oldY,
            TickContext context
    ) {
        ZombieInstance target = firstCollision(ball, oldX, oldY, context.getWorld());
        if (target == null) {
            return;
        }
        int centerX = (int) Math.floor(ball.getX());
        int centerY = (int) Math.round(ball.getY());
        int affected = 0;
        for (ZombieInstance zombie : context.getWorld().getZombieInstances()) {
            if (zombie.isDead()
                    || Math.abs(zombie.getTileX() - centerX) > 1
                    || Math.abs(zombie.getRow() - centerY) > 1) {
                continue;
            }
            zombie.takeDamage(explosionDamage);
            affected++;
        }
        ball.markContact(target);
        ball.deactivate();
        context.emit("Explode-o-nut " + ball.getId() + " exploded at ("
                + centerX + ", " + centerY + ") for " + explosionDamage
                + " damage in a 3x3 area; targets " + affected);
    }

    private void collideGiantWallNut(
            BowlingBall ball,
            double oldX,
            double oldY,
            TickContext context
    ) {
        List<ZombieInstance> collisions = collisions(ball, oldX, oldY, context.getWorld());
        for (ZombieInstance zombie : collisions) {
            zombie.kill();
            ball.markContact(zombie);
            context.emit("Giant Wall-nut " + ball.getId() + " crushed "
                    + zombie.getSpec().getName());
        }
    }

    private ZombieInstance firstCollision(
            BowlingBall ball,
            double oldX,
            double oldY,
            SimulationWorld world
    ) {
        List<ZombieInstance> collisions = collisions(ball, oldX, oldY, world);
        return collisions.isEmpty() ? null : collisions.get(0);
    }

    private List<ZombieInstance> collisions(
            BowlingBall ball,
            double oldX,
            double oldY,
            SimulationWorld world
    ) {
        List<ZombieInstance> result = new ArrayList<>();
        for (ZombieInstance zombie : world.getZombieInstances()) {
            if (zombie.isDead() || ball.hasContacted(zombie)
                    || !sweptCollision(oldX, oldY, ball.getX(), ball.getY(), zombie)) {
                continue;
            }
            result.add(zombie);
        }
        result.sort(Comparator.comparingDouble(ZombieInstance::getX));
        return result;
    }

    private boolean sweptCollision(
            double oldX,
            double oldY,
            double newX,
            double newY,
            ZombieInstance zombie
    ) {
        double minX = Math.min(oldX, newX) - COLLISION_RADIUS;
        double maxX = Math.max(oldX, newX) + COLLISION_RADIUS;
        if (zombie.getX() < minX || zombie.getX() > maxX) {
            return false;
        }
        double deltaX = newX - oldX;
        double progress = Math.abs(deltaX) < 0.000001
                ? 0.0 : (zombie.getX() - oldX) / deltaX;
        progress = Math.max(0.0, Math.min(1.0, progress));
        double yAtZombie = oldY + (newY - oldY) * progress;
        return Math.abs(yAtZombie - zombie.getRow()) <= COLLISION_RADIUS;
    }

    private BowlingBall activeBallAt(int x, int y) {
        for (BowlingBall ball : balls) {
            if (ball.isActive() && (int) Math.floor(ball.getX()) == x
                    && (int) Math.round(ball.getY()) == y) {
                return ball;
            }
        }
        return null;
    }

    private int liveThreatCount(SimulationWorld world) {
        int count = 0;
        for (ZombieInstance zombie : world.getZombieInstances()) {
            if (!zombie.isDead()) {
                count++;
            }
        }
        return count;
    }
}
