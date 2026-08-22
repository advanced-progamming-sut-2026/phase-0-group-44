package model.inGame.plant;

import model.GameEngine;
import model.Position;
import model.enums.DamageType;
import model.enums.PlantType;
import model.inGame.projectile.HypnosisEffect;
import model.inGame.projectile.NormalEffect;
import model.inGame.projectile.PoisonEffect;
import model.inGame.projectile.ProjectileFactory;
import model.inGame.zombie.Zombie;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Canonical blue-row plant mechanics kept separate from mandatory behaviors. */
public final class BonusPlantBehavior extends AbstractTimedBehavior {
    public enum Mode {
        PEA_POD,
        CAULIPOWER,
        ELECTRIC_BLUEBERRY,
        STARFRUIT,
        GOO_PEASHOOTER,
        GRAPESHOT,
        CHOMPER,
        WASABI_WHIP,
        KIWIBEAST,
        SWEET_POTATO,
        HYPNO_SHROOM,
        CAT_TAIL
    }

    private static final int MAX_PEA_POD_HEADS = 5;
    private static final int GRAPESHOT_GRAPES = 8;
    private static final double CHOMPER_DIGEST_SECONDS = 40.0;
    private final Mode mode;
    private final ProjectileFactory projectiles = new ProjectileFactory();
    private static final double GRAPESHOT_IDLE_SECONDS = 0.8;
    private static final double GRAPESHOT_ATTACK_SECONDS = 0.6;

    public BonusPlantBehavior(Mode mode) {
        this.mode = mode;
    }

    @Override
    public void onPlant(Plant plant, GameEngine engine) {
        switch (mode) {
            case PEA_POD -> plant.putState("PEA_POD_HEADS", 1);
            case KIWIBEAST -> plant.putState("KIWIBEAST_STAGE", 1);
            case GRAPESHOT -> plant.putState("ACTIVE_ZERO_HP", true);
            default -> {
                // Remaining bonus plants initialize lazily on their first tick.
            }
        }
    }

    @Override
    public void tick(Plant plant, GameEngine engine, double deltaSeconds) {
        switch (mode) {
            case PEA_POD -> tickPeaPod(plant, engine, deltaSeconds);
            case CAULIPOWER -> tickCaulipower(plant, engine, deltaSeconds);
            case ELECTRIC_BLUEBERRY -> tickElectricBlueberry(plant, engine, deltaSeconds);
            case STARFRUIT -> tickStarfruit(plant, engine, deltaSeconds);
            case GOO_PEASHOOTER -> tickGooPeashooter(plant, engine, deltaSeconds);
            case CHOMPER -> tickChomper(plant, engine, deltaSeconds);
            case WASABI_WHIP -> tickWasabiWhip(plant, engine, deltaSeconds);
            case KIWIBEAST -> tickKiwibeast(plant, engine, deltaSeconds);
            case SWEET_POTATO -> tickSweetPotato(plant, engine);
            case HYPNO_SHROOM, CAT_TAIL -> tickModifier(plant, engine, deltaSeconds);
            case GRAPESHOT -> tickGrapeshot(plant, engine);
        }
    }

    private void tickGrapeshot(Plant plant, GameEngine engine) {
        double age = plant.getAgeSeconds();
        boolean justStartedAttack = false;

        if (age >= GRAPESHOT_IDLE_SECONDS && !plant.getBooleanState("GRAPESHOT_ATTACK_STARTED")) {
            plant.markAttacked();
            plant.putState("GRAPESHOT_ATTACK_STARTED", true);
            justStartedAttack = true;
            System.out.println("[Grapeshot] attack started at age=" + age);
        }

        if (!justStartedAttack && age >= GRAPESHOT_IDLE_SECONDS + GRAPESHOT_ATTACK_SECONDS) {
            System.out.println("[Grapeshot] detonating at age=" + age);
            detonateGrapeshot(plant, engine);
        }
    }
    private void tickPeaPod(Plant plant, GameEngine engine, double deltaSeconds) {
        if (engine.getFirstZombieAhead(plant, 20.0) == null
                || !ready(plant, deltaSeconds, plant.getStats().getActionInterval())) {
            return;
        }
        plant.markAttacked();
        firePeas(plant, engine, peaPodHeads(plant), boostedDamage(plant, engine));
    }

