package model.miniGame;

import model.Result;
import model.enums.PlantType;
import model.enums.VaseContentType;
import model.enums.ZombieType;
import model.inGame.plant.PlantDefinition;
import model.inGame.plant.PlantRegistry;
import model.inGame.zombie.ZombieDefinition;
import model.inGame.zombie.ZombieRegistry;
import model.sim.GameOutcome;
import model.sim.SimulationSystem;
import model.sim.SimulationWorld;
import model.sim.TickContext;
import model.sim.board.DefaultPlantSpecSource;
import model.sim.board.PlantInstance;
import model.sim.board.PlantSpec;
import model.sim.board.PlantSpecSource;
import model.sim.board.Tile;
import model.sim.zombie.ZombieInstance;
import model.sim.zombie.ZombieSpec;
import util.RandomSource;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/** Attempt-local Vasebreaker board state and its isolated rule operations. */
public final class VaseBreakerState {
    private final VaseBreakerLevelRules rules;
    private final RandomSource random;
    private final PlantSpecSource plantSpecs;
    private final PlantRegistry plantRegistry;
    private final ZombieRegistry zombieRegistry;
    private final List<Vase> vases;
    private final List<SeedPacket> packets = new ArrayList<>();
    private int nextPacketId = 1;

    public VaseBreakerState(
            VaseBreakerLevelRules rules,
            RandomSource random,
            SimulationWorld world
    ) {
        if (rules == null || random == null || world == null) {
            throw new IllegalArgumentException("Vasebreaker state requires rules, randomness, and a world.");
        }
        this.rules = rules;
        this.random = random;
        this.plantRegistry = PlantRegistry.getDefault();
        this.zombieRegistry = ZombieRegistry.getDefault();
        this.plantSpecs = new DefaultPlantSpecSource(plantRegistry);
        this.vases = createVases(world);
    }

    public VaseBreakerLevelRules getRules() {
        return rules;
    }

    public List<Vase> getVases() {
        return Collections.unmodifiableList(vases);
    }

    public List<SeedPacket> getPackets() {
        return Collections.unmodifiableList(packets);
    }

    public int getBrokenVaseCount() {
        return (int) vases.stream().filter(Vase::isBroken).count();
    }

    public int getUnbrokenVaseCount() {
        return vases.size() - getBrokenVaseCount();
    }

    public Result<String> breakVase(MiniGameSession session, int x, int y) {
        Result<String> result = new Result<>();
        Vase vase = vaseAt(x, y);
        if (vase == null) {
            result.appendToMessage("no vase at (" + x + ", " + y + ")");
            return result;
        }
        if (!vase.breakOnce()) {
            result.appendToMessage("the vase at (" + x + ", " + y + ") is already broken");
            return result;
        }

        String message;
        switch (vase.getContentType()) {
            case EMPTY -> message = "broke vase " + vase.getId() + "; it was empty";
            case ZOMBIE -> {
                ZombieInstance zombie = releaseZombie(session.getSimulation().getWorld(), vase);
                message = "broke vase " + vase.getId() + "; released "
                        + zombie.getSpec().getName();
            }
            case SEED_PACKET -> {
                SeedPacket packet = dropPacket(session, vase);
                message = packetMessage("broke vase " + vase.getId() + "; dropped", packet);
            }
            case PLANT_VASE -> {
                SeedPacket packet = dropPacket(session, vase);
                message = packetMessage("broke Plant Vase " + vase.getId() + "; dropped", packet);
            }
            case GARGANTUAR_VASE -> {
                releaseZombie(session.getSimulation().getWorld(), vase);
                message = "broke Gargantuar Vase " + vase.getId() + "; released Gargantuar";
            }
            default -> throw new IllegalStateException("Unsupported vase content.");
        }
        result.setStatus(true);
        result.setData(message);
        result.appendToMessage(message);
        return result;
    }

    public Result<String> collectPacket(MiniGameSession session, int packetId) {
        Result<String> result = new Result<>();
        SeedPacket packet = packetById(packetId);
        if (packet == null) {
            result.appendToMessage("unknown seed packet " + packetId);
            return result;
        }
        if (!packet.collect(session.getSimulation().getCurrentTick())) {
            result.appendToMessage(packet.getState() == SeedPacket.State.EXPIRED
                    ? "seed packet " + packetId + " has expired"
                    : "seed packet " + packetId + " cannot be collected in state "
                    + packet.getState().name().toLowerCase());
            return result;
        }
        result.setStatus(true);
        result.setData(packet.getPlantType().name());
        result.appendToMessage("collected seed packet " + packetId + " for "
                + packet.getPlantType().name());
        return result;
    }

