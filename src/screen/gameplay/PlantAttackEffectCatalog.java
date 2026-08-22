package screen.gameplay;

import model.enums.PlantType;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;

public final class PlantAttackEffectCatalog {

    public record EffectSpec(String pamPath, String clip, float duration) { }

    private static final Map<PlantType, EffectSpec> EFFECTS = new EnumMap<>(PlantType.class);
    // Stage-keyed effects for plants that fire several visually distinct attack
    // variants (e.g. Bowling Bulb's special/special2/special3). Falls back to
    // EFFECTS via forType() when a plant has no variant entries.
    private static final Map<PlantType, Map<String, EffectSpec>> VARIANT_EFFECTS = new EnumMap<>(PlantType.class);

    static {
        // Clip names confirmed from runtime errors are exact; others are best-effort
        // guesses of "animation" until verified the same way.
        register(PlantType.CACTUS, "768/INITIAL/EFFECTS/CACTUS_AIRATTACK/CACTUS_AIRATTACK.PAM", "idle2", 0.6f);
        register(PlantType.FUME_SHROOM, "768/INITIAL/EFFECTS/FUMESHROOM_BUBBLES/FUMESHROOM_BUBBLES.PAM", "special", 0.6f);
        register(PlantType.TORCHWOOD, "768/INITIAL/EFFECTS/TORCHWOOD_HIT_EFFECTS/TORCHWOOD_HIT_EFFECTS.PAM", "hit_normal", 0.6f);
        register(PlantType.WINTER_MELON, "768/FULL/EFFECTS/T_WINTERMELON_PROJECTILE/T_WINTERMELON_PROJECTILE.PAM", "animation", 0.6f);
        register(PlantType.EXPLODE_O_NUT, "768/INITIAL/EFFECTS/EXPLODEONUT_BLINK/EXPLODEONUT_BLINK.PAM", "animation", 0.6f);
        register(PlantType.SUN_BEAN, "768/FULL/EFFECTS/SUNBEAN_PLANTFOOD_EFFECT_OVERLAY1/SUNBEAN_PLANTFOOD_EFFECT_OVERLAY1.PAM", "animation", 0.6f);
        register(PlantType.CAULIPOWER, "768/INITIAL/EFFECTS/CAULIPOWER_PROJECTILE/CAULIPOWER_PROJECTILE.PAM", "animation", 0.9f);
        register(PlantType.ELECTRIC_BLUEBERRY, "768/INITIAL/EFFECTS/ELECTRICBLUEBERRY_CLOUD_PROJECTILE/ELECTRICBLUEBERRY_CLOUD_PROJECTILE.PAM", "attack", 0.9f);
        register(PlantType.STARFRUIT, "768/INITIAL/EFFECTS/STARFRUIT_PROJECTILE_PLANTFOOD/STARFRUIT_PROJECTILE_PLANTFOOD.PAM", "animation", 0.8f);
        register(PlantType.GRAPESHOT, "768/INITIAL/EFFECTS/ESCAPEROOT_EXPLOSION_GRAPESHOT/ESCAPEROOT_EXPLOSION_GRAPESHOT.PAM", "animation", 0.5f);
        register(PlantType.KIWIBEAST, "768/INITIAL/EFFECTS/KIWIBEAST_TILE_HIT/KIWIBEAST_TILE_HIT.PAM", "animation", 0.8f);
        register(PlantType.GOO_PEASHOOTER, "768/INITIAL/EFFECTS/GOOPEASHOOTER_PROJECTILES/GOOPEASHOOTER_PROJECTILES.PAM", "projectile_t1", 0.8f);
        register(PlantType.CABBAGE_PULT, "768/INITIAL/EFFECTS/CABBAGEPULT_PLANTFOOD_PROJECTILE/CABBAGEPULT_PLANTFOOD_PROJECTILE.PAM", "plantfood_cabbage", 0.9f);
        register(PlantType.CACTUS, "768/INITIAL/EFFECTS/T_CACTUS_PROJECTILE/T_CACTUS_PROJECTILE.PAM", "idle", 0.9f);
        register(PlantType.CHERRY_BOMB, "768/INITIAL/EFFECTS/ESCAPEROOT_EXPLOSION_CHERRYBOMB_TOP/ESCAPEROOT_EXPLOSION_CHERRYBOMB_TOP.PAM", "animation", 0.8f);
        // CITRON path corrected: real folder has no "T_" prefix.
        register(PlantType.CITRON, "768/FULL/EFFECTS/CITRON_CITRUS_ORB/CITRON_CITRUS_ORB.PAM", "Citron_Citrus_Orb", 0.6f);
        register(PlantType.FIRE_PEASHOOTER, "768/INITIAL/EFFECTS/FIREPEASHOOTER_FIRE/FIREPEASHOOTER_FIRE.PAM", "idle", 0.5f);
        register(PlantType.GRAVE_BUSTER, "768/INITIAL/EFFECTS/GRAVEBUSTER_EXPLOSION_POTATOMINE/GRAVEBUSTER_EXPLOSION_POTATOMINE.PAM", "animation", 0.8f);
        register(PlantType.HOT_POTATO, "768/FULL/EFFECTS/HOTPOTATO_STEAMFX/HOTPOTATO_STEAMFX.PAM", "animation", 0.8f);
        register(PlantType.ICE_SHROOM, "768/FULL/EFFECTS/ICESHROOM_MELEE_ATTACK/ICESHROOM_MELEE_ATTACK.PAM", "animation", 0.7f);
        register(PlantType.JALAPENO, "768/INITIAL/EFFECTS/JALAPENO_FIRE/JALAPENO_FIRE.PAM", "idle", 0.8f);
        register(PlantType.KERNEL_PULT, "768/INITIAL/EFFECTS/T_KERNALPULT_PROJECTILE/T_KERNALPULT_PROJECTILE.PAM", "animation", 0.6f);
        register(PlantType.MEGA_GATLING_PEA, "768/INITIAL/EFFECTS/MEGAGATLING_PROJECTILE/MEGAGATLING_PROJECTILE.PAM", "animation", 0.5f);
        register(PlantType.MELON_PULT, "768/INITIAL/EFFECTS/T_MELON_PROJECTILE/T_MELON_PROJECTILE.PAM", "animation", 0.6f);
        register(PlantType.PEA_POD, "768/FULL/EFFECTS/PEAPOD_PLANTFOOD_GIANTPEA/PEAPOD_PLANTFOOD_GIANTPEA.PAM", "animation", 0.5f);
        register(PlantType.PEPPER_PULT, "768/FULL/EFFECTS/PEPPERPULT_PROJECTILE/PEPPERPULT_PROJECTILE.PAM", "animation", 0.6f);
        register(PlantType.PUFF_SHROOM, "768/INITIAL/EFFECTS/T_PUFFSHROOM_PROJECTILE/T_PUFFSHROOM_PROJECTILE.PAM", "animation", 0.6f);
        register(PlantType.PHAT_BEET, "768/FULL/EFFECTS/PHATBEETS_TILE_HIT/PHATBEETS_TILE_HIT.PAM", "animation", 0.5f);
        register(PlantType.POTATO_MINE, "768/INITIAL/EFFECTS/POTATOMINE_EXPLOSION/POTATOMINE_EXPLOSION.PAM", "animation", 0.8f);
        register(PlantType.PRIMAL_POTATO_MINE, "768/INITIAL/EFFECTS/PRIMAL_POTATOMINE_EXPLOSION/PRIMAL_POTATOMINE_EXPLOSION.PAM", "animation", 0.8f);

        // --- New this pass ---
        register(PlantType.PEASHOOTER, "768/INITIAL/EFFECTS/SLINGPEA_PROJECTILE/SLINGPEA_PROJECTILE.PAM", "tier2", 0.5f); // clip/duration guessed — verify
        register(PlantType.SNOW_PEA, "768/INITIAL/EFFECTS/T_SNOW_PEA/T_SNOW_PEA.PAM", "animation", 0.5f); // clip/duration guessed — verify
        register(PlantType.REPEATER, "768/INITIAL/EFFECTS/REPEATER_PLANTFOOD_GIANTPEA/REPEATER_PLANTFOOD_GIANTPEA.PAM", "animation", 0.6f); // clip/duration guessed — verify
        register(PlantType.THREEPEATER, "768/INITIAL/EFFECTS/T_PEA_PROJECTILE/T_PEA_PROJECTILE.PAM", "animation", 0.5f); // clip/duration guessed — verify
        register(PlantType.ROTOBAGA, "768/FULL/EFFECTS/T_ROTORUTABAGA_PROJECTILE1/T_ROTORUTABAGA_PROJECTILE1.PAM", "animation", 0.6f); // clip/duration guessed — verify
        // Split Pea's attack/attack2/attack3 plant clips (right/both/left) all reuse this same effect PAM.
        register(PlantType.SPLIT_PEA, "768/INITIAL/EFFECTS/SLINGPEA_PROJECTILE/SLINGPEA_PROJECTILE.PAM", "tier2", 0.5f); // clip/duration guessed — verify
        register(PlantType.SEA_SHROOM, "768/FULL/EFFECTS/SEASHROOM_PROJECTILE/SEASHROOM_PROJECTILE.PAM", "animation", 0.6f); // clip/duration guessed — verify

        registerVariant(PlantType.BOWLING_BULB, "special",
                "768/FULL/EFFECTS/BOWLINGBULB_PROJECTILE1/BOWLINGBULB_PROJECTILE1.PAM", "animation", 0.6f); // clip/duration guessed — verify
        registerVariant(PlantType.BOWLING_BULB, "special2",
                "768/FULL/EFFECTS/BOWLINGBULB_PROJECTILE2/BOWLINGBULB_PROJECTILE2.PAM", "animation", 0.6f); // clip/duration guessed — verify
        registerVariant(PlantType.BOWLING_BULB, "special3",
                "768/FULL/EFFECTS/BOWLINGBULB_PROJECTILE3/BOWLINGBULB_PROJECTILE3.PAM", "animation", 0.6f); // clip/duration guessed — verify
    }

    private PlantAttackEffectCatalog() {
    }

    private static void register(PlantType type, String pamPath, String clip, float duration) {
        EFFECTS.put(type, new EffectSpec(pamPath, clip, duration));
    }

    private static void registerVariant(PlantType type, String variant, String pamPath, String clip, float duration) {
        VARIANT_EFFECTS.computeIfAbsent(type, t -> new LinkedHashMap<>())
                .put(variant, new EffectSpec(pamPath, clip, duration));
    }

    public static EffectSpec forType(PlantType type) {
        return EFFECTS.get(type);
    }

    /** Looks up a stage-specific effect (e.g. "special"/"special2"/"special3"); falls back to forType(). */
    public static EffectSpec forVariant(PlantType type, String variant) {
        Map<String, EffectSpec> variants = VARIANT_EFFECTS.get(type);
        if (variants == null) {
            return forType(type);
        }
        EffectSpec spec = variants.get(variant);
        return spec != null ? spec : forType(type);
    }
}