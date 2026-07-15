package model.sim.zombie;

/**
 * A record of a zombie dying this advance, so the controller can apply rewards
 * afterwards. Cheat kills are flagged so their rewards can be suppressed.
 */
public final class ZombieDeath {

    private final ZombieSpec spec;
    private final int tileX;
    private final int row;
    private final boolean glowing;
    private final boolean cheatKill;

    public ZombieDeath(ZombieSpec spec, int tileX, int row, boolean glowing, boolean cheatKill) {
        this.spec = spec;
        this.tileX = tileX;
        this.row = row;
        this.glowing = glowing;
        this.cheatKill = cheatKill;
    }

    public ZombieSpec getSpec() {
        return spec;
    }

    public int getTileX() {
        return tileX;
    }

    public int getRow() {
        return row;
    }

    public boolean isGlowing() {
        return glowing;
    }

    public boolean isCheatKill() {
        return cheatKill;
    }
}