    public Result<String> plantPacket(MiniGameSession session, int packetId, int x, int y) {
        Result<String> result = new Result<>();
        SeedPacket packet = packetById(packetId);
        if (packet == null) {
            result.appendToMessage("unknown seed packet " + packetId);
            return result;
        }
        packet.expireIfDue(session.getSimulation().getCurrentTick());
        if (packet.getState() == SeedPacket.State.EXPIRED) {
            result.appendToMessage("seed packet " + packetId + " has expired");
            return result;
        }
        if (packet.getState() != SeedPacket.State.COLLECTED) {
            result.appendToMessage("collect seed packet " + packetId + " before planting it");
            return result;
        }

        SimulationWorld world = session.getSimulation().getWorld();
        Tile tile = world.getBoard().tileAt(x, y);
        if (tile == null) {
            result.appendToMessage("invalid tile");
            return result;
        }
        if (tile.hasAnyPlant() || tile.isGravestone() || tile.isFrozen()) {
            result.appendToMessage("this tile does not permit that plant");
            return result;
        }
        PlantSpec spec = plantSpecs.specOf(packet.getPlantType());
        if (spec == null || spec.providesSupport() || spec.isWaterCapable()) {
            result.appendToMessage("this temporary packet cannot be planted on normal Vasebreaker ground");
            return result;
        }

        if (!packet.plant(session.getSimulation().getCurrentTick())) {
            result.appendToMessage("seed packet " + packetId + " is no longer available");
            return result;
        }
        PlantInstance plant = new PlantInstance(spec, x, y);
        tile.setStackedPlant(plant);
        world.addPlant(plant);
        result.setStatus(true);
        result.setData(packet.getPlantType().name());
        result.appendToMessage("planted one-use " + packet.getPlantType().name()
                + " packet " + packetId + " at (" + x + ", " + y + ")");
        return result;
    }

    public String status(MiniGameSession session) {
        long tick = session.getSimulation().getCurrentTick();
        StringBuilder output = new StringBuilder();
        output.append("Vases:\n");
        for (Vase vase : vases) {
            output.append('#').append(vase.getId())
                    .append(" at (").append(vase.getX()).append(", ")
                    .append(vase.getY()).append("): ");
            if (vase.isBroken()) {
                output.append("broken");
            } else if (vase.getContentType() == VaseContentType.PLANT_VASE) {
                output.append("Plant Vase");
            } else if (vase.getContentType() == VaseContentType.GARGANTUAR_VASE) {
                output.append("Gargantuar Vase");
            } else {
                // Normal vase contents stay hidden until the vase is broken.
                output.append("normal vase");
            }
            output.append('\n');
        }
        output.append("Packets:\n");
        if (packets.isEmpty()) {
            output.append("none\n");
        } else {
            for (SeedPacket packet : packets) {
                output.append('#').append(packet.getId()).append(' ')
                        .append(packet.getPlantType().name())
                        .append(" at (").append(packet.getDropX()).append(", ")
                        .append(packet.getDropY()).append(") — ")
                        .append(packet.getState().name().toLowerCase())
                        .append(" — expires at tick ").append(packet.getExpiresAtTick())
                        .append('\n');
            }
        }
        output.append("Summary: vases ").append(getBrokenVaseCount()).append('/')
                .append(vases.size()).append("; threats ")
                .append(liveThreatCount(session.getSimulation().getWorld()))
                .append("; active packets ")
                .append(packets.stream().filter(SeedPacket::isAvailable).count())
                .append("; tick ").append(tick);
        return output.toString();
    }

    public GameOutcome evaluate(SimulationWorld world) {
        if (world.getOutcome() == GameOutcome.LOST) {
            return GameOutcome.LOST;
        }
        if (getUnbrokenVaseCount() == 0 && liveThreatCount(world) == 0) {
            world.setOutcome(GameOutcome.WON);
            return GameOutcome.WON;
        }
        return GameOutcome.RUNNING;
    }

    public SimulationSystem tickSystem() {
        return context -> {
            expirePackets(context);
            attackWithPlants(context);
        };
    }

