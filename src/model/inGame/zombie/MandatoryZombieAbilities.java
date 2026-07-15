package model.inGame.zombie;

import model.GameEngine;
import model.Position;
import model.enums.DamageType;
import model.enums.ObstacleType;
import model.inGame.projectile.ProjectileType;
import model.enums.ZombieType;
import model.inGame.plant.Plant;
import model.inGame.projectile.Projectile;

import java.util.ArrayList;
import java.util.List;

final class GargantuarAbility implements ZombieSpecialAbility {
    @Override
    public void onDamaged(Zombie zombie, GameEngine engine, int damage, DamageType damageType) {
        if (engine == null || zombie.getBooleanState("IMP_THROWN")
                || zombie.getHealth() > zombie.getMaxHealth() / 2) {
            return;
        }
        zombie.putState("IMP_THROWN", true);
        Zombie imp = engine.getZombieFactory().create(ZombieType.IMP, zombie.getRow(), 2.5);
        engine.addZombie(imp);
        engine.recordEvent("Gargantuar threw an Imp into the third column from the left.");
    }
}

final class AllStarAbility implements ZombieSpecialAbility {
    @Override
    public void tickBeforeMovement(Zombie zombie, GameEngine engine, double deltaSeconds) {
        if (!zombie.hasState("CHARGING")) {
            zombie.putState("CHARGING", true);
        }
        zombie.setRuntimeSpeedMultiplier(zombie.getBooleanState("CHARGING") ? 5.0 : 0.35);
    }
}

final class ParasolAbility implements ZombieSpecialAbility {
    @Override
    public ZombieProjectileDisposition projectileDisposition(
            Zombie zombie, Projectile projectile, GameEngine engine
    ) {
        return projectile.getType() == ProjectileType.LOBBED
                ? ZombieProjectileDisposition.BLOCK : ZombieProjectileDisposition.ACCEPT;
    }
}

final class TurquoiseAbility implements ZombieSpecialAbility {
    @Override
    public void tickBeforeMovement(Zombie zombie, GameEngine engine, double deltaSeconds) {
        if (!zombie.getBooleanState("STEALING")) {
            if (engine.findPlantWithin(zombie.getRow(), zombie.getX(), zombie.getDirection(), 4.0) == null) {
                return;
            }
            zombie.putState("STEALING", true);
            zombie.putState("STEAL_TIME", 0.0);
            zombie.putState("STEAL_TICK", 0.0);
        }
        double elapsed = zombie.getDoubleState("STEAL_TIME", 0.0) + deltaSeconds;
        double tick = zombie.getDoubleState("STEAL_TICK", 0.0) + deltaSeconds;
        while (tick >= 1.0 && elapsed <= 5.000001) {
            tick -= 1.0;
            int stolen = engine.removeSun(25);
            zombie.putState("STOLEN_SUN", zombie.getIntState("STOLEN_SUN", 0) + stolen);
        }
        zombie.putState("STEAL_TIME", elapsed);
        zombie.putState("STEAL_TICK", tick);
        if (elapsed >= 5.0) {
            engine.destroyPlantsAhead(zombie, 4);
            engine.recordEvent("Turquoise Zombie fired its four-tile laser.");
            zombie.putState("STEALING", false);
            zombie.putState("STEAL_TIME", 0.0);
        }
    }

    @Override
    public void onDeath(Zombie zombie, GameEngine engine) {
        int refund = zombie.getIntState("STOLEN_SUN", 0) / 2;
        engine.addSun(refund);
        if (refund > 0) {
            engine.recordEvent("Turquoise Zombie dropped " + refund + " stolen sun.");
        }
    }
}

final class ProspectorAbility implements ZombieSpecialAbility {
    @Override
    public void tickBeforeMovement(Zombie zombie, GameEngine engine, double deltaSeconds) {
        if (!zombie.hasState("DYNAMITE_LIT")) {
            zombie.putState("DYNAMITE_LIT", true);
        }
        if (!zombie.getBooleanState("DYNAMITE_LIT") || zombie.getBooleanState("REVERSED")) {
            return;
        }
        double timer = zombie.getDoubleState("DYNAMITE_TIMER", 0.0) + deltaSeconds;
        zombie.putState("DYNAMITE_TIMER", timer);
        if (timer >= 10.0) {
            zombie.setX(0.1);
            zombie.setDirection(1);
            zombie.putState("REVERSED", true);
            engine.recordEvent("Prospector dynamite exploded and reversed its direction.");
        }
    }