    private void tickCaulipower(Plant plant, GameEngine engine, double deltaSeconds) {
        if (engine.getZombies().isEmpty()
                || !ready(plant, deltaSeconds, plant.getStats().getActionInterval())) {
            return;
        }
        Zombie target = randomHostile(engine);
        if (target != null) {
            plant.markAttacked();
            engine.spawnProjectile(projectiles.homing(plant, target, 0, new HypnosisEffect()));
        }
    }

    private void tickElectricBlueberry(Plant plant, GameEngine engine, double deltaSeconds) {
        if (engine.getZombies().isEmpty()
                || !ready(plant, deltaSeconds, plant.getStats().getActionInterval())) {
            return;
        }
        Zombie target = randomHostile(engine);
        if (target != null) {
            plant.markAttacked();
            engine.spawnProjectile(projectiles.homing(
                    plant, target, boostedDamage(plant, engine), new NormalEffect()));
        }
    }

    private void tickStarfruit(Plant plant, GameEngine engine, double deltaSeconds) {
        if (engine.getZombies().isEmpty()
                || !ready(plant, deltaSeconds, plant.getStats().getActionInterval())) {
            return;
        }
        plant.markAttacked();
        fireStar(plant, engine, boostedDamage(plant, engine));
    }

    private void tickGooPeashooter(Plant plant, GameEngine engine, double deltaSeconds) {
        if (engine.getFirstZombieAhead(plant, 20.0) == null
                || !ready(plant, deltaSeconds, plant.getStats().getActionInterval())) {
            return;
        }
        int poisonTick = 5 + (int) plant.getStats().getSpecial("DAMAGE_PER_TICK", 0);
        plant.markAttacked();
        engine.spawnProjectile(projectiles.direct(
                plant,
                plant.getPosition().getRow(),
                1,
                boostedDamage(plant, engine),
                new PoisonEffect(poisonTick, 5.0),
                20.0));
    }

    private void detonateGrapeshot(Plant plant, GameEngine engine) {
        int damage = boostedDamage(plant, engine);
        engine.damageArea(plant.getPosition(), 1, 1, damage, DamageType.NORMAL);
        int bounces = 1 + (int) plant.getStats().getSpecial("BOUNCES", 0);
        for (int i = 0; i < GRAPESHOT_GRAPES; i++) {
            int rowOffset = i % 3 - 1;
            int row = Math.max(0, Math.min(
                    engine.getGameMap().getRows() - 1,
                    plant.getPosition().getRow() + rowOffset));
            engine.spawnProjectile(projectiles.bouncing(
                    plant, row, Math.max(1, damage / 6), new NormalEffect(), 1 + bounces, 30.0));
        }
        plant.expire(engine);
    }

    private void tickChomper(Plant plant, GameEngine engine, double deltaSeconds) {
        double digest = Math.max(1.0, CHOMPER_DIGEST_SECONDS
                + plant.getStats().getSpecial("DIGEST_DURATION", 0));
        double digestUntil = plant.getState("DIGEST_UNTIL", Double.class, 0.0);
        if (plant.getAgeSeconds() < digestUntil) {
            return;
        }
        Zombie target = firstContact(plant, engine, 1);
        if (target == null || !ready(plant, deltaSeconds, 0.25)) {
            return;
        }
        System.out.println("[Chomper] biting " + target.getName());
        plant.markAttacked();
        target.receiveDamage(Integer.MAX_VALUE, DamageType.TRUE, engine);
        plant.putState("DIGEST_UNTIL", plant.getAgeSeconds() + digest);
        engine.recordEvent("Chomper swallowed " + target.getName() + ".");
    }
    private void tickWasabiWhip(Plant plant, GameEngine engine, double deltaSeconds) {
        if (!hasLaneContact(plant, engine, 1)
                || !ready(plant, deltaSeconds, plant.getStats().getActionInterval())) {
            return;
        }
        plant.markAttacked();
        for (Zombie zombie : engine.getZombiesInArea(plant.getPosition(), 0, 1)) {
            if (zombie.getRow() == plant.getPosition().getRow()) {
                zombie.receiveDamage(boostedDamage(plant, engine), DamageType.FIRE, engine);
                zombie.onFireHit(engine);
            }
        }
    }

