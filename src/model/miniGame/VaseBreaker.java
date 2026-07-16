package model.miniGame;

import model.enums.VaseContentType;
import model.enums.ZombieType;
import java.util.*;
import util.RandomSource;
import util.SeededRandomSource;
import model.enums.PlantType;

public final class VaseBreaker {
    private final VasebreakerLevel config;
    private final Map<String,Vase> vases = new LinkedHashMap<>();
    private final Map<String,SeedPacket> packets = new LinkedHashMap<>();
    private int releasedThreats;
    private int packetSequence;
    private final RandomSource random;
    private static final PlantType[] PLANT_VASE_POOL = {PlantType.PEASHOOTER, PlantType.WALL_NUT, PlantType.SNOW_PEA, PlantType.REPEATER, PlantType.MELON_PULT};

    public VaseBreaker(VasebreakerLevel config) { this(config, new SeededRandomSource(0L)); }
    public VaseBreaker(VasebreakerLevel config, RandomSource random) {
        this.config = config; this.random = Objects.requireNonNull(random);
        for (Vase vase : config.getVases()) vases.put(vase.getId(), vase);
    }
    public VasebreakerLevel getConfig() { return config; }
    public Collection<Vase> getVases() { return Collections.unmodifiableCollection(vases.values()); }
    public Collection<SeedPacket> getPackets() { return Collections.unmodifiableCollection(packets.values()); }
    public int getReleasedThreats() { return releasedThreats; }
    public boolean allVasesBroken() { return vases.values().stream().allMatch(Vase::isBroken); }
    public boolean isWon() { return allVasesBroken() && releasedThreats == 0; }

    public String breakVase(int row, int column, long tick) {
        Vase vase = vases.values().stream().filter(v -> v.getRow()==row && v.getColumn()==column).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No vase exists at that tile."));
        vase.breakOpen();
        return switch (vase.getContentType()) {
            case EMPTY -> "vase empty";
            case ZOMBIE, GARGANTUAR -> {
                releasedThreats++;
                ZombieType type = vase.getContentType()==VaseContentType.GARGANTUAR ? ZombieType.GARGANTUAR : vase.getZombieType();
                yield "released " + type.name().toLowerCase().replace('_',' ');
            }
            case PLANT_PACKET -> {
                String id = "packet-" + (++packetSequence);
                PlantType plant = vase.getType() == VaseType.PLANT
                        ? PLANT_VASE_POOL[random.nextInt(PLANT_VASE_POOL.length)] : vase.getPlantType();
                packets.put(id, new SeedPacket(id, plant, tick, config.getPacketLifetimeTicks()));
                yield "dropped " + id + " " + plant.name().toLowerCase().replace('_',' ');
            }
        };
    }

    public String plantPacket(String packetId, int row, int column, long tick) {
        SeedPacket packet = packets.get(packetId);
        if (packet == null) throw new IllegalArgumentException("Unknown seed packet.");
        if (row < 0 || row >= 5 || column < 0 || column >= 9) throw new IllegalArgumentException("Invalid lawn tile.");
        packet.use(tick);
        return "planted " + packet.getPlantType().name().toLowerCase().replace('_',' ') + " at " + row + " " + column;
    }

    public int expirePackets(long tick) {
        int before = packets.size();
        packets.entrySet().removeIf(e -> e.getValue().isExpired(tick));
        return before - packets.size();
    }

    public void resolveThreat() {
        if (releasedThreats <= 0) throw new IllegalStateException("No released threat remains.");
        releasedThreats--;
    }
}
