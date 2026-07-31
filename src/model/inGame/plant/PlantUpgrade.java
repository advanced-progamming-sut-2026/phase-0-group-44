package model.inGame.plant;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class PlantUpgrade {
    private static final Pattern SIGNED_NUMBER = Pattern.compile("([+-])\\s*(\\d+(?:\\.\\d+)?)");

    private final int level;
    private final String description;
    private final UpgradeEffect effect;

    public PlantUpgrade(int level, String description, UpgradeEffect effect) {
        this.level = level;
        this.description = description == null ? "" : description.trim();
        this.effect = effect;
    }

    @SuppressWarnings("PMD.ExcessiveMethodLength")
    public static PlantUpgrade parse(int level, String description) {
        String text = description == null ? "" : description.trim();
        String normalized = text.toLowerCase(Locale.ROOT);
        double signed = signedValue(text);

        UpgradeEffect effect;
        if (normalized.startsWith("hp ")) {
            effect = UpgradeEffect.numeric("HP", signed);
        } else if (normalized.startsWith("dmg ")) {
            effect = UpgradeEffect.numeric("DAMAGE", signed);
        } else if (normalized.startsWith("cost ")) {
            effect = UpgradeEffect.numeric("COST", signed);
        } else if (normalized.startsWith("cooldown ")) {
            effect = UpgradeEffect.numeric("RECHARGE", signed);
        } else if (normalized.startsWith("prod. time ") || normalized.startsWith("charge time ")) {
            effect = UpgradeEffect.numeric("ACTION_INTERVAL", signed);
        } else if (normalized.startsWith("atk speed ")) {
            effect = UpgradeEffect.percent("ACTION_INTERVAL", -Math.abs(signed) / 100.0);
        } else if (normalized.startsWith("grow time ")) {
            effect = UpgradeEffect.numeric("GROW_TIME", signed);
        } else if (normalized.startsWith("sun +")) {
            effect = UpgradeEffect.numeric("SUN_AMOUNT", signed);
        } else if (normalized.startsWith("chill time ")) {
            effect = UpgradeEffect.numeric("CHILL_DURATION", signed);
        } else if (normalized.startsWith("freeze time ")) {
            effect = UpgradeEffect.numeric("FREEZE_DURATION", signed);
        } else if (normalized.startsWith("regen ")) {
            effect = UpgradeEffect.numeric("REGEN_INTERVAL", signed);
        } else if (normalized.startsWith("pierce ")) {
            effect = UpgradeEffect.numeric("PIERCE", signed);
        } else if (normalized.startsWith("plant food chance ")) {
            effect = UpgradeEffect.percent("PLANT_FOOD_CHANCE", Math.abs(signed) / 100.0);
        } else if (normalized.startsWith("range ")) {
            effect = UpgradeEffect.numeric("RANGE", signed);
        } else if (normalized.startsWith("lifespan ")) {
            effect = UpgradeEffect.numeric("LIFESPAN", signed);
        } else if (normalized.startsWith("butter ")) {
            effect = UpgradeEffect.percent("BUTTER_CHANCE", Math.abs(signed) / 100.0);
        } else if (normalized.startsWith("aoe dmg ")) {
            effect = UpgradeEffect.numeric("SPLASH_DAMAGE", signed);
        } else if (normalized.startsWith("warmth radius ")) {
            effect = UpgradeEffect.numeric("WARMTH_RADIUS", signed);
        } else if (normalized.startsWith("arm time ")) {
            effect = UpgradeEffect.numeric("ARM_TIME", signed);
        } else if (normalized.startsWith("reflect dmg ")) {
            effect = UpgradeEffect.numeric("REFLECT_DAMAGE", signed);
        } else if (normalized.startsWith("sun drop ")) {
            effect = UpgradeEffect.numeric("SUN_DROP", signed);
        } else if (normalized.startsWith("eat time ")) {
            effect = UpgradeEffect.numeric("EAT_TIME", signed);
        } else if (normalized.startsWith("duration ")) {
            effect = UpgradeEffect.numeric("MINT_DURATION", signed);
        } else if (normalized.startsWith("targets ")) {
            effect = UpgradeEffect.numeric("TARGET_COUNT", signed);
        } else if (normalized.contains("double sun chance")) {
            effect = UpgradeEffect.flag("DOUBLE_SUN_CHANCE");
        } else if (normalized.contains("can crush 2x")) {
            effect = UpgradeEffect.numeric("CRUSH_CHARGES", 1);
        } else if (normalized.contains("aoe on death")) {
            effect = UpgradeEffect.flag("DEATH_EXPLOSION_AOE");
        } else if (normalized.contains("melt area 3x3")) {
            effect = UpgradeEffect.flag("MELT_AREA_3X3");
        } else if (normalized.contains("explode on finish")) {
            effect = UpgradeEffect.flag("EXPLODE_ON_FINISH");
        } else if (normalized.contains("reset family cooldowns")) {
            effect = UpgradeEffect.flag("RESET_FAMILY_COOLDOWNS");
        } else if (normalized.contains("plant food on enterance") || normalized.contains("plant food on entrance")) {
            effect = UpgradeEffect.flag("AUTO_PLANT_FOOD_ON_ENTER");
        } else {
            effect = UpgradeEffect.flag(normalizeKey(text));
        }
        return new PlantUpgrade(level, text, effect);
    }

    private static double signedValue(String text) {
        Matcher matcher = SIGNED_NUMBER.matcher(text);
        if (!matcher.find()) {
            return 0.0;
        }
        double value = Double.parseDouble(matcher.group(2));
        return matcher.group(1).equals("-") ? -value : value;
    }

    private static String normalizeKey(String text) {
        String key = text.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]+", "_");
        return key.replaceAll("^_+|_+$", "");
    }

    public int getLevel() {
        return level;
    }

    public UpgradeEffect getEffect() {
        return effect;
    }
}
