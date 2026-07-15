package zombie;

import model.enums.ZombieType;
import model.inGame.zombie.ZombieBehaviorFactory;
import model.inGame.zombie.ZombieDefinition;
import model.inGame.zombie.ZombieRegistry;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ZombieRegistryTest {

    @Test
    void canonicalRegistryContainsEveryClassifiedDocumentRow() {
        ZombieRegistry registry = ZombieRegistry.getDefault();

        assertEquals(28, registry.findAll().size());
        assertEquals(22, registry.findMandatory().size());
        assertEquals(6, registry.findBonus().size());
        assertEquals(Set.of(
                ZombieType.ARCADE_ZOMBIE,
                ZombieType.TROGLOBITE,
                ZombieType.FISHERMAN,
                ZombieType.JESTER,
                ZombieType.WIZARD,
                ZombieType.KING
        ), registry.findBonus().stream()
                .map(ZombieDefinition::getType)
                .collect(Collectors.toSet()));
    }

    @Test
    void factoryCoversEveryMandatoryRowAndNoBlueRow() {
        ZombieRegistry registry = ZombieRegistry.getDefault();
        ZombieBehaviorFactory factory = new ZombieBehaviorFactory();

        for (ZombieDefinition definition : registry.findMandatory()) {
            assertTrue(factory.supports(definition.getType()), definition.getName());
            factory.create(definition);
        }
        for (ZombieDefinition definition : registry.findBonus()) {
            assertFalse(factory.supports(definition.getType()), definition.getName());
            assertThrows(UnsupportedOperationException.class,
                    () -> factory.create(definition));
        }
    }

    @Test
    void canonicalArmorValuesAndAliasesAreCentralized() {
        ZombieRegistry registry = ZombieRegistry.getDefault();

        assertEquals(370, registry.require(ZombieType.CONEHEAD).getArmor());
        assertEquals(1100, registry.require(ZombieType.BUCKETHEAD).getArmor());
        assertEquals(3200, registry.require(ZombieType.KNIGHT).getArmor());
        assertEquals(2200, registry.require(ZombieType.BLOCKHEAD).getArmor());
        assertEquals(ZombieType.ALL_STAR, ZombieType.fromToken("all-star"));
        assertEquals(ZombieType.RA_ZOMBIE, ZombieType.fromToken("Ra"));
        assertEquals(ZombieType.OCTOPUS_ZOMBIE, ZombieType.fromToken("octopus"));
    }
}