    private void tickKiwibeast(Plant plant, GameEngine engine, double deltaSeconds) {
        int stage = plant.getAgeSeconds() >= 72.0 ? 3 : plant.getAgeSeconds() >= 24.0 ? 2 : 1;
        plant.putState("KIWIBEAST_STAGE", stage);
        if (engine.getZombiesInArea(plant.getPosition(), 1, 1).isEmpty()
                || !ready(plant, deltaSeconds, plant.getStats().getActionInterval())) {
            return;
        }
        int base = switch (stage) {
            case 3 -> 45;
            case 2 -> 30;
            default -> 15;
        };
        int upgraded = base + (plant.getStats().getDamage() - 15);
        plant.markAttacked();
        engine.damageArea(plant.getPosition(), 1, 1, upgraded, DamageType.NORMAL);
    }

    private void tickSweetPotato(Plant plant, GameEngine engine) {
        for (Zombie zombie : engine.getZombies()) {
            if (zombie.isDead() || zombie.isHypnotized()) {
                continue;
            }
            if (Math.abs(zombie.getRow() - plant.getPosition().getRow()) == 1
                    && Math.abs(zombie.getX() - (plant.getPosition().getColumn() + 0.5)) <= 3.0) {
                zombie.moveToRow(plant.getPosition().getRow());
            }
        }
    }

    private void tickModifier(Plant plant, GameEngine engine, double deltaSeconds) {
        if (mode == Mode.HYPNO_SHROOM) {
            return;
        }
        if (engine.getZombies().isEmpty()
                || !ready(plant, deltaSeconds, plant.getStats().getActionInterval())) {
            return;
        }
        Zombie target = engine.findNearestZombie(
                plant.getPosition().getRow(), plant.getPosition().getColumn(), true);
        if (target != null) {
            plant.markAttacked();
            engine.spawnProjectile(projectiles.homing(
                    plant, target, boostedDamage(plant, engine), new NormalEffect()));
        }
    }

    @Override
    public void onDamaged(Plant plant, GameEngine engine, Zombie attacker, int damageTaken) {
        if (mode == Mode.HYPNO_SHROOM && attacker != null && !attacker.isDead()) {
            attacker.hypnotize();
            engine.recordEvent(attacker.getName() + " ate Hypno-shroom and was hypnotized.");
            plant.expire(engine);
        }
    }

    @Override
    public void onPlantFood(Plant plant, GameEngine engine) {
        switch (mode) {
            case PEA_POD -> firePeas(plant, engine, peaPodHeads(plant),
                    boostedDamage(plant, engine) * 20);
            case CAULIPOWER -> hypnotizeRandom(engine, 3);
            case ELECTRIC_BLUEBERRY -> strikeRandom(plant, engine, 3);
            case STARFRUIT -> {
                for (int i = 0; i < 5; i++) {
                    fireStar(plant, engine, boostedDamage(plant, engine));
                }
            }
            case GOO_PEASHOOTER -> {
                for (int i = 0; i < 5; i++) {
                    tickGooPeashooter(plant, engine, plant.getStats().getActionInterval());
                }
            }
            case CHOMPER -> {
                for (Zombie zombie : engine.getRandomZombies(3)) {
                    zombie.receiveDamage(Integer.MAX_VALUE, DamageType.TRUE, engine);
                }
                plant.putState("DIGEST_UNTIL", plant.getAgeSeconds());
            }
            case WASABI_WHIP -> engine.damageArea(
                    plant.getPosition(), 1, 1, boostedDamage(plant, engine) * 4, DamageType.FIRE);
            case KIWIBEAST -> engine.damageArea(
                    plant.getPosition(), 2, 2, boostedDamage(plant, engine) * 6, DamageType.NORMAL);
            case SWEET_POTATO -> {
                for (Zombie zombie : engine.getZombies()) {
                    if (!zombie.isDead() && !zombie.isHypnotized()
                            && Math.abs(zombie.getRow() - plant.getPosition().getRow()) <= 2
                            && Math.abs(zombie.getX() - plant.getPosition().getColumn()) <= 4.0) {
                        zombie.moveToRow(plant.getPosition().getRow());
                    }
                }
                plant.healToFull();
            }
            case HYPNO_SHROOM -> transformNearestToGargantuar(plant, engine);
            case CAT_TAIL -> {
                for (int i = 0; i < 8; i++) {
                    tickModifier(plant, engine, plant.getStats().getActionInterval());
                }
            }
            case GRAPESHOT -> {
                // Instant-use plant is already gone.
            }
        }
    }

