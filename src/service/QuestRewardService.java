package service;

import model.News;
import model.config.ChapterCatalog;
import model.config.GameWorld;
import model.enums.NewsType;
import model.inGame.plant.PlantDefinition;
import model.inGame.plant.PlantRepository;
import model.user.User;
import model.utility.Quest;
import model.utility.QuestProgress;
import model.utility.Reward;
import util.RandomSource;

import java.util.ArrayList;
import java.util.List;

/** Applies a completed quest reward exactly once; persistence is owned by QuestService. */
public final class QuestRewardService {
    private final PlantRepository plantRepository;
    private final RandomSource random;

    public QuestRewardService(PlantRepository plantRepository, RandomSource random) {
        this.plantRepository = plantRepository;
        this.random = random;
    }

    public String apply(User user, Quest quest, QuestProgress progress) {
        if (user == null || quest == null || progress == null) {
            throw new IllegalArgumentException("User, quest and progress are required.");
        }
        Reward reward = quest.getReward();
        int amount = reward.resolveAmount(progress.getVariableValue());
        if (amount < 0) {
            throw new IllegalStateException("Canonical reward resolved to a negative amount.");
        }

        return switch (reward.getType()) {
            case COINS -> {
                user.setCoins(Math.addExact(user.getCoins(), amount));
                yield amount + " coins granted";
            }
            case GEMS -> {
                user.setGems(Math.addExact(user.getGems(), amount));
                yield amount + " gems granted";
            }
            case INVENTORY -> {
                user.getInventory().merge(reward.getTarget(), amount, Math::addExact);
                yield amount + " " + reward.getTarget() + " granted";
            }
            case PLANT_UNLOCK -> unlockPlant(user, reward.getTarget());
            case LEVEL_UNLOCK -> unlockLevel(user, reward.getTarget());
        };
    }

    private String unlockPlant(User user, String target) {
        if (!"random-new-plant".equals(target)) {
            PlantDefinition definition = plantRepository == null
                    ? null : plantRepository.findByName(target);
            if (definition == null) {
                throw new IllegalStateException("Unknown plant unlock target: " + target);
            }
            return unlockPlant(user, definition);
        }

        List<PlantDefinition> candidates = new ArrayList<>();
        if (plantRepository != null) {
            for (PlantDefinition definition : plantRepository.findAll()) {
                if (definition != null && definition.isMandatory()
                        && !user.getCollection().hasPlant(definition.getType())) {
                    candidates.add(definition);
                }
            }
        }
        if (candidates.isEmpty()) {
            return "no locked mandatory plant remains";
        }
        PlantDefinition selected = candidates.get(random.nextInt(candidates.size()));
        return unlockPlant(user, selected);
    }

    private String unlockPlant(User user, PlantDefinition definition) {
        if (!user.getCollection().hasPlant(definition.getType())) {
            user.getCollection().purchasePlant(definition);
            user.getNewsList().add(new News(
                    "New plant unlocked",
                    "You unlocked the plant " + definition.getName() + ".",
                    NewsType.PLANT_UNLOCKED
            ));
        }
        return definition.getName() + " unlocked";
    }

    private String unlockLevel(User user, String target) {
        if (target == null) {
            throw new IllegalStateException("Level unlock reward has no target.");
        }
        String[] parts = target.split("\\|", 2);
        if (parts.length != 2) {
            throw new IllegalStateException(
                    "Level unlock target must be '<chapter>|<level>': " + target);
        }
        GameWorld world = GameWorld.fromName(parts[0]);
        int level;
        try {
            level = Integer.parseInt(parts[1]);
        } catch (NumberFormatException exception) {
            throw new IllegalStateException("Invalid level unlock target: " + target);
        }
        if (world == null || !ChapterCatalog.unlockLevel(user, world, level)) {
            return "level was already available";
        }
        String label = world.getDisplayName() + " level " + level;
        user.getNewsList().add(new News(
                "New level unlocked",
                "You unlocked " + label + ".",
                NewsType.LEVEL_UNLOCKED
        ));
        return label + " unlocked";
    }
}
