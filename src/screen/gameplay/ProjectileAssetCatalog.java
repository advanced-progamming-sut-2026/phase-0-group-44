package screen.gameplay;

import model.enums.PlantType;
import model.inGame.projectile.ButterEffect;
import model.inGame.projectile.FireEffect;
import model.inGame.projectile.HypnosisEffect;
import model.inGame.projectile.IceEffect;
import model.inGame.projectile.PoisonEffect;
import model.inGame.projectile.Projectile;
import model.inGame.projectile.ProjectileImpact;

/**
 * Maps live model projectiles to the supplied PvZ2 projectile artwork.
 *
 * <p>The asset/clip names in this catalog are aligned with the verified
 * projectile mappings in rezaxgz/Plants-vs-Zombies-2. Keep projectile artwork
 * separate from plant muzzle/impact effects: these PAMs physically travel with
 * the authoritative model projectile.</p>
 */
public final class ProjectileAssetCatalog {
    public record VisualSpec(
            String pamPath,
            String clip,
            String imageId,
            float sizeTiles
    ) {
        public VisualSpec(String pamPath, String clip, float sizeTiles) {
            this(pamPath, clip, null, sizeTiles);
        }

        public boolean usesStaticImage() {
            return imageId != null && !imageId.isBlank();
        }
    }

    /** Real PvZ2 collision animation for a projectile that has just hit. */
    public record ImpactSpec(
            String pamPath,
            String clip,
            float scale,
            float durationSeconds
    ) { }

    private static final VisualSpec PEA = new VisualSpec(
            "768/INITIAL/EFFECTS/T_PEA_PROJECTILE/T_PEA_PROJECTILE.PAM",
            "animation",
            0.46f
    );
    private static final VisualSpec SNOW_PEA = new VisualSpec(
            "768/INITIAL/EFFECTS/T_SNOW_PEA/T_SNOW_PEA.PAM",
            "animation",
            0.44f
    );
    private static final VisualSpec FIRE_PEA = new VisualSpec(
            "768/INITIAL/EFFECTS/T_FIRE_PEA/T_FIRE_PEA.PAM",
            "animation",
            0.48f
    );
    private static final VisualSpec GOO_PEA = new VisualSpec(
            "768/INITIAL/EFFECTS/GOOPEASHOOTER_PROJECTILES/GOOPEASHOOTER_PROJECTILES.PAM",
            "projectile_t1",
            0.45f
    );
    /*
     * Kernel-pult's real flying corn kernel is a named region in the supplied
     * PLANTKERNELPULT atlas. Use that exact region instead of asking the PAM
     * player to approximate the projectile. The PAM path remains a fallback
     * for reduced asset packs that omit the atlas entry.
     */
    private static final VisualSpec KERNEL = new VisualSpec(
            "768/INITIAL/EFFECTS/T_KERNALPULT_PROJECTILE/T_KERNALPULT_PROJECTILE.PAM",
            "animation",
            "IMAGE_EFFECTS_T_KERNALPULT_PROJECTILE_T_KERNALPULT_PROJECTILE_35X37",
            0.37f
    );
    /*
     * Butter is also a standalone atlas region. Keep the kernel PAM as the
     * runtime fallback for reduced asset packs.
     */
    private static final VisualSpec BUTTER = new VisualSpec(
            KERNEL.pamPath(),
            KERNEL.clip(),
            "IMAGE_EFFECTS_KERNELPULT_PROJECTILE_BUTTER",
            0.46f
    );
    private static final VisualSpec PUFF = new VisualSpec(
            "768/INITIAL/EFFECTS/T_PUFFSHROOM_PROJECTILE/T_PUFFSHROOM_PROJECTILE.PAM",
            "animation",
            0.46f
    );
    private static final VisualSpec SEA_SHROOM = new VisualSpec(
            "768/FULL/EFFECTS/SEASHROOM_PROJECTILE/SEASHROOM_PROJECTILE.PAM",
            "animation",
            0.46f
    );
    private static final VisualSpec FUME_SHROOM = new VisualSpec(
            "768/INITIAL/EFFECTS/FUMESHROOM_BUBBLES/FUMESHROOM_BUBBLES.PAM",
            "special",
            0.62f
    );
    private static final VisualSpec MEGA_GATLING = new VisualSpec(
            "768/INITIAL/EFFECTS/MEGAGATLING_PROJECTILE/MEGAGATLING_PROJECTILE.PAM",
            "animation",
            0.48f
    );
    private static final VisualSpec ELECTRIC_BLUEBERRY = new VisualSpec(
            "768/INITIAL/EFFECTS/ELECTRICBLUEBERRY_CLOUD_PROJECTILE/"
                    + "ELECTRICBLUEBERRY_CLOUD_PROJECTILE.PAM",
            "idle",
            0.52f
    );
    private static final VisualSpec CAULIPOWER = new VisualSpec(
            "768/INITIAL/EFFECTS/CAULIPOWER_PROJECTILE/CAULIPOWER_PROJECTILE.PAM",
            "animation",
            0.52f
    );
    private static final VisualSpec BOWLING_BULB_1 = new VisualSpec(
            "768/FULL/EFFECTS/BOWLINGBULB_PROJECTILE1/BOWLINGBULB_PROJECTILE1.PAM",
            "animation",
            0.56f
    );
    private static final VisualSpec BOWLING_BULB_2 = new VisualSpec(
            "768/FULL/EFFECTS/BOWLINGBULB_PROJECTILE2/BOWLINGBULB_PROJECTILE2.PAM",
            "animation",
            0.56f
    );
    private static final VisualSpec BOWLING_BULB_3 = new VisualSpec(
            "768/FULL/EFFECTS/BOWLINGBULB_PROJECTILE3/BOWLINGBULB_PROJECTILE3.PAM",
            "animation",
            0.56f
    );

