package model.inGame.zombie;

import model.GameEngine;
import model.inGame.plant.Plant;

/** Instant collision attack while a pushed machine/ice armor piece remains. */
final class ArmoredRamZombieAttack implements ZombieAttackComponent {
    private final String armorName;
    private final NormalZombieAttack fallback = new NormalZombieAttack();

    ArmoredRamZombieAttack(String armorName) {
        this.armorName = armorName;
    }

    @Override
    public boolean attack(Zombie zombie, GameEngine engine, double deltaSeconds) {
        Plant plant = engine.findCollidingPlant(zombie);
        Zombie hypnotized = engine.findCollidingHypnotizedZombie(zombie);
        ZombieArmorPart ram = zombie.findArmorPart(armorName);
        boolean activeRam = ram != null && !ram.isBroken();
        if (activeRam && plant != null) {
            plant.receiveDamage(Integer.MAX_VALUE, engine, zombie);
            engine.recordEvent(zombie.getName() + " crushed " + plant.getEffectiveType() + ".");
            return true;
        }
        if (activeRam && hypnotized != null) {
            hypnotized.receiveDamage(Integer.MAX_VALUE, model.enums.DamageType.TRUE, engine);
            engine.recordEvent(zombie.getName() + " crushed a hypnotized zombie.");
            return true;
        }
        return fallback.attack(zombie, engine, deltaSeconds);
    }
}
