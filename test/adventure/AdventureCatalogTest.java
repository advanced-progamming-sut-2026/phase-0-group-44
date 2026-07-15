package adventure;

import model.config.AdventureCatalog;
import model.config.GameWorld;
import model.level.BossLevel;
import model.level.Chapter;
import model.level.NormalLevel;
import model.level.SpecialLevel;
import model.level.SpecialLevelType;
import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdventureCatalogTest {

    @Test
    void fourChaptersHaveFourLevelsAndEverySpecialTypeAppearsExactlyOnce() {
        assertEquals(4, AdventureCatalog.chapters().size());
        Map<SpecialLevelType, Integer> counts = new EnumMap<>(SpecialLevelType.class);

        for (Chapter chapter : AdventureCatalog.chapters()) {
            assertEquals(4, chapter.getLevels().size());
            assertInstanceOf(NormalLevel.class, chapter.getLevel(1));
            assertInstanceOf(SpecialLevel.class, chapter.getLevel(2));
            assertInstanceOf(SpecialLevel.class, chapter.getLevel(3));
            assertInstanceOf(BossLevel.class, chapter.getLevel(4));
            assertTrue(chapter.getLevel(4).isBossDeferred());

            for (int number = 2; number <= 3; number++) {
                SpecialLevelType type = chapter.getLevel(number)
                        .getAdventureConfig().getSpecialType();
                counts.merge(type, 1, Integer::sum);
            }
        }

        assertEquals(SpecialLevelType.values().length, counts.size());
        for (SpecialLevelType type : SpecialLevelType.values()) {
            assertEquals(1, counts.get(type));
        }
    }

    @Test
    void chapterOrderAndNamesMatchTheFourMandatoryWorlds() {
        assertEquals(GameWorld.ANCIENT_EGYPT,
                AdventureCatalog.chapters().get(0).getWorld());
        assertEquals(GameWorld.FROSTBITE_CAVES,
                AdventureCatalog.chapters().get(1).getWorld());
        assertEquals(GameWorld.BIG_WAVE_BEACH,
                AdventureCatalog.chapters().get(2).getWorld());
        assertEquals(GameWorld.DARK_AGES,
                AdventureCatalog.chapters().get(3).getWorld());
    }
}
