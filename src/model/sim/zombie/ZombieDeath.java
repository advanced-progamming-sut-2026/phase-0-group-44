package model.sim.zombie;

/**
 * A record of a zombie dying this advance, so the controller can apply rewards
 * afterwards. Cheat kills are flagged so their rewards can be suppressed.
 */
public final class ZombieDeath {

    private final ZombieSpec spec;
    private final int tileX;
    private final int row;
    private final double deathX;
    private final boolean glowing;
    private final boolean cheatKill;

    public ZombieDeath(ZombieSpec spec, int tileX, int row, boolean glowing, boolean cheatKill) {
        this(spec, tileX, row, tileX + 0.5, glowing, cheatKill);
    }

    public ZombieDeath(
            ZombieSpec spec,
            int tileX,
            int row,
            double deathX,
            boolean glowing,
            boolean cheatKill
    ) {
        this.spec = spec;
        this.tileX = tileX;
        this.row = row;
        this.deathX = deathX;
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

    /** Continuous board-space x coordinate at the instant of death. */
    public double getDeathX() {
        return deathX;
    }

    public boolean isGlowing() {
        return glowing;
    }

    public boolean isCheatKill() {
        return cheatKill;
    }
}
