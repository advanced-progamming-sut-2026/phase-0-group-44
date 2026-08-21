package screen.gameplay;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import model.enums.PlantType;

import java.util.EnumMap;
import java.util.Map;

public final class PlantAnimationCatalog {
    private static final Map<PlantType, PlantAnimationSpec> SPECS = new EnumMap<>(PlantType.class);

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
        put(PlantType.APPEASE_MINT, initial());
        put(PlantType.ARMA_MINT, initial());
        put(PlantType.BOMBARD_MINT, initial());
        put(PlantType.BONK_CHOY, initial());
        put(PlantType.BOWLING_BULB, full());
        put(PlantType.CABBAGE_PULT, initial());
        put(PlantType.CACTUS, initial());
        put(PlantType.CAULIPOWER, initial()
                .clip(PlantAnimationState.IDLE, "Idle1_1")
                .clip(PlantAnimationState.IDLE2, "Idle2_1")
                .clip(PlantAnimationState.IDLE3, "Idle3_1")
                .clip(PlantAnimationState.IDLE4, "Idle4_1")
                .clip(PlantAnimationState.ATTACK, "Ataack")); // sic in source — verify before shipping
        put(PlantType.CHERRY_BOMB, full());
        put(PlantType.CHOMPER, initial());
        put(PlantType.CITRON, full());
        put(PlantType.DOOM_SHROOM, full());
        put(PlantType.ELECTRIC_BLUEBERRY, initial());
        put(PlantType.ENCHANT_MINT, initial());
        put(PlantType.ENDURIAN, full());
        put(PlantType.ENFORCE_MINT, initial());
        put(PlantType.ENLIGHTEN_MINT, initial());
        put(PlantType.EXPLODE_O_NUT, initial());
        put(PlantType.FIRE_PEASHOOTER, initial());
        put(PlantType.FUME_SHROOM, initial());
        put(PlantType.GARLIC, full());
        put(PlantType.GOLD_BLOOM, initial());
        put(PlantType.GOO_PEASHOOTER, initial());
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
                .clip(PlantAnimationState.IDLE, "Idle_stage1_")
                .clip(PlantAnimationState.ATTACK, "Attack_stage1"));
        put(PlantType.LILY_PAD, full());
        put(PlantType.MAGNET_SHROOM, full());
        put(PlantType.MEGA_GATLING_PEA, initial().folder("MEGAGATLING")); // folder drops "PEA" — verify
        put(PlantType.MELON_PULT, initial());
        put(PlantType.PEA_POD, full());
        put(PlantType.PEASHOOTER, initial());
        put(PlantType.PEPPER_PULT, full());
        put(PlantType.PUFF_SHROOM, full().folder("PERFSHROOM")); // source says "perfshroom" — double-check this one
        put(PlantType.PHAT_BEET, full().folder("PHATBEETS")); // folder is plural — verify
        put(PlantType.POTATO_MINE, initial());
        put(PlantType.PRIMAL_POTATO_MINE, full().folder("PRIMAL_POTATOMINE")); // keeps the underscore — verify
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

    /**
     * Resolves the PAM path for {@code type}, checking disk and falling
     * back to the other tier if the preferred one is missing. Returns
     * {@code null} (after logging) if neither tier has the asset, so the
     * caller can skip drawing instead of handing PamPlayer a bad path.
     */
    public static String resolveExistingPamPath(PlantType type, FileHandle assetRoot) {
        if (assetRoot == null) {
            return pamPath(type);
        }
        PlantAnimationSpec spec = spec(type);
        String preferred = pamPath(type, spec.tier());
        if (assetRoot.child(preferred).exists()) {
            return preferred;
        }
        PlantAnimationTier other = spec.tier() == PlantAnimationTier.INITIAL
                ? PlantAnimationTier.FULL : PlantAnimationTier.INITIAL;
        String fallback = pamPath(type, other);
        if (assetRoot.child(fallback).exists()) {
            return fallback;
        }
        Gdx.app.error("PlantAnimationCatalog",
                "No PAM asset found for " + type + " at " + preferred + " or " + fallback);
        return null;
    }
}