    private void firePeas(Plant plant, GameEngine engine, int count, int damage) {
        for (int i = 0; i < count; i++) {
            engine.spawnProjectile(projectiles.direct(
                    plant, plant.getPosition().getRow(), 1, damage, new NormalEffect(), 20.0));
        }
    }

    private void fireStar(Plant plant, GameEngine engine, int damage) {
        int row = plant.getPosition().getRow();
        engine.spawnProjectile(projectiles.direct(plant, row, 1, damage, new NormalEffect(), 20.0));
        for (int adjacent : new int[]{row - 1, row + 1}) {
            if (adjacent >= 0 && adjacent < engine.getGameMap().getRows()) {
                engine.spawnProjectile(projectiles.direct(
                        plant, adjacent, 1, damage, new NormalEffect(), 20.0));
                engine.spawnProjectile(projectiles.direct(
                        plant, adjacent, -1, damage, new NormalEffect(), 20.0));
            }
        }
    }

    private int peaPodHeads(Plant plant) {
        return Math.max(1, Math.min(MAX_PEA_POD_HEADS,
                plant.getState("PEA_POD_HEADS", Integer.class, 1)));
    }

    private Zombie randomHostile(GameEngine engine) {
        List<Zombie> hostiles = new ArrayList<>();
        for (Zombie zombie : engine.getZombies()) {
            if (!zombie.isDead() && !zombie.isHypnotized()) {
                hostiles.add(zombie);
            }
        }
        return hostiles.isEmpty() ? null : hostiles.get(engine.getRandom().nextInt(hostiles.size()));
    }

    private void hypnotizeRandom(GameEngine engine, int count) {
        for (Zombie zombie : engine.getRandomZombies(count)) {
            zombie.hypnotize();
        }
    }

    private void strikeRandom(Plant plant, GameEngine engine, int count) {
        for (Zombie zombie : engine.getRandomZombies(count)) {
            zombie.receiveDamage(boostedDamage(plant, engine), DamageType.TRUE, engine);
        }
    }

    private Zombie firstContact(Plant plant, GameEngine engine, int radius) {
        List<Zombie> candidates = new ArrayList<>(
                engine.getZombiesInArea(plant.getPosition(), 0, radius));
        candidates.sort(Comparator.comparingDouble(Zombie::getX));
        for (Zombie zombie : candidates) {
            if (zombie.getRow() == plant.getPosition().getRow()) {
                return zombie;
            }
        }
        return null;
    }

    private boolean hasLaneContact(Plant plant, GameEngine engine, int radius) {
        return firstContact(plant, engine, radius) != null;
    }

    private void transformNearestToGargantuar(Plant plant, GameEngine engine) {
        Zombie target = firstContact(plant, engine, 1);
        if (target == null) {
            target = engine.findNearestZombie(
                    plant.getPosition().getRow(), plant.getPosition().getColumn(), true);
        }
        if (target == null) {
            return;
        }
        Position position = new Position(target.getRow(), Math.max(0, target.getColumn()));
        target.receiveDamage(Integer.MAX_VALUE, DamageType.TRUE, engine);
        Zombie ally = engine.getZombieFactory().create(
                model.enums.ZombieType.GARGANTUAR,
                position.getRow(),
                Math.min(engine.getGameMap().getColumns() - 0.01, position.getColumn() + 0.5));
        ally.hypnotize();
        engine.addZombie(ally);
        plant.expire(engine);
    }
}
