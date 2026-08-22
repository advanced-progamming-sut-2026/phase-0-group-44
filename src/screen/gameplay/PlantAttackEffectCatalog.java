package screen.gameplay;

import model.enums.PlantType;

import java.util.EnumMap;
import java.util.Map;

public final class PlantAttackEffectCatalog {

    public record EffectSpec(String pamPath, String clip, float duration) { }

    private static final Map<PlantType, EffectSpec> EFFECTS = new EnumMap<>(PlantType.class);

    static {
        // Clip names confirmed from runtime errors are exact; others are best-effort
        // guesses of "animation" until verified the same way.
        register(PlantType.EXPLODE_O_NUT, "768/INITIAL/EFFECTS/EXPLODEONUT_BLINK/EXPLODEONUT_BLINK.PAM", "animation", 0.6f); // clip name guessed — verify
        register(PlantType.SUN_BEAN, "768/FULL/EFFECTS/SUNBEAN_PLANTFOOD_EFFECT_OVERLAY1/SUNBEAN_PLANTFOOD_EFFECT_OVERLAY1.PAM", "animation", 0.6f); // clip name guessed — verify
        register(PlantType.CAULIPOWER, "768/INITIAL/EFFECTS/CAULIPOWER_PROJECTILE/CAULIPOWER_PROJECTILE.PAM", "animation", 0.9f);
        register(PlantType.ELECTRIC_BLUEBERRY, "768/INITIAL/EFFECTS/ELECTRICBLUEBERRY_CLOUD_PROJECTILE/ELECTRICBLUEBERRY_CLOUD_PROJECTILE.PAM", "attack", 0.9f);
        register(PlantType.STARFRUIT, "768/INITIAL/EFFECTS/STARFRUIT_PROJECTILE_PLANTFOOD/STARFRUIT_PROJECTILE_PLANTFOOD.PAM", "animation", 0.8f);
        register(PlantType.GRAPESHOT, "768/INITIAL/EFFECTS/ESCAPEROOT_EXPLOSION_GRAPESHOT/ESCAPEROOT_EXPLOSION_GRAPESHOT.PAM", "animation", 0.5f);
        register(PlantType.KIWIBEAST, "768/INITIAL/EFFECTS/KIWIBEAST_TILE_HIT/KIWIBEAST_TILE_HIT.PAM", "animation", 0.8f);
        register(PlantType.GOO_PEASHOOTER, "768/INITIAL/EFFECTS/GOOPEASHOOTER_PROJECTILES/GOOPEASHOOTER_PROJECTILES.PAM", "projectile_t1", 0.8f);
        register(PlantType.CABBAGE_PULT, "768/INITIAL/EFFECTS/CABBAGEPULT_PLANTFOOD_PROJECTILE/CABBAGEPULT_PLANTFOOD_PROJECTILE.PAM", "animation", 0.9f);
        register(PlantType.CACTUS, "768/INITIAL/EFFECTS/T_CACTUS_PROJECTILE/T_CACTUS_PROJECTILE.PAM", "idle", 0.9f); // confirmed: [idle, idle2, idle3]
        register(PlantType.CHERRY_BOMB, "768/INITIAL/EFFECTS/ESCAPEROOT_EXPLOSION_CHERRYBOMB_TOP/ESCAPEROOT_EXPLOSION_CHERRYBOMB_TOP.PAM", "animation", 0.8f);
        register(PlantType.CITRON, "768/FULL/EFFECTS/T_CITRON_CITRUS_ORB/T_CITRON_CITRUS_ORB.PAM", "animation", 0.6f);
        register(PlantType.FIRE_PEASHOOTER, "768/INITIAL/EFFECTS/FIREPEASHOOTER_FIRE/FIREPEASHOOTER_FIRE.PAM", "animation", 0.5f);
        register(PlantType.GRAVE_BUSTER, "768/INITIAL/EFFECTS/GRAVEBUSTER_EXPLOSION_POTATOMINE/GRAVEBUSTER_EXPLOSION_POTATOMINE.PAM", "animation", 0.8f);
        register(PlantType.HOT_POTATO, "768/FULL/EFFECTS/HOTPOTATO_STEAMFX/HOTPOTATO_STEAMFX.PAM", "animation", 0.8f);
        register(PlantType.ICE_SHROOM, "768/FULL/EFFECTS/ICESHROOM_MELEE_ATTACK/ICESHROOM_MELEE_ATTACK.PAM", "animation", 0.7f);
        register(PlantType.JALAPENO, "768/INITIAL/EFFECTS/JALAPENO_FIRE/JALAPENO_FIRE.PAM", "idle", 0.8f);
        register(PlantType.KERNEL_PULT, "768/INITIAL/EFFECTS/T_KERNALPULT_PROJECTILE/T_KERNALPULT_PROJECTILE.PAM", "animation", 0.6f);
        register(PlantType.MEGA_GATLING_PEA, "768/INITIAL/EFFECTS/MEGAGATLING_PROJECTILE/MEGAGATLING_PROJECTILE.PAM", "animation", 0.5f); // confirmed: [animation, animation3]
        register(PlantType.MELON_PULT, "768/INITIAL/EFFECTS/T_MELON_PROJECTILE/T_MELON_PROJECTILE.PAM", "animation", 0.6f); // confirmed: [animation, animation2, animation3]
        register(PlantType.PEA_POD, "768/FULL/EFFECTS/PEAPOD_PLANTFOOD_GIANTPEA/PEAPOD_PLANTFOOD_GIANTPEA.PAM", "animation", 0.5f); // confirmed: [animation]
        register(PlantType.PEPPER_PULT, "768/FULL/EFFECTS/PEPPERPULT_PROJECTILE/PEPPERPULT_PROJECTILE.PAM", "animation", 0.6f);
        register(PlantType.PUFF_SHROOM, "768/FULL/EFFECTS/PERFSHROOM_CLOUDS/PERFSHROOM_CLOUDS.PAM", "animation", 0.6f);
        register(PlantType.PHAT_BEET, "768/FULL/EFFECTS/PHATBEETS_TILE_HIT/PHATBEETS_TILE_HIT.PAM", "animation", 0.5f);
        register(PlantType.POTATO_MINE, "768/INITIAL/EFFECTS/POTATOMINE_EXPLOSION/POTATOMINE_EXPLOSION.PAM", "animation", 0.8f);
        register(PlantType.PRIMAL_POTATO_MINE, "768/INITIAL/EFFECTS/PRIMAL_POTATOMINE_EXPLOSION/PRIMAL_POTATOMINE_EXPLOSION.PAM", "animation", 0.8f);
    }

    private PlantAttackEffectCatalog() {
    }

    private static void register(PlantType type, String pamPath, String clip, float duration) {
        EFFECTS.put(type, new EffectSpec(pamPath, clip, duration));
    }

    public static EffectSpec forType(PlantType type) {
        return EFFECTS.get(type);
    }
}