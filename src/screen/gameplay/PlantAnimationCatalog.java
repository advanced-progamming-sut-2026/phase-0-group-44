package screen.gameplay;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import model.enums.PlantType;

import java.util.EnumMap;
import java.util.Map;

public final class PlantAnimationCatalog {
    private static final Map<PlantType, PlantAnimationSpec> SPECS = new EnumMap<>(PlantType.class);
    private static final Map<PlantType, PlantAnimationState> ATTACK_STATE_OVERRIDES = new EnumMap<>(PlantType.class);
    static {
        ATTACK_STATE_OVERRIDES.put(PlantType.CHOMPER, PlantAnimationState.BITE);
    }

    /** The clip that represents this plant's "attacking" pose — usually ATTACK, but not always (e.g. Chomper bites). */
    public static String attackClipName(PlantType type) {
        PlantAnimationState state = ATTACK_STATE_OVERRIDES.getOrDefault(type, PlantAnimationState.ATTACK);
        return clipName(type, state);
    }

    static {
        register();
    }

    private PlantAnimationCatalog() {
    }

    private static void put(PlantType type, PlantAnimationSpec.Builder builder) {
        SPECS.put(type, builder.build(type));
    }

    private static PlantAnimationSpec.Builder initial() {
        return PlantAnimationSpec.builder(PlantAnimationTier.INITIAL);
    }

    private static PlantAnimationSpec.Builder full() {
        return PlantAnimationSpec.builder(PlantAnimationTier.FULL);
    }

    private static void register() {
        put(PlantType.WALL_NUT, initial().damageStages(3)); // idle, damage, damage2, damage3 — tier unverified, guessed INITIAL
        put(PlantType.TALL_NUT, initial().damageStages(2)); // idle, damage, damage2 — tier unverified, guessed INITIAL
        put(PlantType.PUMPKIN, initial().idleStages(3)); // idle, idle2, idle3 — no damage clips supplied, treated as idle variety like Cactus
        put(PlantType.SUN_BEAN, initial().idleStages(2)); // idle, idle2 — no damage clips supplied
        put(PlantType.STARFRUIT, initial());
        put(PlantType.SWEET_POTATO, initial().clip(PlantAnimationState.DAMAGE, "idle_damage"));
        put(PlantType.APPEASE_MINT, initial());
        put(PlantType.ARMA_MINT, initial());
        put(PlantType.BOMBARD_MINT, initial());
        put(PlantType.BONK_CHOY, initial().idleStages(3));
        put(PlantType.BOWLING_BULB, full());
        put(PlantType.CABBAGE_PULT, initial());
        put(PlantType.CACTUS, initial().idleStages(3));
        put(PlantType.CAULIPOWER, initial().idleStages(4)
                .clip(PlantAnimationState.IDLE, "idle1_1")
                .clip(PlantAnimationState.IDLE2, "idle2_1")
                .clip(PlantAnimationState.IDLE3, "idle3_1")
                .clip(PlantAnimationState.IDLE4, "idle4_1")
                .clip(PlantAnimationState.ATTACK, "attack"));
        put(PlantType.CHERRY_BOMB, full());
        put(PlantType.CHOMPER, initial().idleStages(4));
        put(PlantType.CITRON, full());
        put(PlantType.DOOM_SHROOM, full());
        put(PlantType.ELECTRIC_BLUEBERRY, initial()
                .clip(PlantAnimationState.IDLE, "idle1_1")
                .clip(PlantAnimationState.IDLE, "idle1_2")
                .clip(PlantAnimationState.IDLE, "idle2_1")
                .clip(PlantAnimationState.IDLE, "idle2_2")
                .clip(PlantAnimationState.IDLE, "idle2_3")
                .clip(PlantAnimationState.IDLE, "idle2_4")
                .clip(PlantAnimationState.IDLE, "idle3_1")
                .clip(PlantAnimationState.ATTACK, "attack"));

        // real clips: idle1_1, idle1_2, idle2_1..4, idle3_1..3, idle4_1..3, attack, plantfood, water
        put(PlantType.ENCHANT_MINT, initial());
        put(PlantType.ENDURIAN, full().damageStages(1)
                .clip(PlantAnimationState.IDLE, "attack_loop"));
        put(PlantType.ENFORCE_MINT, initial());
        put(PlantType.ENLIGHTEN_MINT, initial());
        put(PlantType.EXPLODE_O_NUT, initial().idleStages(3).damageStages(3));
        put(PlantType.FIRE_PEASHOOTER, initial());
        put(PlantType.FUME_SHROOM, initial());
        put(PlantType.GARLIC, full().damageStages(2)
                .clip(PlantAnimationState.DAMAGE, "idle_damage")
                .clip(PlantAnimationState.DAMAGE2, "idle_damage2"));
        put(PlantType.GOLD_BLOOM, initial());
        put(PlantType.GOO_PEASHOOTER, initial().idleStages(3));
        put(PlantType.GRAPESHOT, initial());
        put(PlantType.GRAVE_BUSTER, initial());
        put(PlantType.HOT_POTATO, full());
        put(PlantType.HYPNO_SHROOM, initial());
        put(PlantType.ICE_SHROOM, full());
        put(PlantType.ICEBERG_LETTUCE, initial().folder("ICEBURG")); // asset folder is short/misspelled — verify
        put(PlantType.IMITATER, initial().folder("IMITATOR")); // enum spelling differs from folder — verify
        put(PlantType.JALAPENO, initial());
        put(PlantType.KERNEL_PULT, initial().folder("KERNALPULT")); // folder uses "KERNAL" — verify
        put(PlantType.KIWIBEAST, initial()
                .clip(PlantAnimationState.IDLE, "idle_stage1_")
                .clip(PlantAnimationState.ATTACK, "attack_stage1"));
        put(PlantType.LILY_PAD, full());
        put(PlantType.MAGNET_SHROOM, full());
        put(PlantType.MEGA_GATLING_PEA, initial().folder("MEGAGATLING")); // folder drops "PEA" — verify
        put(PlantType.MELON_PULT, initial());
        put(PlantType.PEA_POD, full().idleStages(5));
        put(PlantType.PEASHOOTER, initial());
        put(PlantType.PEPPER_PULT, full());
        put(PlantType.PUFF_SHROOM, full().folder("PERFSHROOM")); // source says "perfshroom" — double-check this one
        put(PlantType.PHAT_BEET, full().folder("PHATBEETS")); // folder is plural — verify
        put(PlantType.POTATO_MINE, initial());
        put(PlantType.PRIMAL_POTATO_MINE, full().folder("PRIMAL_POTATOMINE"));

    }