    @Override
    public void onIceHit(Zombie zombie, GameEngine engine) {
        if (!zombie.getBooleanState("REVERSED")) {
            zombie.putState("DYNAMITE_LIT", false);
            engine.recordEvent("Prospector dynamite was extinguished by ice.");
        }
    }
}

final class PianistAbility implements ZombieSpecialAbility {
    @Override
    public void tickAfterMovement(Zombie zombie, GameEngine engine, double deltaSeconds) {
        int activations = zombie.advanceTimerCount("PIANO_SWITCH", deltaSeconds, 4.0);
        for (int i = 0; i < activations; i++) {
            moveRandomZombie(zombie, engine);
        }
    }

    private void moveRandomZombie(Zombie pianist, GameEngine engine) {
        List<Zombie> candidates = new ArrayList<>();
        for (Zombie other : engine.getZombies()) {
            if (other != pianist && !other.isDead() && !other.isHypnotized()) {
                candidates.add(other);
            }
        }
        if (candidates.isEmpty()) {
            return;
        }
        Zombie target = candidates.get(engine.getRandom().nextInt(candidates.size()));
        List<Integer> rows = new ArrayList<>();
        if (target.getRow() > 0) {
            rows.add(target.getRow() - 1);
        }
        if (target.getRow() + 1 < engine.getGameMap().getRows()) {
            rows.add(target.getRow() + 1);
        }
        if (!rows.isEmpty()) {
            target.moveToRow(rows.get(engine.getRandom().nextInt(rows.size())));
            engine.recordEvent("Pianist moved " + target.getName() + " to an adjacent row.");
        }
    }
}

final class NewspaperAbility implements ZombieSpecialAbility {
    @Override
    public void onArmorBroken(Zombie zombie, GameEngine engine, String armorName) {
        if ("newspaper".equalsIgnoreCase(armorName)) {
            zombie.putState("ENRAGED", true);
            engine.recordEvent("Newspaper Zombie became enraged.");
        }
    }
}

final class BarrelRollerAbility implements ZombieSpecialAbility {
    @Override
    public void onArmorBroken(Zombie zombie, GameEngine engine, String armorName) {
        if (!"barrel".equalsIgnoreCase(armorName) || zombie.getBooleanState("IMPS_RELEASED")) {
            return;
        }
        zombie.putState("IMPS_RELEASED", true);
        for (int i = 0; i < 2; i++) {
            engine.addZombie(engine.getZombieFactory().create(
                    ZombieType.IMP, zombie.getRow(), Math.max(0.1, zombie.getX() + i * 0.1)));
        }
        engine.recordEvent("The barrel broke and released two Imps.");
    }

    @Override
    public void onDeath(Zombie zombie, GameEngine engine) {
        ZombieArmorPart barrel = zombie.findArmorPart("barrel");
        if (barrel == null || barrel.isBroken()) {
            return;
        }
        Position position = new Position(zombie.getRow(), Math.max(0,
                Math.min(engine.getGameMap().getColumns() - 1, zombie.getColumn())));
        engine.placeObstacle(position, ObstacleType.BARREL, barrel.getHealth(), "RELEASE_TWO_IMPS");
        engine.recordEvent("Barrel Roller died; its barrel remained as a projectile blocker.");
    }
}

final class RaAbility implements ZombieSpecialAbility {
    @Override
    public void tickBeforeMovement(Zombie zombie, GameEngine engine, double deltaSeconds) {
        int activations = zombie.advanceTimerCount("RA_STEAL", deltaSeconds, 1.0);
        for (int i = 0; i < activations; i++) {
            int stolen = engine.stealNearestGroundSun(zombie.getRow(), zombie.getX());
            if (stolen > 0) {
                zombie.putState("RA_STOLEN_SUN", zombie.getIntState("RA_STOLEN_SUN", 0) + stolen);
            }
        }
    }

