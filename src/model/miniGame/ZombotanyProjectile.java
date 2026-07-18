package model.miniGame;

/** A left-moving pea fired by a Peashooter Zombie. */
public final class ZombotanyProjectile {
    private double x;
    private final int row;
    private final int damage;
    private final double speedTilesPerSecond;
    private boolean active = true;

    public ZombotanyProjectile(
            double x,
            int row,
            int damage,
            double speedTilesPerSecond
    ) {
        if (row < 0 || damage <= 0 || speedTilesPerSecond <= 0) {
            throw new IllegalArgumentException("Invalid Zombotany projectile.");
        }
        this.x = x;
        this.row = row;
        this.damage = damage;
        this.speedTilesPerSecond = speedTilesPerSecond;
    }

    public double getX() { return x; }
    public int getRow() { return row; }
    public int getDamage() { return damage; }
    public boolean isActive() { return active; }

    public void advanceOneTick() {
        x -= speedTilesPerSecond / model.sim.TickContext.TICKS_PER_SECOND;
        if (x < 0) active = false;
    }

    public void deactivate() { active = false; }
}