    public static String damageStageClip(PlantType type, double hpRatio) {
        PlantAnimationSpec s = spec(type);
        int stages = s.damageStageCount();
        if (stages <= 0 || hpRatio >= 1.0) {
            return null;
        }
        double damageFraction = 1.0 - Math.max(0.0, Math.min(1.0, hpRatio));
        double band = 1.0 / stages;
        int stageIndex = (int) Math.ceil(damageFraction / band);
        stageIndex = Math.max(1, Math.min(stages, stageIndex));
        PlantAnimationState[] damageStates = {
                PlantAnimationState.DAMAGE, PlantAnimationState.DAMAGE2, PlantAnimationState.DAMAGE3
        };
        return s.clipName(damageStates[stageIndex - 1]);
    }

    public static java.util.List<String> idleClipSequence(PlantType type) {
        PlantAnimationSpec s = spec(type);
        PlantAnimationState[] states = {
                PlantAnimationState.IDLE, PlantAnimationState.IDLE2, PlantAnimationState.IDLE3,
                PlantAnimationState.IDLE4, PlantAnimationState.IDLE5
        };
        java.util.List<String> result = new java.util.ArrayList<>();
        for (int i = 0; i < s.idleStageCount(); i++) {
            result.add(s.clipName(states[i]));
        }
        return result;
    }

    public static PlantAnimationSpec spec(PlantType type) {
        PlantAnimationSpec spec = SPECS.get(type);
        if (spec != null) {
            return spec;
        }
        // No confirmed data yet for this plant — fall back to the naive
        // underscore-stripped guess so nothing crashes, but this entry
        // needs real data before it can be trusted.
        return PlantAnimationSpec.builder(PlantAnimationTier.INITIAL).build(type);
    }

    public static String pamPath(PlantType type) {
        return pamPath(type, spec(type).tier());
    }

    public static String pamPath(PlantType type, PlantAnimationTier tier) {
        String folder = spec(type).folder();
        return tier.root() + folder + "/" + folder + ".PAM";
    }

    public static String clipName(PlantType type, PlantAnimationState state) {
        return spec(type).clipName(state);
    }

    public static String resolveExistingPamPath(PlantType type, FileHandle assetRoot) {
        if (assetRoot == null) {
            return pamPath(type);
        }
        PlantAnimationSpec spec = spec(type);
        String preferred = pamPath(type, spec.tier());
        if (existsDirectOrUnderImages(assetRoot, preferred)) {
            return preferred;
        }

        PlantAnimationTier other = spec.tier() == PlantAnimationTier.INITIAL
                ? PlantAnimationTier.FULL : PlantAnimationTier.INITIAL;
        String fallback = pamPath(type, other);
        if (existsDirectOrUnderImages(assetRoot, fallback)) {
            return fallback;
        }

        Gdx.app.error("PlantAnimationCatalog",
                "No PAM asset found for " + type + " at " + preferred + " or " + fallback
                        + " (also checked IMAGES/ prefix for both)");
        return null;
    }

    /**
     * pvz-asset-browser stores files on disk under an IMAGES/ prefix that
     * PamPlayer already accounts for internally when it actually reads bytes
     * (see how zombie PAMs resolve fine with no IMAGES/ in their path). This
     * only checks existence — the path returned to callers must stay
     * IMAGES-free, or PamPlayer will double-prepend it (see
     * BattlefieldChapterEffects/BattlefieldEnvironmentLayer/
     * BattlefieldFrostbiteStateLayer for the same existence-only pattern).
     */
    private static boolean existsDirectOrUnderImages(FileHandle assetRoot, String path) {
        return assetRoot.child(path).exists()
                || assetRoot.child("IMAGES").child(path).exists();
    }
}