    private ProjectileAssetCatalog() {
    }

    public static VisualSpec forProjectile(Projectile projectile) {
        if (projectile == null) {
            return PEA;
        }

        PlantType source = projectile.getSourceType();

        // Torchwood-style fire transformation changes the projectile after it
        // leaves the source plant. In the verified reference, transformed
        // pea-family shots switch to the complete T_FIRE_PEA visual.
        if (projectile.getEffect() instanceof FireEffect && isPeaFamily(source)) {
            return FIRE_PEA;
        }

        if (source != null) {
            VisualSpec sourceVisual = switch (source) {
                case PEASHOOTER, REPEATER, THREEPEATER, PEA_POD, SPLIT_PEA -> PEA;
                case MEGA_GATLING_PEA -> MEGA_GATLING;
                case SNOW_PEA -> SNOW_PEA;
                case FIRE_PEASHOOTER -> FIRE_PEA;
                case GOO_PEASHOOTER -> GOO_PEA;
                case ROTOBAGA -> new VisualSpec(
                        "768/FULL/EFFECTS/ROTORUTABAGA_PROJECTILE1/"
                                + "ROTORUTABAGA_PROJECTILE1.PAM",
                        "animation", 0.42f);
                case CITRON -> new VisualSpec(
                        "768/FULL/EFFECTS/T_CITRON_CITRUS_ORB/"
                                + "T_CITRON_CITRUS_ORB.PAM",
                        "Citron_Citrus_Orb", 0.66f);
                case CAULIPOWER -> CAULIPOWER;
                case ELECTRIC_BLUEBERRY -> ELECTRIC_BLUEBERRY;
                case CACTUS -> new VisualSpec(
                        "768/INITIAL/EFFECTS/T_CACTUS_PROJECTILE/"
                                + "T_CACTUS_PROJECTILE.PAM",
                        "idle", 0.44f);
                case STARFRUIT -> new VisualSpec(
                        "768/INITIAL/EFFECTS/T_STARFRUIT_PROJECTILE/"
                                + "T_STARFRUIT_PROJECTILE.PAM",
                        "animation", 0.48f);
                case SEA_SHROOM -> SEA_SHROOM;
                case PUFF_SHROOM -> PUFF;
                case FUME_SHROOM -> FUME_SHROOM;
                case CABBAGE_PULT -> new VisualSpec(
                        "768/INITIAL/EFFECTS/T_CABBAGEPULT_PROJECTILE/"
                                + "T_CABBAGEPULT_PROJECTILE.PAM",
                        "animation", 0.54f);
                case KERNEL_PULT -> projectile.getEffect() instanceof ButterEffect
                        ? BUTTER : KERNEL;
                case MELON_PULT -> new VisualSpec(
                        "768/INITIAL/EFFECTS/T_MELON_PROJECTILE/"
                                + "T_MELON_PROJECTILE.PAM",
                        "animation", 0.64f);
                case WINTER_MELON -> new VisualSpec(
                        "768/FULL/EFFECTS/T_WINTERMELON_PROJECTILE/"
                                + "T_WINTERMELON_PROJECTILE.PAM",
                        "animation", 0.64f);
                case PEPPER_PULT -> new VisualSpec(
                        "768/FULL/EFFECTS/T_PEPPERPULT_PROJECTILE/"
                                + "T_PEPPERPULT_PROJECTILE.PAM",
                        "animation", 0.58f);
                case BOWLING_BULB -> switch (projectile.getVisualVariant()) {
                    case 2 -> BOWLING_BULB_2;
                    case 3 -> BOWLING_BULB_3;
                    default -> BOWLING_BULB_1;
                };
                case GRAPESHOT -> new VisualSpec(
                        "768/INITIAL/EFFECTS/GRAPESHOT_PROJECTILE/"
                                + "GRAPESHOT_PROJECTILE.PAM",
                        "animation_forward", 0.46f);
                case CAT_TAIL -> new VisualSpec(
                        "768/INITIAL/EFFECTS/T_HOMING_THISTLE_PROJECTILE/"
                                + "T_HOMING_THISTLE_PROJECTILE.PAM",
                        "animation", 0.46f);
                default -> null;
            };
            if (sourceVisual != null) {
                return sourceVisual;
            }
        }

        // Effect fallbacks are intentionally after source-specific artwork so
        // Winter Melon / Pepper-pult do not collapse into Snow Pea / Fire Pea.
        if (projectile.getEffect() instanceof IceEffect) {
            return SNOW_PEA;
        }
        if (projectile.getEffect() instanceof PoisonEffect) {
            return GOO_PEA;
        }
        if (projectile.getEffect() instanceof HypnosisEffect) {
            return CAULIPOWER;
        }
        return PEA;
    }