    @Override
    public void onDeath(Zombie zombie, GameEngine engine) {
        int stolen = zombie.getIntState("RA_STOLEN_SUN", 0);
        engine.addSun(stolen);
        if (stolen > 0) {
            engine.recordEvent("Ra Zombie returned " + stolen + " stolen sun.");
        }
    }
}

final class ExplorerAbility implements ZombieSpecialAbility {
    @Override
    public void tickBeforeMovement(Zombie zombie, GameEngine engine, double deltaSeconds) {
        if (!zombie.hasState("TORCH_LIT")) {
            zombie.putState("TORCH_LIT", true);
        }
    }

    @Override
    public void onIceHit(Zombie zombie, GameEngine engine) {
        zombie.putState("TORCH_LIT", false);
        engine.recordEvent("Explorer torch was extinguished.");
    }

    @Override
    public void onFireHit(Zombie zombie, GameEngine engine) {
        zombie.putState("TORCH_LIT", true);
        engine.recordEvent("Explorer torch was relit.");
    }
}

final class TombraiserAbility implements ZombieSpecialAbility {
    @Override
    public void tickAfterMovement(Zombie zombie, GameEngine engine, double deltaSeconds) {
        int activations = zombie.advanceTimerCount("TOMBRAISER_BONES", deltaSeconds, 8.0);
        for (int i = 0; i < activations; i++) {
            createGraves(engine);
        }
    }

    private void createGraves(GameEngine engine) {
        List<Position> valid = new ArrayList<>(engine.emptyPositions(false));
        int created = 0;
        while (created < 2 && !valid.isEmpty()) {
            int index = engine.getRandom().nextInt(valid.size());
            Position position = valid.remove(index);
            engine.placeObstacle(position, ObstacleType.GRAVE, 700, "");
            created++;
        }
        if (created > 0) {
            engine.recordEvent("Tombraiser created " + created + " graves.");
        }
    }
}

final class HunterAbility implements ZombieSpecialAbility {
    @Override
    public void tickAfterMovement(Zombie zombie, GameEngine engine, double deltaSeconds) {
        int activations = zombie.advanceTimerCount("HUNTER_ICE", deltaSeconds, 3.0);
        for (int i = 0; i < activations; i++) {
            Plant target = engine.findNearestPlantInLane(zombie.getRow(), zombie.getX());
            if (target != null) {
                engine.addIceHit(target);
                engine.recordEvent("Hunter hit " + target.getEffectiveType() + " with ice.");
            }
        }
    }
}

final class SnorkelAbility implements ZombieSpecialAbility {
    @Override
    public void tickBeforeMovement(Zombie zombie, GameEngine engine, double deltaSeconds) {
        int column = Math.max(0, Math.min(engine.getGameMap().getColumns() - 1, zombie.getColumn()));
        boolean water = engine.getGameMap().getTile(zombie.getRow(), column)
                .getTerrain().requiresWaterCapablePlant();
        boolean eating = engine.findCollidingPlant(zombie) != null;
        zombie.putState("SUBMERGED", water && !eating);
    }

    @Override
    public ZombieProjectileDisposition projectileDisposition(
            Zombie zombie, Projectile projectile, GameEngine engine
    ) {
        if (zombie.getBooleanState("SUBMERGED") && projectile.getType() != ProjectileType.LOBBED) {
            return ZombieProjectileDisposition.BLOCK;
        }
        return ZombieProjectileDisposition.ACCEPT;
    }
}

final class OctopusAbility implements ZombieSpecialAbility {
    @Override
    public void tickAfterMovement(Zombie zombie, GameEngine engine, double deltaSeconds) {
        int activations = zombie.advanceTimerCount("OCTOPUS_THROW", deltaSeconds, 5.0);
        for (int i = 0; i < activations; i++) {
            Plant target = engine.findNearestPlantInLane(zombie.getRow(), zombie.getX());
            if (target != null && engine.applyOctopus(target)) {
                engine.recordEvent("Octopus Zombie disabled " + target.getEffectiveType() + ".");
            }
        }
    }
}
