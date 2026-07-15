package model.inGame.projectile;

import model.GameEngine;
import model.Position;
import model.enums.DamageType;
import model.inGame.zombie.Zombie;

public class AreaEffect implements ProjectileEffect {
    private final ProjectileEffect primary;
    private final int splashDamage;
    private final int rowRadius;
    private final int columnRadius;
    private final DamageType splashType;

    public AreaEffect(ProjectileEffect primary, int splashDamage,
                      int rowRadius, int columnRadius, DamageType splashType) {
        this.primary = primary;
        this.splashDamage = Math.max(0, splashDamage);
        this.rowRadius = Math.max(0, rowRadius);
        this.columnRadius = Math.max(0, columnRadius);
        this.splashType = splashType == null ? DamageType.NORMAL : splashType;
    }

    @Override
    public void apply(Projectile projectile, Zombie zombie, GameEngine engine) {
        primary.apply(projectile, zombie, engine);
        if (splashDamage <= 0) {
            return;
        }
        Position center = new Position(zombie.getRow(), Math.max(0, zombie.getColumn()));
        for (Zombie nearby : engine.getZombiesInArea(center, rowRadius, columnRadius)) {
            if (nearby != zombie && !nearby.isDead()) {
                nearby.takeDamage(splashDamage, splashType);
                if (splashType == DamageType.ICE) {
                    nearby.applySlow(5.0);
                } else if (splashType == DamageType.FIRE) {
                    nearby.thaw();
                }
            }
        }
    }
}
