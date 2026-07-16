package model.quest;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class QuestCatalog {
    private final List<QuestDefinition> definitions;
    public QuestCatalog(List<QuestDefinition> definitions) { this.definitions = List.copyOf(definitions); }
    public List<QuestDefinition> getDefinitions() { return definitions; }

    public static QuestCatalog load(Path path) {
        try {
            List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
            List<QuestDefinition> result = new ArrayList<>();
            for (int i = 1; i < lines.size(); i++) {
                String line = lines.get(i).replace("\uFEFF", "").trim();
                if (line.isEmpty() || line.replace(",", "").isBlank()) continue;
                String[] c = line.split(",", -1);
                if (c.length < 6) throw new IllegalArgumentException("Malformed canonical quest row " + (i + 1));
                result.add(new QuestDefinition(c[0].trim(), QuestCategory.fromCanonical(c[1]), c[2].trim(),
                        c[3].trim(), QuestPriority.fromCanonical(c[4]), c[5].trim(), result.size(), parseReward(c[3])));
            }
            return new QuestCatalog(result);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot load canonical quest table: " + path, e);
        }
    }

    private static QuestReward parseReward(String text) {
        String normalized = text.replace('۱','1').replace('۲','2').replace('۳','3').replace('۴','4')
                .replace('۵','5').replace('۶','6').replace('۷','7').replace('۸','8').replace('۹','9').replace('۰','0');
        int amount = firstNumber(normalized);
        if (normalized.contains("الماس") || normalized.toLowerCase().contains("gem")) return new QuestReward(RewardKind.GEMS, amount, null, false);
        if (normalized.contains("سکه") || normalized.toLowerCase().contains("coin")) return new QuestReward(RewardKind.COINS, amount, null, false);
        if (normalized.contains("گیاه جدید")) return new QuestReward(RewardKind.PLANT_UNLOCK, 1, "random-killer-plant", true);
        if (normalized.contains("پک دانه")) return new QuestReward(RewardKind.INVENTORY, amount, "seed-packet", false);
        return new QuestReward(RewardKind.INVENTORY, amount, text, false);
    }

    private static int firstNumber(String text) {
        String digits = text.replaceAll("^[^0-9]*([0-9]+).*$", "$1");
        if (digits.equals(text) && !text.matches(".*[0-9].*")) return 0;
        try { return Integer.parseInt(digits); } catch (NumberFormatException e) { return 0; }
    }
}
