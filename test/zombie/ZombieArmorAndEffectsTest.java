package zombie;

import model.GameEngine;
import model.enums.DamageType;
import model.enums.ZombieType;
import model.inGame.zombie.Zombie;
import model.inGame.zombie.ZombieArmorPart;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ZombieArmorAndEffectsTest {

    @Test
    void normalDamageConsumesArmorWhilePoisonBypassesIt() {
        GameEngine engine = new GameEngine();
        Zombie bucket = engine.spawnZombie(ZombieType.BUCKETHEAD, 0, 8.0);

        bucket.receiveDamage(100, DamageType.NORMAL, engine);
        assertEquals(bucket.getMaxHealth(), bucket.getHealth());
        assertEquals(1000, bucket.findArmorPart("bucket").getHealth());

        bucket.receiveDamage(50, DamageType.POISON, engine);
        assertEquals(bucket.getMaxHealth() - 50, bucket.getHealth());
        assertEquals(1000, bucket.findArmorPart("bucket").getHealth());
    }

    @Test
    void magnetRemovesBucketAndOnlyTheKnightHelmet() {
        GameEngine engine = new GameEngine();
        Zombie bucket = engine.spawnZombie(ZombieType.BUCKETHEAD, 0, 8.0);
        Zombie knight = engine.spawnZombie(ZombieType.KNIGHT, 1, 8.0);

        assertTrue(bucket.removeMetalArmor());
        assertTrue(bucket.findArmorPart("bucket").isBroken());

        assertTrue(knight.removeMetalArmor());
        assertTrue(knight.findArmorPart("helmet").isBroken());
        assertNull(knight.findArmorPart("shoulderArmor"));
        assertFalse(knight.removeMetalArmor());
    }

    @Test
    void frostbiteZombiesIgnoreFreezeAndDragonImpIgnoresFire() {
        GameEngine engine = new GameEngine();
        Zombie hunter = engine.spawnZombie(ZombieType.HUNTER, 0, 8.0);
        Zombie normal = engine.spawnZombie(ZombieType.NORMAL, 1, 8.0);
        Zombie dragon = engine.spawnZombie(ZombieType.DRAGON_IMP, 2, 8.0);

        hunter.applyFreeze(5.0);
        normal.applyFreeze(5.0);
        assertFalse(hunter.isFrozen());
        assertTrue(normal.isFrozen());

        int health = dragon.getHealth();
        dragon.receiveDamage(100, DamageType.FIRE, engine);
        assertEquals(health, dragon.getHealth());
        dragon.receiveDamage(100, DamageType.NORMAL, engine);
        assertEquals(health - 100, dragon.getHealth());
    }

    @Test
    void activeEffectsRetainAndReportRemainingDuration() {
        GameEngine engine = new GameEngine();
        Zombie zombie = engine.spawnZombie(ZombieType.NORMAL, 0, 8.0);

        zombie.applySlow(5.0);
        zombie.applyPoison(3, 4.0);
        engine.tick(1.0);

        assertEquals(4.0, zombie.getSlowedSeconds(), 0.0001);
        assertTrue(zombie.infoText().contains("chilled: 4.0s"));
        assertTrue(zombie.infoText().contains("poisoned: 3.0s"));
    }

    @Test
    void knightUsesOneSynchronizedArmorPool() {
        GameEngine engine = new GameEngine();
        Zombie knight = engine.spawnZombie(ZombieType.KNIGHT, 0, 8.0);

        assertEquals(1, knight.getArmorParts().size());
        assertEquals(1600, armor(knight, "helmet").getHealth());
    }

    private ZombieArmorPart armor(Zombie zombie, String name) {
        return zombie.getArmorParts().stream()
                .filter(part -> part.getName().equals(name))
                .findFirst()
                .orElseThrow();
    }
}
