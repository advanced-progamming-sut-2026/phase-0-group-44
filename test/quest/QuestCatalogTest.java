package quest;

import model.utility.Quest;
import model.utility.QuestCategory;
import model.utility.QuestPriority;
import model.utility.Reward;
import model.utility.RewardType;
import org.junit.jupiter.api.Test;
import service.QuestCatalog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class QuestCatalogTest {

    @Test
    void canonicalWorkbookExportLoadsAllTwentyRowsInStableOrder() {
        QuestCatalog catalog = QuestCatalog.fromFile(QuestCatalog.DEFAULT_PATH);

        assertEquals(20, catalog.getQuests().size());
        assertEquals("آفتاب گیر روزانه", catalog.getQuests().get(0).getName());
        assertEquals("وقت چمن‌زنی", catalog.getQuests().get(19).getName());
        assertEquals(14, catalog.byCategory(QuestCategory.DAILY).size());
        assertEquals(3, catalog.byCategory(QuestCategory.MAIN).size());
        assertEquals(3, catalog.byCategory(QuestCategory.EPIC).size());
    }

    @Test
    void canonicalPrioritiesConditionsAndRewardsAreMappedWithoutNewIds() {
        Quest defense = catalog().findByName("استاد دفاع");
        assertNotNull(defense);
        assertEquals("استاد دفاع", defense.getName());
        assertEquals(QuestPriority.CRITICAL, defense.getPriority());
        assertEquals(RewardType.GEMS, defense.getReward().getType());
        assertEquals(200, defense.getReward().resolveAmount(null));

        Quest sun = catalog().findByName("آفتاب گیر روزانه");
        assertEquals(Reward.AmountRule.VARIABLE_DIVIDED_BY_100,
                sun.getReward().getAmountRule());
        assertEquals(30, sun.getReward().resolveAmount("3000"));

        Quest economic = catalog().findByName("گیاه خوار اقتصادی");
        assertEquals(RewardType.INVENTORY, economic.getReward().getType());
        assertEquals(15, economic.getReward().resolveAmount("5"));

        Quest mower = catalog().findByName("وقت چمن‌زنی");
        assertEquals(RewardType.GEMS, mower.getReward().getType());
        assertEquals(40, mower.getReward().resolveAmount("40"));
    }

    private QuestCatalog catalog() {
        return QuestCatalog.fromFile(QuestCatalog.DEFAULT_PATH);
    }
}
