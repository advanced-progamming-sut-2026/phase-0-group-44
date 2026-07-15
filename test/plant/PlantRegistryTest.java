package plant;

import model.enums.PlantTag;
import model.enums.PlantType;
import model.inGame.plant.PlantDefinition;
import model.inGame.plant.PlantFactory;
import model.inGame.plant.PlantRegistry;
import model.inGame.plant.PlantStats;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlantRegistryTest {
    private final PlantRegistry registry = PlantRegistry.getDefault();

    @Test
    void canonicalCsvContainsEveryClassifiedRow() {
        assertEquals(69, registry.findAll().size());
        assertEquals(57, registry.findMandatory().size());
        assertEquals(12, registry.findBonus().size());

        Set<PlantType> expectedBonus = Set.of(
                PlantType.PEA_POD,
                PlantType.CAULIPOWER,
                PlantType.ELECTRIC_BLUEBERRY,
                PlantType.STARFRUIT,
                PlantType.GOO_PEASHOOTER,
                PlantType.GRAPESHOT,
                PlantType.CHOMPER,
                PlantType.WASABI_WHIP,
                PlantType.KIWIBEAST,
                PlantType.SWEET_POTATO,
                PlantType.HYPNO_SHROOM,
                PlantType.CAT_TAIL
        );
        assertEquals(expectedBonus, registry.findBonus().stream()
                .map(PlantDefinition::getType).collect(Collectors.toSet()));
    }

    @Test
    void everyMandatoryRowHasAFactoryStrategyAndNoBonusRowDoes() {
        PlantFactory factory = new PlantFactory(registry);
        for (PlantDefinition definition : registry.findMandatory()) {
            assertTrue(factory.supports(definition.getType()), definition.getName());
            if (definition.getType() == PlantType.IMITATER) {
                factory.createImitater(PlantType.PEASHOOTER, 1);
            } else {
                factory.create(definition.getType(), 1);
            }
        }
        for (PlantDefinition definition : registry.findBonus()) {
            assertFalse(factory.supports(definition.getType()), definition.getName());
            assertThrows(UnsupportedOperationException.class,
                    () -> factory.create(definition.getType(), 1));
        }
    }

    @Test
    void tagsAndTimingsComeFromCanonicalCsv() {
        PlantDefinition sunShroom = registry.require(PlantType.SUN_SHROOM);
        assertTrue(sunShroom.hasTag(PlantTag.SHROOM));
        assertTrue(sunShroom.hasTag(PlantTag.RAMP_UP));
        assertTrue(sunShroom.hasTag(PlantTag.NIGHT));
        assertEquals(24.0, sunShroom.getActionInterval());
        assertEquals(5.0, sunShroom.getRecharge());
    }

    @Test
    void upgradesOnlyChangeTheirDeclaredStatistics() {
        PlantDefinition peashooter = registry.require(PlantType.PEASHOOTER);
        PlantStats levelOne = peashooter.statsAtLevel(1);
        PlantStats levelTwo = peashooter.statsAtLevel(2);
        PlantStats levelThree = peashooter.statsAtLevel(3);
        PlantStats levelFour = peashooter.statsAtLevel(4);

        assertEquals(20, levelOne.getDamage());
        assertEquals(30, levelTwo.getDamage());
        assertEquals(300, levelTwo.getHp());
        assertEquals(450, levelThree.getHp());
        assertEquals(75, levelFour.getCost());
        assertEquals(levelOne.getRecharge(), levelFour.getRecharge());
    }
}