    /**
     * Returns the supplied PvZ2 hit/splat animation for the authoritative
     * impact marker.  Source-specific artwork wins over generic damage-type
     * fallbacks so lobbers keep their own melon/pepper/cabbage splats.
     */
    public static ImpactSpec forImpact(ProjectileImpact impact) {
        if (impact == null) {
            return null;
        }

        PlantType source = impact.getSourceType();
        if (source == PlantType.KERNEL_PULT) {
            ImpactSpec kernelImpact = impact.isButter()
                    ? new ImpactSpec(
                    "768/INITIAL/EFFECTS/SPLAT_KERNALPULT_BUTTER/"
                            + "SPLAT_KERNALPULT_BUTTER.PAM",
                    "animation", 0.34f, 0.67f)
                    : new ImpactSpec(
                    "768/INITIAL/EFFECTS/SPLAT_KERNALPULT_KERNAL/"
                            + "SPLAT_KERNALPULT_KERNAL.PAM",
                    "animation", 0.28f, 0.67f);
            return impact.isSecondary() ? secondaryImpact(kernelImpact) : kernelImpact;
        }

        if (source != null) {
            ImpactSpec sourceImpact = switch (source) {
                case GOO_PEASHOOTER -> new ImpactSpec(
                        "768/INITIAL/EFFECTS/GOOPEASHOOTER_PROJECTILES/"
                                + "GOOPEASHOOTER_PROJECTILES.PAM",
                        "hit_t1", 0.30f, 0.63f);
                case ROTOBAGA -> new ImpactSpec(
                        "768/FULL/EFFECTS/T_ROTORUTABAGA_PROJECTILE_HIT/"
                                + "T_ROTORUTABAGA_PROJECTILE_HIT.PAM",
                        "animation", 0.28f, 0.54f);
                case CITRON -> new ImpactSpec(
                        "768/FULL/EFFECTS/T_CITRON_CITRUS_ORB_HIT/"
                                + "T_CITRON_CITRUS_ORB_HIT.PAM",
                        "animation", 0.42f, 1.30f);
                case ELECTRIC_BLUEBERRY -> new ImpactSpec(
                        "768/INITIAL/EFFECTS/ELECTRICBLUEBERRY_CLOUD_PROJECTILE/"
                                + "ELECTRICBLUEBERRY_CLOUD_PROJECTILE.PAM",
                        "attack", 0.36f, 1.33f);
                case CACTUS -> new ImpactSpec(
                        "768/INITIAL/EFFECTS/CACTUS_PROJECTILE_HIT/"
                                + "CACTUS_PROJECTILE_HIT.PAM",
                        "animation", 0.28f, 0.50f);
                case STARFRUIT -> new ImpactSpec(
                        "768/INITIAL/EFFECTS/T_STARFRUIT_PROJECTILE_HIT/"
                                + "T_STARFRUIT_PROJECTILE_HIT.PAM",
                        "idle", 0.28f, 1.00f);
                case PUFF_SHROOM, SEA_SHROOM -> new ImpactSpec(
                        "768/INITIAL/EFFECTS/T_PUFFSHROOM_HIT/T_PUFFSHROOM_HIT.PAM",
                        "animation", 0.30f, 0.67f);
                case FUME_SHROOM -> new ImpactSpec(
                        "768/INITIAL/EFFECTS/FUMESHROOM_BUBBLES_HIT/"
                                + "FUMESHROOM_BUBBLES_HIT.PAM",
                        "animation", 0.38f, 0.47f);
                case CABBAGE_PULT -> new ImpactSpec(
                        "768/INITIAL/EFFECTS/SPLAT_CABBAGEPULT/SPLAT_CABBAGEPULT.PAM",
                        "animation", 0.32f, 0.67f);
                case MELON_PULT -> new ImpactSpec(
                        "768/INITIAL/EFFECTS/T_SPLAT_MELONPULT/T_SPLAT_MELONPULT.PAM",
                        "animation", 0.38f, 0.67f);
                case WINTER_MELON -> new ImpactSpec(
                        "768/FULL/EFFECTS/T_SPLAT_WINTERMELON/T_SPLAT_WINTERMELON.PAM",
                        "animation", 0.40f, 0.84f);
                case PEPPER_PULT -> new ImpactSpec(
                        "768/FULL/EFFECTS/T_PEPPERPULT_PROJECTILE_SPLAT/"
                                + "T_PEPPERPULT_PROJECTILE_SPLAT.PAM",
                        "animation", 0.38f, 1.27f);
                case GRAPESHOT -> new ImpactSpec(
                        "768/INITIAL/EFFECTS/GRAPESHOT_HIT/GRAPESHOT_HIT.PAM",
                        "animation", 0.32f, 0.90f);
                case CAT_TAIL -> new ImpactSpec(
                        "768/INITIAL/EFFECTS/T_HOMING_THISTLE_PROJECTILE_HIT/"
                                + "T_HOMING_THISTLE_PROJECTILE_HIT.PAM",
                        "animation", 0.28f, 0.54f);
                default -> null;
            };
            if (sourceImpact != null) {
                return impact.isSecondary() ? secondaryImpact(sourceImpact) : sourceImpact;
            }
        }

        // Torchwood can turn any pea-family shot into a fire projectile after
        // launch, so damage type is checked after source-specific exceptions.
        if (impact.getDamageType() == model.enums.DamageType.FIRE) {
            return new ImpactSpec(
                    "768/INITIAL/EFFECTS/T_SPLAT_FIRE_PEA/T_SPLAT_FIRE_PEA.PAM",
                    "animation", 0.32f, 0.63f);
        }
        if (impact.getDamageType() == model.enums.DamageType.ICE) {
            return new ImpactSpec(
                    "768/INITIAL/EFFECTS/T_SPLAT_SNOW_PEA/T_SPLAT_SNOW_PEA.PAM",
                    "animation", 0.30f, 0.84f);
        }
        if (impact.getDamageType() == model.enums.DamageType.POISON) {
            return new ImpactSpec(
                    "768/INITIAL/EFFECTS/GOOPEASHOOTER_PROJECTILES/"
                            + "GOOPEASHOOTER_PROJECTILES.PAM",
                    "hit_t1", 0.30f, 0.63f);
        }
        if (isPeaFamily(source)) {
            return new ImpactSpec(
                    "768/INITIAL/EFFECTS/T_SPLAT_PEA/T_SPLAT_PEA.PAM",
                    "animation", 0.30f, 0.84f);
        }
        return null;
    }

