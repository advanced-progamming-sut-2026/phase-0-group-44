package model.miniGame;

import java.util.List;

public final class VasebreakerLevel {
    private final int level;
    private final long packetLifetimeTicks;
    private final List<Vase> vases;
    public VasebreakerLevel(int level, long packetLifetimeTicks, List<Vase> vases) {
        this.level = level; this.packetLifetimeTicks = packetLifetimeTicks; this.vases = List.copyOf(vases);
    }
    public int getLevel() { return level; }
    public long getPacketLifetimeTicks() { return packetLifetimeTicks; }
    public List<Vase> getVases() { return vases; }
}