    private List<Vase> createVases(SimulationWorld world) {
        List<Content> contents = new ArrayList<>();
        add(contents, rules.getEmptyVases(), VaseContentType.EMPTY);
        add(contents, rules.getZombieVases(), VaseContentType.ZOMBIE);
        add(contents, rules.getPacketVases(), VaseContentType.SEED_PACKET);
        add(contents, rules.getPlantVases(), VaseContentType.PLANT_VASE);
        add(contents, rules.getGargantuarVases(), VaseContentType.GARGANTUAR_VASE);
        shuffle(contents);

        List<Vase> result = new ArrayList<>();
        for (int index = 0; index < contents.size(); index++) {
            int x = world.getColumns() - 1 - index / world.getRows();
            int y = index % world.getRows();
            if (x < 1) {
                throw new IllegalArgumentException("Vase count does not fit the configured board.");
            }
            Content content = contents.get(index);
            PlantType plant = null;
            ZombieType zombie = null;
            if (content.type == VaseContentType.SEED_PACKET
                    || content.type == VaseContentType.PLANT_VASE) {
                plant = pick(rules.getPlantPool());
            } else if (content.type == VaseContentType.ZOMBIE) {
                zombie = pick(rules.getZombiePool());
            } else if (content.type == VaseContentType.GARGANTUAR_VASE) {
                zombie = ZombieType.GARGANTUAR;
            }
            result.add(new Vase(index + 1, x, y, content.type, plant, zombie));
        }
        result.sort(Comparator.comparingInt(Vase::getId));
        return result;
    }

    private void add(List<Content> contents, int count, VaseContentType type) {
        for (int i = 0; i < count; i++) {
            contents.add(new Content(type));
        }
    }

    private void shuffle(List<Content> values) {
        for (int i = values.size() - 1; i > 0; i--) {
            int other = random.nextInt(i + 1);
            Content value = values.get(i);
            values.set(i, values.get(other));
            values.set(other, value);
        }
    }

    private <T> T pick(List<T> values) {
        return values.get(random.nextInt(values.size()));
    }

    private Vase vaseAt(int x, int y) {
        for (Vase vase : vases) {
            if (vase.getX() == x && vase.getY() == y) {
                return vase;
            }
        }
        return null;
    }

    private SeedPacket packetById(int id) {
        for (SeedPacket packet : packets) {
            if (packet.getId() == id) {
                return packet;
            }
        }
        return null;
    }

    private SeedPacket dropPacket(MiniGameSession session, Vase vase) {
        long expires = session.getSimulation().getCurrentTick() + rules.getPacketLifetimeTicks();
        SeedPacket packet = new SeedPacket(
                nextPacketId++, vase.getPlantType(), vase.getX(), vase.getY(), expires);
        packets.add(packet);
        return packet;
    }

    private String packetMessage(String prefix, SeedPacket packet) {
        return prefix + " packet " + packet.getId() + " (" + packet.getPlantType().name()
                + "), expires at tick " + packet.getExpiresAtTick();
    }

    private ZombieInstance releaseZombie(SimulationWorld world, Vase vase) {
        ZombieDefinition definition = zombieRegistry.requireMandatory(vase.getZombieType());
        double x = Math.min(world.getColumns() - 0.01, vase.getX() + 0.75);
        ZombieInstance zombie = new ZombieInstance(ZombieSpec.fromDefinition(definition), x, vase.getY());
        world.addZombie(zombie);
        return zombie;
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

    private void expirePackets(TickContext context) {
        for (SeedPacket packet : packets) {
            if (packet.expireIfDue(context.getCurrentTick() + 1)) {
                context.emit("seed packet " + packet.getId() + " expired");
            }
        }
    }

    private void attackWithPlants(TickContext context) {
        if (context.getCurrentTick() % TickContext.TICKS_PER_SECOND != 0) {
            return;
        }
        for (Object damageable : new ArrayList<>(context.getWorld().getPlants())) {
            if (!(damageable instanceof PlantInstance plant) || !plant.isActive()) {
                continue;
            }
            PlantDefinition definition = plantRegistry.findByType(plant.getType());
            if (definition == null || definition.getDamage() <= 0) {
                continue;
            }
            ZombieInstance target = nearestThreat(context.getWorld(), plant);
            if (target == null) {
                continue;
            }
            int damage = definition.getDamage()
                    * Math.max(1, definition.getDamageProfile().getProjectileCount());
            target.takeDamage(damage);
            context.emit(plant.getType().name() + " hit " + target.getSpec().getName()
                    + " for " + damage);
        }
    }

    private ZombieInstance nearestThreat(SimulationWorld world, PlantInstance plant) {
        ZombieInstance nearest = null;
        for (ZombieInstance zombie : world.getZombieInstances()) {
            if (zombie.isDead() || zombie.getRow() != plant.getTileY()
                    || zombie.getX() < plant.getTileX()) {
                continue;
            }
            if (nearest == null || zombie.getX() < nearest.getX()) {
                nearest = zombie;
            }
        }
        return nearest;
    }

    private static final class Content {
        private final VaseContentType type;

        private Content(VaseContentType type) {
            this.type = type;
        }
    }
}