    private static ImpactSpec secondaryImpact(ImpactSpec spec) {
        if (spec == null) {
            return null;
        }
        return new ImpactSpec(
                spec.pamPath(),
                spec.clip(),
                Math.max(0.18f, spec.scale() * 0.72f),
                spec.durationSeconds()
        );
    }

    /**
     * Delay a graphical game projectile until the plant animation reaches the
     * authored release point. Values for lobbers come directly from the PAM
     * use_action frames (30 FPS); normal shooters use the midpoint of the
     * existing 0.6 s attack window.
     */
    public static double releaseDelaySeconds(Projectile projectile) {
        if (projectile == null || projectile.getSourceType() == null) {
            return 0.0;
        }
        return switch (projectile.getSourceType()) {
            case CABBAGE_PULT -> 18.0 / 30.0;
            case KERNEL_PULT -> 19.0 / 30.0;
            case MELON_PULT -> 21.0 / 30.0;
            case WINTER_MELON -> 25.0 / 30.0;
            case PEPPER_PULT -> 13.0 / 30.0;
            // Grapeshot's bouncing grapes are spawned at the detonation frame,
            // after its attack pose has already played.
            case GRAPESHOT -> 0.0;
            case PEASHOOTER, REPEATER, THREEPEATER, SNOW_PEA, ROTOBAGA,
                    PEA_POD, SPLIT_PEA, CITRON, CAULIPOWER,
                    ELECTRIC_BLUEBERRY, BOWLING_BULB, CACTUS,
                    FIRE_PEASHOOTER, STARFRUIT, GOO_PEASHOOTER,
                    MEGA_GATLING_PEA, SEA_SHROOM, PUFF_SHROOM,
                    FUME_SHROOM, CAT_TAIL -> 0.30;
            default -> 0.0;
        };
    }

    /**
     * Keep lobber attack clips visible for one complete authored clip rather
     * than returning to idle before the delayed projectile is released.
     */
    public static double attackWindowSeconds(PlantType type) {
        if (type == null) {
            return 0.60;
        }
        return switch (type) {
            case CABBAGE_PULT -> 50.0 / 30.0;
            case KERNEL_PULT -> 56.0 / 30.0;
            case MELON_PULT -> 59.0 / 30.0;
            case WINTER_MELON -> 65.0 / 30.0;
            case PEPPER_PULT -> 60.0 / 30.0;
            default -> 0.60;
        };
    }

    private static boolean isPeaFamily(PlantType source) {
        return source == PlantType.PEASHOOTER
                || source == PlantType.REPEATER
                || source == PlantType.THREEPEATER
                || source == PlantType.PEA_POD
                || source == PlantType.SPLIT_PEA
                || source == PlantType.MEGA_GATLING_PEA
                || source == PlantType.SNOW_PEA
                || source == PlantType.FIRE_PEASHOOTER
                || source == PlantType.GOO_PEASHOOTER;
    }
}
