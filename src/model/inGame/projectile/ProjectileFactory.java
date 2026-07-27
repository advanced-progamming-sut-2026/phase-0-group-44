package model.inGame.projectile;

import model.enums.PlantTag;
import model.inGame.plant.Plant;
import model.inGame.zombie.Zombie;

public class ProjectileFactory {
    public static final double DEFAULT_SPEED = 6.0;

    public Projectile direct(Plant plant, int row, int direction, int damage,
                             ProjectileEffect effect, double range) {
        return create(plant, row, direction, damage, ProjectileType.DIRECT,
                effect, 1, range, null, 0.0);
    }

    public Projectile piercing(Plant plant, int row, int direction, int damage,
                               ProjectileEffect effect, int maxHits, double range) {
        return create(plant, row, direction, damage, ProjectileType.PIERCING,
                effect, maxHits, range, null, 0.0);
    }

    public Projectile lobbed(Plant plant, Zombie target, int damage,
                             ProjectileEffect effect) {
        double distance = Math.max(1.0, Math.abs(target.getX() - plant.getPosition().getColumn()));
        return create(plant, plant.getPosition().getRow(), 1, damage,
                ProjectileType.LOBBED, effect, 1, distance, target,
                Math.max(0.3, distance / DEFAULT_SPEED));
    }

    public Projectile lobbedAt(Plant plant, int row, double landingX, int damage,
                               ProjectileEffect effect) {
        double spawnX = plant.getPosition().getColumn() + 0.55;
        double range = Math.max(1.0, landingX - spawnX);
        return create(plant, row, 1, damage, ProjectileType.LOBBED, effect, 1, range, null,
                Math.max(0.3, range / DEFAULT_SPEED));
    }

    public Projectile homing(Plant plant, Zombie target, int damage,
                             ProjectileEffect effect) {
        return create(plant, plant.getPosition().getRow(), 1, damage,
                ProjectileType.HOMING, effect, 1, 20.0, target, 0.5);
    }

    public Projectile bouncing(Plant plant, int row, int damage,
                               ProjectileEffect effect, int maxHits, double range) {
        return create(plant, row, 1, damage, ProjectileType.BOUNCING,
                effect, maxHits, range, null, 0.0);
    }

    public Projectile create(Plant plant, int row, int direction, int damage,
                             ProjectileType type, ProjectileEffect effect,
                             int maxHits, double range, Zombie target,
                             double flightTime) {
        if (plant == null || plant.getPosition() == null) {
            throw new IllegalArgumentException("Projectile source must be placed on the board.");
        }
        return new Projectile(
                plant.getEffectiveType(),
                row,
                plant.getPosition().getColumn() + (direction >= 0 ? 0.55 : -0.05),
                direction,
                damage,
                DEFAULT_SPEED,
                type,
                effect,
                maxHits,
                range,
                target,
                flightTime,
                plant.getEffectiveDefinition().hasTag(PlantTag.PEA)
        );
    }
}
