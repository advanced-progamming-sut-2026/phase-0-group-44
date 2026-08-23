package screen.gameplay;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.scenes.scene2d.Actor;
import model.enums.ZombieType;
import model.inGame.zombie.Zombie;
import model.inGame.zombie.ZombieArmorPart;
import pvz.libpvz.pam.PamPlayer;

import java.util.HashMap;
import java.util.Map;

/**
 * Pure presentation actor for one model Zombie.
 *
 * It never changes gameplay state. It only:
 *  - asks ZombieGraphicsRegistry which clip represents current model state,
 *  - asks the registry which optional PAM parts should be visible,
 *  - draws the selected PAM.
 */
public final class PamZombieActor extends Actor {
    private static final float HIT_REACTION_SECONDS = 0.16f;
    private static final float PIANO_DAMAGE_SECONDS = 1.0f;
    private static final float GARGANTUAR_SMASH_RATE = 0.80f;
    private static final float ARCADE_CABINET_FRONT_OFFSET = 105f;
    private static final float BARREL_FRONT_OFFSET = 92f;
    private static final float TROGLOBITE_ICE_FRONT_OFFSET = 102f;
    private static final String ARCADE_CABINET_PAM =
            "768/FULL/EFFECTS/80S_ARCADE_CABINET/"
                    + "80S_ARCADE_CABINET.PAM";
    private static final String TROGLOBITE_ICE_PAM =
            "768/FULL/EFFECTS/FROSTBITE_ICE_BLOCK_ZOMBIE/"
                    + "FROSTBITE_ICE_BLOCK_ZOMBIE.PAM";
    private static final String BARREL_PAM =
            "768/FULL/ZOMBIE/ZOMBIE_PIRATE_BARREL_PUSHER_BARREL/"
                    + "ZOMBIE_PIRATE_BARREL_PUSHER_BARREL.PAM";

    private final PamPlayer pamPlayer;

    private Zombie zombie;
    private ZombieAnimationInfo animationInfo;

    private String previewPamPath;
    private String previewClip;
    private boolean previewMode;

    private String currentClip;
    private float stateTime;

    private float scale = 0.52f;
    private float xOffset;
    private float yOffset;
    private boolean playing = true;

    private final Map<String, Boolean> partsVisibility =
            new HashMap<>();
    private final Map<String, Boolean> arcadeCabinetVisibility =
            new HashMap<>();
    private final Map<String, Boolean> troglobiteIceVisibility =
            new HashMap<>();
    private final Map<String, Boolean> barrelVisibility =
            new HashMap<>();

    private boolean visibilityBuilt;
    private int damageVisibilitySignature = Integer.MIN_VALUE;
    private int hitReactionSequence;
    private int gargantuarSmashSequence;
    private float hitReactionRemaining;
    private boolean deathAnimationStarted;
    private float deathAnimationDuration;

    public PamZombieActor(
            PamPlayer pamPlayer,
            Zombie zombie,
            ZombieAnimationInfo animationInfo
    ) {
        this.pamPlayer = pamPlayer;
        this.zombie = zombie;
        this.animationInfo = animationInfo;
        this.currentClip = chooseClip();
        this.hitReactionSequence =
                zombie.getIntState("HIT_REACTION_SEQUENCE", 0);
        this.gargantuarSmashSequence =
                zombie.getIntState("GARGANTUAR_SMASH_SEQUENCE", 0);
    }

    public PamZombieActor(
            PamPlayer pamPlayer,
            String pamPath,
            String clip,
            float scale
    ) {
        this.pamPlayer = pamPlayer;
        this.previewPamPath = pamPath;
        this.previewClip = clip;
        this.scale = scale;
        this.previewMode = true;
        this.currentClip = clip;
    }

    @Override
    public void act(float delta) {
        super.act(delta);

        if (previewMode) {
            if (playing) {
                stateTime += delta;
            }
            return;
        }

        if (zombie == null || animationInfo == null) {
            return;
        }

        updateHitReaction(delta);
        synchronizeGargantuarSmash();

        int nextDamageSignature = damageVisibilitySignature();
        if (nextDamageSignature != damageVisibilitySignature) {
            damageVisibilitySignature = nextDamageSignature;
            visibilityBuilt = false;
        }

        ensurePartVisibility();

        String nextClip = chooseClip();

        if (nextClip != null
                && !nextClip.equals(currentClip)) {
            currentClip = nextClip;
            stateTime = 0f;
        }

        ZombieVisualState visualState =
                ZombieVisualStateResolver.resolve(zombie);

        if (visualState != ZombieVisualState.FROZEN
                && visualState != ZombieVisualState.STUNNED) {
            stateTime += animationDelta(delta);
        }
    }

    private float animationDelta(float delta) {
        if (zombie.getType() == ZombieType.GARGANTUAR
                && zombie.hasState("GARGANTUAR_SMASH_ELAPSED")) {
            return delta * GARGANTUAR_SMASH_RATE;
        }
        return delta;
    }

    private void updateHitReaction(float delta) {
        int nextSequence =
                zombie.getIntState("HIT_REACTION_SEQUENCE", 0);

        if (nextSequence != hitReactionSequence) {
            hitReactionSequence = nextSequence;
            if (supportsHitReaction(zombie.getType())) {
                hitReactionRemaining =
                        zombie.getType() == ZombieType.PIANIST
                                ? PIANO_DAMAGE_SECONDS
                                : HIT_REACTION_SECONDS;
            }
        }

        hitReactionRemaining = Math.max(
                0f,
                hitReactionRemaining - delta
        );
    }

    private void synchronizeGargantuarSmash() {
        if (zombie.getType() != ZombieType.GARGANTUAR) {
            return;
        }

        int nextSequence =
                zombie.getIntState(
                        "GARGANTUAR_SMASH_SEQUENCE",
                        0
                );

        if (nextSequence != gargantuarSmashSequence) {
            gargantuarSmashSequence = nextSequence;
            stateTime = 0f;
        }
    }

    private static boolean supportsHitReaction(ZombieType type) {
        return type == ZombieType.IMP
                || type == ZombieType.ALL_STAR
                || type == ZombieType.ARCADE_ZOMBIE
                || type == ZombieType.PARASOL_ZOMBIE
                || type == ZombieType.TURQUOISE_ZOMBIE
                || type == ZombieType.PROSPECTOR
                || type == ZombieType.PIANIST
                || type == ZombieType.NEWSPAPER_ZOMBIE
                || type == ZombieType.BARREL_ROLLER
                || type == ZombieType.RA_ZOMBIE
                || type == ZombieType.EXPLORER
                || type == ZombieType.TOMBRAISER
                || type == ZombieType.DODO_RIDER
                || type == ZombieType.HUNTER
                || type == ZombieType.TROGLOBITE
                || type == ZombieType.FISHERMAN
                || type == ZombieType.SNORKEL
                || type == ZombieType.OCTOPUS_ZOMBIE
                || type == ZombieType.JESTER
                || type == ZombieType.WIZARD
                || type == ZombieType.KING
                || type == ZombieType.DRAGON_IMP;
    }

    @Override
    public void draw(
            Batch batch,
            float parentAlpha
    ) {
        if (pamPlayer == null) {
            return;
        }

        if (previewMode) {
            drawPreview(batch);
        } else {
            drawGameplay(batch, parentAlpha);
        }
    }

    private void drawPreview(Batch batch) {
        if (previewPamPath == null
                || previewClip == null) {
            return;
        }

        float centerX =
                getX() + getWidth() * 0.5f + xOffset;

        float centerY =
                getY() + getHeight() * 0.5f + yOffset;

        pamPlayer.draw(
                batch,
                previewPamPath,
                previewClip,
                stateTime,
                centerX,
                centerY,
                scale,
                scale,
                true
        );
    }

    private void drawGameplay(Batch batch, float parentAlpha) {
        if (zombie == null
                || animationInfo == null
                || currentClip == null) {
            return;
        }

        float centerX =
                getX() + getWidth() * 0.5f + xOffset;

        float centerY =
                getY() + getHeight() * 0.5f + yOffset;

        if (hitReactionRemaining > 0f
                && zombie.getType() != ZombieType.PIANIST) {
            float progress =
                    1f - hitReactionRemaining
                            / HIT_REACTION_SECONDS;
            centerX += MathUtils.sin(
                    progress * MathUtils.PI * 4f
            ) * 4f;
        }

        float horizontalScale =
                zombie.getDirection() < 0
                        ? scale
                        : -scale;

        Matrix4 originalTransform =
                new Matrix4(
                        batch.getTransformMatrix()
                );

        Matrix4 zombieTransform =
                new Matrix4(originalTransform);

        zombieTransform.translate(
                centerX,
                centerY,
                0f
        );

        zombieTransform.scale(
                horizontalScale,
                scale,
                1f
        );

        zombieTransform.translate(
                -centerX,
                -centerY,
                0f
        );

        batch.setTransformMatrix(
                zombieTransform
        );

        drawTroglobiteIce(
                batch,
                centerX,
                centerY
        );

        drawBarrel(
                batch,
                centerX,
                centerY
        );

        drawArcadeCabinet(
                batch,
                centerX,
                centerY
        );

        /*
         * PAM rendering must not inherit whatever tint/alpha the previously drawn
         * Scene2D actor left on the shared Batch. Night Ops deliberately ends its
         * environment layer with translucent blue overlays; without resetting the
         * Batch here, zombies inherit that alpha and look like ghosts.
         */
        Color originalBatchColor = new Color(batch.getColor());
        Color actorColor = getColor();
        float actorAlpha = actorColor.a * parentAlpha;
        batch.setColor(actorColor.r, actorColor.g, actorColor.b, actorAlpha);

        if (zombie.getBooleanState("GLOWING")) {
            float pulse = MathUtils.sin(stateTime * 7.0f) * 0.5f + 0.5f;
            batch.setColor(
                    1f,
                    0.78f + 0.18f * pulse,
                    0.30f + 0.24f * pulse,
                    actorAlpha
            );
        }

        pamPlayer.draw(
                batch,
                animationInfo.getPath(),
                currentClip,
                stateTime,
                centerX,
                centerY,
                shouldLoopCurrentClip(),
                partsVisibility.isEmpty()
                        ? null
                        : partsVisibility
        );

        batch.setColor(originalBatchColor);
        batch.setTransformMatrix(
                originalTransform
        );
    }

    private boolean shouldLoopCurrentClip() {
        if (zombie.isDead()) {
            return false;
        }

        return zombie.getType() != ZombieType.GARGANTUAR
                || !zombie.getBooleanState("EATING")
                || currentClip == null
                || !currentClip.toLowerCase().contains("smash");
    }

    private void drawArcadeCabinet(
            Batch batch,
            float centerX,
            float centerY
    ) {
        if (zombie.getType() != ZombieType.ARCADE_ZOMBIE
                || !zombie.getBooleanState("PUSHING")) {
            return;
        }

        pamPlayer.draw(
                batch,
                ARCADE_CABINET_PAM,
                "active",
                stateTime,
                centerX - ARCADE_CABINET_FRONT_OFFSET,
                centerY,
                true,
                buildArcadeCabinetVisibility()
        );
    }

    private Map<String, Boolean> buildArcadeCabinetVisibility() {
        arcadeCabinetVisibility.clear();

        int stage = arcadeCabinetDamageStage();
        for (int i = 0; i <= 5; i++) {
            arcadeCabinetVisibility.put(
                    "arcade_cabinet_active_damage" + i,
                    i == stage
            );
        }

        return arcadeCabinetVisibility;
    }

    private int arcadeCabinetDamageStage() {
        ZombieArmorPart machine =
                zombie.findArmorPart("arcadeMachine");

        if (machine == null || machine.getMaxHealth() <= 0) {
            return 5;
        }

        double damageRatio =
                1.0 - (double) machine.getHealth()
                        / machine.getMaxHealth();

        if (damageRatio < 280.0 / 1200.0) {
            return 0;
        }
        if (damageRatio < 560.0 / 1200.0) {
            return 1;
        }
        if (damageRatio < 840.0 / 1200.0) {
            return 2;
        }
        if (damageRatio < 1120.0 / 1200.0) {
            return 3;
        }
        return machine.isBroken() ? 5 : 4;
    }

    private void drawTroglobiteIce(
            Batch batch,
            float centerX,
            float centerY
    ) {
        ZombieArmorPart ice = zombie.findArmorPart("groundIce");
        if (zombie.getType() != ZombieType.TROGLOBITE
                || ice == null
                || ice.isBroken()) {
            return;
        }

        pamPlayer.draw(
                batch,
                TROGLOBITE_ICE_PAM,
                "idle",
                stateTime,
                centerX - TROGLOBITE_ICE_FRONT_OFFSET,
                centerY,
                true,
                buildTroglobiteIceVisibility(ice)
        );
    }

    private Map<String, Boolean> buildTroglobiteIceVisibility(
            ZombieArmorPart ice
    ) {
        troglobiteIceVisibility.clear();

        double damageRatio =
                1.0 - (double) ice.getHealth()
                        / ice.getMaxHealth();
        int stage = Math.min(
                6,
                Math.max(0, (int) Math.ceil(damageRatio * 6.0))
        );

        troglobiteIceVisibility.put("ice_block_full", stage == 0);
        for (int i = 0; i <= 5; i++) {
            troglobiteIceVisibility.put(
                    "ice_block_damage" + i,
                    stage > 0 && i == stage - 1
            );
        }

        return troglobiteIceVisibility;
    }

    private void drawBarrel(
            Batch batch,
            float centerX,
            float centerY
    ) {
        ZombieArmorPart barrel = zombie.findArmorPart("barrel");
        if (zombie.getType() != ZombieType.BARREL_ROLLER
                || barrel == null
                || barrel.isBroken()) {
            return;
        }

        pamPlayer.draw(
                batch,
                BARREL_PAM,
                "roll",
                stateTime,
                centerX - BARREL_FRONT_OFFSET,
                centerY,
                true,
                buildBarrelVisibility(barrel)
        );
    }

    private Map<String, Boolean> buildBarrelVisibility(
            ZombieArmorPart barrel
    ) {
        barrelVisibility.clear();

        int stage = armorDamageStage(
                barrel.getHealth(),
                barrel.getMaxHealth()
        );

        barrelVisibility.put("barrel_front_normal", stage == 0);
        barrelVisibility.put("barrel_front_damage_01", stage == 1);
        barrelVisibility.put("barrel_front_damage_02", stage == 2);

        return barrelVisibility;
    }

    private void ensurePartVisibility() {
        if (visibilityBuilt) {
            return;
        }

        visibilityBuilt = true;
        partsVisibility.clear();

        if (zombie == null
                || animationInfo == null
                || pamPlayer == null) {
            return;
        }

        String[] wantedTokens =
                ZombieGraphicsRegistry.visiblePartTokens(
                        zombie.getType() == ZombieType.NORMAL
                                && zombie.getBooleanState("KING_PROMOTED")
                                ? ZombieType.KNIGHT
                                : zombie.getType()
                );

        PamPlayer.AnimationPart root =
                pamPlayer.getParts(
                        animationInfo.getPath()
                );

        if (root == null) {
            return;
        }

        if (wantedTokens.length > 0) {
            enableMatchingHierarchy(
                    root,
                    wantedTokens,
                    false
            );
        }

        applyBasicZombieDamageVisibility(root);

        if (!partsVisibility.isEmpty()) {
            System.out.println(
                    "[ZombieGraphics] "
                            + zombie.getType()
                            + " PART VISIBILITY = "
                            + partsVisibility
            );
        }
    }

    private int damageVisibilitySignature() {
        if (zombie == null) {
            return 0;
        }

        int signature =
                switch (zombie.getType()) {
                    case ALL_STAR -> allStarDamageStage();
                    case DODO_RIDER -> dodoDamageStage();
                    default -> bodyDamageStage();
                };

        for (ZombieArmorPart part : zombie.getArmorParts()) {
            signature = 31 * signature + part.getHealth();
        }

        signature = 31 * signature
                + (zombie.getBooleanState("IMP_THROWN") ? 1 : 0);

        return signature;
    }

    private int bodyDamageStage() {
        if (zombie == null
                || zombie.getMaxHealth() <= 0
                || zombie.isDead()) {
            return 0;
        }

        int armDropHealth =
                switch (zombie.getType()) {
                    case GARGANTUAR, DODO_RIDER, PIANIST -> -1;
                    case ALL_STAR -> 525;
                    case PROSPECTOR -> 105;
                    default -> zombie.getMaxHealth() / 2;
                };

        return armDropHealth >= 0
                && zombie.getHealth() <= armDropHealth
                ? 1
                : 0;
    }

    private int dodoDamageStage() {
        if (zombie == null
                || zombie.getMaxHealth() <= 0
                || zombie.isDead()) {
            return 0;
        }

        double ratio =
                (double) zombie.getHealth()
                        / zombie.getMaxHealth();

        if (ratio > 2.0 / 3.0) {
            return 0;
        }
        if (ratio > 1.0 / 3.0) {
            return 1;
        }
        return 2;
    }

    private int armorDamageStage() {
        if (zombie == null) {
            return 3;
        }

        int health = 0;
        int maxHealth = 0;

        for (ZombieArmorPart part : zombie.getArmorParts()) {
            health += part.getHealth();
            maxHealth += part.getMaxHealth();
        }

        if (maxHealth <= 0 || health <= 0) {
            return 3;
        }

        return armorDamageStage(health, maxHealth);
    }

    private void applyBasicZombieDamageVisibility(
            PamPlayer.AnimationPart root
    ) {
        ZombieType type = zombie.getType();

        if (type == ZombieType.NORMAL
                && zombie.getBooleanState("KING_PROMOTED")) {
            applyKnightVisibility(root);
        } else if (type == ZombieType.CONEHEAD) {
            selectArmorVariant(
                    root,
                    "ZOMBIE_ARMOR_CONE_NORM",
                    "ZOMBIE_ARMOR_CONE_DAMAGE_01",
                    "ZOMBIE_ARMOR_CONE_DAMAGE_02"
            );
        } else if (type == ZombieType.BUCKETHEAD) {
            selectArmorVariant(
                    root,
                    "ZOMBIE_ARMOR_BUCKET_NORM",
                    "ZOMBIE_ARMOR_BUCKET_DAMAGE_01",
                    "ZOMBIE_ARMOR_BUCKET_DAMAGE_02"
            );
        } else if (type == ZombieType.KNIGHT) {
            applyKnightVisibility(root);
        } else if (type == ZombieType.BLOCKHEAD) {
            applyBlockheadVisibility(root);
        } else if (type == ZombieType.ALL_STAR
                && !zombie.isDead()) {
            applyAllStarDamageVisibility(root);
        } else if (type == ZombieType.NEWSPAPER_ZOMBIE) {
            applyNewspaperVisibility(root);
        } else if (type == ZombieType.BARREL_ROLLER
                && zombie.findArmorPart("barrel") != null
                && !zombie.findArmorPart("barrel").isBroken()) {
            hideMainBarrel(root);
        } else if (type == ZombieType.GARGANTUAR
                && zombie.getBooleanState("IMP_THROWN")) {
            setAllMatchingSubtreeVisibility(
                    root,
                    "ZOMBIE_IMP",
                    false
            );
        }

        if (zombie.isDead()) {
            return;
        }

        if (type == ZombieType.DODO_RIDER) {
            applyDodoDamageVisibility(root, dodoDamageStage());
            return;
        }

        if (type != ZombieType.NORMAL
                && type != ZombieType.CONEHEAD
                && type != ZombieType.BUCKETHEAD
                && type != ZombieType.KNIGHT
                && type != ZombieType.BLOCKHEAD
                && type != ZombieType.GARGANTUAR
                && type != ZombieType.IMP
                && type != ZombieType.ARCADE_ZOMBIE
                && type != ZombieType.PARASOL_ZOMBIE
                && type != ZombieType.TURQUOISE_ZOMBIE
                && type != ZombieType.PROSPECTOR
                && type != ZombieType.NEWSPAPER_ZOMBIE
                && type != ZombieType.BARREL_ROLLER
                && type != ZombieType.RA_ZOMBIE
                && type != ZombieType.EXPLORER
                && type != ZombieType.TOMBRAISER
                && type != ZombieType.HUNTER
                && type != ZombieType.TROGLOBITE
                && type != ZombieType.FISHERMAN
                && type != ZombieType.SNORKEL
                && type != ZombieType.OCTOPUS_ZOMBIE
                && type != ZombieType.JESTER
                && type != ZombieType.WIZARD
                && type != ZombieType.KING
                && type != ZombieType.DRAGON_IMP) {
            return;
        }

        int bodyStage = bodyDamageStage();

        applyBodyDamageVisibility(root, type, bodyStage);
    }

    private void hideMainBarrel(PamPlayer.AnimationPart root) {
        hideArmorFamily(root, "BARREL_FRONT");
        hideArmorFamily(root, "BARREL_SIDE");
        hideArmorFamily(root, "BARREL_SLAT");
        hideArmorFamily(root, "BARREL_EYES");
    }

    private void applyBodyDamageVisibility(
            PamPlayer.AnimationPart root,
            ZombieType type,
            int bodyStage
    ) {
        if (bodyStage < 1 || type == ZombieType.GARGANTUAR) {
            return;
        }

        if (type == ZombieType.NORMAL
                && zombie.getBooleanState("KING_PROMOTED")) {
            showDamagedOuterArm(
                    root,
                    "ZOMBIE_ARM_OUTER_UPPER",
                    "ZOMBIE_ARM_OUTER_UPPER_BONE",
                    "ZOMBIE_ARM_OUTER_LOWER",
                    "ZOMBIE_HAND_OUTER"
            );
            return;
        }

        switch (type) {
            case NORMAL, CONEHEAD, BUCKETHEAD ->
                    showDamagedOuterArm(
                            root,
                            "ZOMBIE_EGYPT_ARM_OUTER_UPPER",
                            "ZOMBIE_EGYPT_ARM_OUTER_UPPER_BONE",
                            "ZOMBIE_EGYPT_ARM_OUTER_LOWER",
                            "ZOMBIE_EGYPT_HAND_OUTER"
                    );
            case KNIGHT, BLOCKHEAD, ALL_STAR ->
                    showDamagedOuterArm(
                            root,
                            "ZOMBIE_ARM_OUTER_UPPER",
                            "ZOMBIE_ARM_OUTER_UPPER_BONE",
                            "ZOMBIE_ARM_OUTER_LOWER",
                            "ZOMBIE_HAND_OUTER"
                    );
            case IMP ->
                    showDamagedOuterArm(
                            root,
                            "ZOMBIE_IMP_ARM_OUTER_UPPER_01",
                            "ZOMBIE_IMP_ARM_OUTER_UPPER_02",
                            "ZOMBIE_IMP_ARM_OUTER_LOWER",
                            "ZOMBIE_IMP_HAND_OUTER"
                    );
            case ARCADE_ZOMBIE ->
                    showDamagedOuterArm(
                            root,
                            "ZOMBIE_TROGLOBITE_ARM_OUTER_UPPER",
                            "ZOMBIE_TROGLOBITE_ARM_OUTER_UPPER_BONE",
                            "ZOMBIE_TROGLOBITE_ARM_OUTER_LOWER",
                            "ZOMBIE_TROGLOBITE_HAND_OUTER"
                    );
            case PARASOL_ZOMBIE ->
                    showDamagedParasolArm(root);
            case TURQUOISE_ZOMBIE ->
                    showDamagedOuterArm(
                            root,
                            "ZOMBIE_EGYPT_RA_ARM_OUTER_UPPER_01",
                            "ZOMBIE_EGYPT_RA_ARM_OUTER_UPPER_02",
                            "ZOMBIE_EGYPT_RA_ARM_OUTER_LOWER",
                            "ZOMBIE_EGYPT_RA_HAND_OUTER"
                    );
            case PROSPECTOR ->
                    showDamagedOuterArm(
                            root,
                            "ZOMBIE_PROS_ARM_OUTER_UPPER_01",
                            "ZOMBIE_PROS_ARM_OUTER_UPPER_02",
                            "ZOMBIE_PROS_ARM_OUTER_LOWER",
                            "ZOMBIE_PROS_HAND_OUTER"
                    );
            case NEWSPAPER_ZOMBIE, BARREL_ROLLER,
                 RA_ZOMBIE, EXPLORER, TOMBRAISER ->
                    applySecondGroupDamage(root, type);
            case HUNTER, TROGLOBITE, SNORKEL ->
                    applyFrostbiteDamage(root, type);
            case FISHERMAN, OCTOPUS_ZOMBIE ->
                    applyBeachDamage(root, type);
            case JESTER, WIZARD, KING, DRAGON_IMP ->
                    applyDarkAgesDamage(root, type);
            default -> { }
        }
    }

    private void applySecondGroupDamage(
            PamPlayer.AnimationPart root,
            ZombieType type
    ) {
        switch (type) {
            case NEWSPAPER_ZOMBIE ->
                    showDamagedOuterArm(
                            root,
                            "ZOMBIE_ARM_OUTER_UPPER",
                            "ZOMBIE_ARM_OUTER_UPPER_BONE",
                            "ZOMBIE_ARM_OUTER_LOWER",
                            "ZOMBIE_HAND_OUTER"
                    );
            case BARREL_ROLLER ->
                    showDamagedOuterArm(
                            root,
                            "ZOMBIE_BARREL_ARM_OUTER_UPPER_01",
                            "ZOMBIE_BARREL_ARM_OUTER_UPPER_02",
                            "ZOMBIE_BARREL_ARM_OUTER_LOWER",
                            "ZOMBIE_BARREL_HAND_OUTER"
                    );
            case RA_ZOMBIE ->
                    showDamagedOuterArm(
                            root,
                            "ZOMBIE_EGYPT_RA_ARM_OUTER_UPPER_01",
                            "ZOMBIE_EGYPT_RA_ARM_OUTER_UPPER_02",
                            "ZOMBIE_EGYPT_RA_ARM_OUTER_LOWER",
                            "ZOMBIE_EGYPT_RA_HAND_OUTER"
                    );
            case EXPLORER ->
                    showDamagedOuterArm(
                            root,
                            "ZOMBIE_EXPL_ARM_OUTER_UPPER_01",
                            "ZOMBIE_EXPL_ARM_OUTER_UPPER_02",
                            "ZOMBIE_EXPL_ARM_OUTER_LOWER",
                            "ZOMBIE_EXPL_HAND_OUTER"
                    );
            case TOMBRAISER ->
                    showDamagedOuterArm(
                            root,
                            "ZOMBIE_EGYPT_TR_ARM_OUTER_UPPER_01",
                            "ZOMBIE_EGYPT_TR_ARM_OUTER_UPPER_02",
                            "ZOMBIE_EGYPT_TR_ARM_OUTER_LOWER",
                            "ZOMBIE_EGYPT_TR_HAND_OUTER"
                    );
            default -> { }
        }
    }

    private void applyFrostbiteDamage(
            PamPlayer.AnimationPart root,
            ZombieType type
    ) {
        switch (type) {
            case HUNTER ->
                    showDamagedOuterArm(
                            root,
                            "ZOMBIE_ARM_OUTER_UPPER",
                            "ZOMBIE_ARM_OUTER_UPPER_BONE",
                            "ZOMBIE_ARM_OUTER_LOWER",
                            "ZOMBIE_HAND_OUTER"
                    );
            case TROGLOBITE ->
                    showDamagedOuterArm(
                            root,
                            "ZOMBIE_TROGLOBITE_ARM_OUTER_UPPER",
                            "ZOMBIE_TROGLOBITE_ARM_OUTER_UPPER_BONE",
                            "ZOMBIE_TROGLOBITE_ARM_OUTER_LOWER",
                            "ZOMBIE_TROGLOBITE_HAND_OUTER"
                    );
            case SNORKEL ->
                    showSnorkelDamagedArm(root);
            default -> { }
        }
    }

    private void applyBeachDamage(
            PamPlayer.AnimationPart root,
            ZombieType type
    ) {
        switch (type) {
            case FISHERMAN ->
                    showDamagedOuterArm(
                            root,
                            "ZOMBIE_ARM_OUTER_UPPER",
                            "ZOMBIE_ARM_OUTER_UPPER_BONE",
                            "ZOMBIE_ARM_OUTER_LOWER",
                            "ZOMBIE_HAND_OUTER"
                    );
            case OCTOPUS_ZOMBIE ->
                    showDamagedOuterArm(
                            root,
                            "ZOMBIE_OCTO_ARM_OUTER_UPPER",
                            "ZOMBIE_OCTO_ARM_OUTER_UPPER_BONE",
                            "ZOMBIE_OCTO_ARM_OUTER_LOWER",
                            "ZOMBIE_OCTO_HAND_OUTER"
                    );
            default -> { }
        }
    }

    private void applyDarkAgesDamage(
            PamPlayer.AnimationPart root,
            ZombieType type
    ) {
        switch (type) {
            case JESTER ->
                    showDamagedOuterArm(
                            root,
                            "ZOMBIE_PROS_ARM_OUTER_UPPER_01",
                            "ZOMBIE_PROS_ARM_OUTER_UPPER_02",
                            "ZOMBIE_PROS_ARM_OUTER_LOWER",
                            "ZOMBIE_PROS_HAND_OUTER"
                    );
            case WIZARD ->
                    hideDroppedOuterArm(
                            root,
                            "ZOMBIE_EGYPTFLAG_ARM_OUTER_UPPER",
                            "ZOMBIE_EGYPTFLAG_ARM_OUTER_LOWER",
                            "ZOMBIE_EGYPTFLAG_HAND_OUTER"
                    );
            case KING ->
                    hideDroppedOuterArm(
                            root,
                            "KING_ARM_FRONT_UPPER",
                            "KING_ARM_FRONT_LOWER",
                            "KING_HAND_FRONT"
                    );
            case DRAGON_IMP ->
                    showDamagedOuterArm(
                            root,
                            "ZOMBIE_IMP_ARM_OUTER_UPPER_01",
                            "ZOMBIE_IMP_ARM_OUTER_UPPER_02",
                            "ZOMBIE_IMP_ARM_OUTER_LOWER",
                            "ZOMBIE_IMP_HAND_OUTER"
                    );
            default -> { }
        }
    }

    private void showSnorkelDamagedArm(
            PamPlayer.AnimationPart root
    ) {
        setAllMatchingSubtreeVisibility(
                root,
                "ZOMBIE_SNORKELER_ARM_OUTER_LOWER",
                false
        );
        setAllMatchingSubtreeVisibility(
                root,
                "ZOMBIE_SNORKELER_HAND_OUTER",
                false
        );
        setExactPartVisibility(
                root,
                "ZOMBIE_SNORKELER_ARM_OUTER_UPPER_03",
                true
        );
    }

    private void hideDroppedOuterArm(
            PamPlayer.AnimationPart root,
            String upperArmFamily,
            String lowerArmFamily,
            String handFamily
    ) {
        setAllMatchingSubtreeVisibility(root, upperArmFamily, false);
        setAllMatchingSubtreeVisibility(root, lowerArmFamily, false);
        setAllMatchingSubtreeVisibility(root, handFamily, false);
    }

    private void showDamagedParasolArm(
            PamPlayer.AnimationPart root
    ) {
        setExactPartVisibility(
                root,
                "ZOMBIE_ARM_OUTER_UPPER",
                false
        );
        setExactPartVisibility(
                root,
                "ZOMBIE_ARM_OUTER_UPPER_BONE",
                true
        );
        setAllMatchingSubtreeVisibility(
                root,
                "ZOMBIE_ARM_OUTER_LOWER",
                false
        );
        // PvZ2 deliberately leaves her glove holding the parasol in place.
    }

    private void showDamagedOuterArm(
            PamPlayer.AnimationPart root,
            String intactUpperArm,
            String damagedUpperArm,
            String lowerArmFamily,
            String handFamily
    ) {
        setExactPartVisibility(root, intactUpperArm, false);
        setExactPartVisibility(root, damagedUpperArm, true);
        setAllMatchingSubtreeVisibility(root, lowerArmFamily, false);
        setAllMatchingSubtreeVisibility(root, handFamily, false);
    }

    private int allStarDamageStage() {
        if (zombie == null || zombie.getMaxHealth() <= 0) {
            return 0;
        }

        double ratio =
                (double) zombie.getHealth()
                        / zombie.getMaxHealth();

        if (ratio > 0.75) {
            return 0;
        }
        if (ratio > 0.50) {
            return 1;
        }
        if (ratio > 0.25) {
            return 2;
        }
        return 3;
    }

    private void applyAllStarDamageVisibility(
            PamPlayer.AnimationPart root
    ) {
        int stage = allStarDamageStage();

        setMatchingSubtreeVisibility(
                root,
                "ALLSTAR_HEAD_HELMET_PARTICLE",
                stage < 1
        );

        if (stage >= 2) {
            setAllMatchingSubtreeVisibility(
                    root,
                    "ZOMBIE_ARM_OUTER_UPPER",
                    false
            );
            setAllMatchingSubtreeVisibility(
                    root,
                    "ZOMBIE_ARM_OUTER_LOWER",
                    false
            );
            setAllMatchingSubtreeVisibility(
                    root,
                    "ZOMBIE_HAND_OUTER_01",
                    false
            );
        }

        if (stage >= 3) {
            setAllMatchingSubtreeVisibility(
                    root,
                    "ZOMBIE_SKULL",
                    false
            );
            setAllMatchingSubtreeVisibility(
                    root,
                    "ZOMBIE_JAW",
                    false
            );
        }
    }

    private void applyNewspaperVisibility(
            PamPlayer.AnimationPart root
    ) {
        int stage = armorDamageStage("newspaper");

        setExactPartVisibility(root, "_ZOMBIE_NEWSPAPER", false);
        setExactPartVisibility(root, "_ZOMBIE_NEWSPAPER_DMG1", false);
        setExactPartVisibility(root, "_ZOMBIE_NEWSPAPER_DMG2", false);

        String selected =
                switch (stage) {
                    case 0 -> "_ZOMBIE_NEWSPAPER";
                    case 1 -> "_ZOMBIE_NEWSPAPER_DMG1";
                    case 2 -> "_ZOMBIE_NEWSPAPER_DMG2";
                    default -> null;
                };

        if (selected != null) {
            setExactPartVisibility(root, selected, true);
        }
    }

    private void applyDodoDamageVisibility(
            PamPlayer.AnimationPart root,
            int stage
    ) {
        selectDodoDamagePart(
                root,
                "DODO_BODY_BACK",
                "DAMAGE1_DODO_BODY_BACK",
                null,
                stage
        );
        selectDodoDamagePart(
                root,
                "DODO_BODY_FRONT",
                "DAMAGE1_DODO_BODY_FRONT",
                "DAMAGE2_DODO_BODY_FRONT",
                stage
        );
        selectDodoDamagePart(
                root,
                "DODO_NECK",
                "DAMAGE1_DODO_NECK",
                "DAMAGE2_DODO_NECK",
                stage
        );
        selectDodoDamagePart(
                root,
                "DODO_TAIL",
                "DAMAGE1_DODO_TAIL",
                "DAMAGE2_DODO_TAIL",
                stage
        );
        selectDodoDamagePart(
                root,
                "DODO_THIGH_FRONT",
                "DAMAGE1_DODO_THIGH_FRONT",
                "DAMAGE2_DODO_THIGH_FRONT",
                stage
        );
        selectDodoDamagePart(
                root,
                "DODO_WING_FRONT",
                "DAMAGE1_DODO_WING_FRONT",
                "DAMAGE2_DODO_WING_FRONT",
                stage
        );
    }

    private void selectDodoDamagePart(
            PamPlayer.AnimationPart root,
            String normalPart,
            String damageOnePart,
            String damageTwoPart,
            int stage
    ) {
        setExactPartVisibility(root, normalPart, false);
        setExactPartVisibility(root, damageOnePart, false);
        if (damageTwoPart != null) {
            setExactPartVisibility(root, damageTwoPart, false);
        }

        String selected =
                switch (stage) {
                    case 0 -> normalPart;
                    case 1 -> damageOnePart;
                    default -> damageTwoPart == null
                            ? damageOnePart
                            : damageTwoPart;
                };

        setExactPartVisibility(root, selected, true);
    }

    private void applyKnightVisibility(
            PamPlayer.AnimationPart root
    ) {
        hideArmorFamily(root, "ZOMBIE_ARMOR_CONE");
        hideArmorFamily(root, "ZOMBIE_ARMOR_BUCKET");

        selectArmorVariant(
                root,
                "ZOMBIE_ARMOR_CROWN_NORM",
                "ZOMBIE_ARMOR_CROWN_DAMAGE_01",
                "ZOMBIE_ARMOR_CROWN_DAMAGE_02",
                armorDamageStage("helmet")
        );

        selectArmorVariant(
                root,
                "ZOMBIE_SHOULDER_ARMOR_NORM",
                "ZOMBIE_SHOULDER_ARMOR_DAMAGE_01",
                "ZOMBIE_SHOULDER_ARMOR_DAMAGE_02",
                armorDamageStage("helmet")
        );

        setMatchingSubtreeVisibility(
                root,
                "KNIGHT_FEATHER",
                zombie.findArmorPart("helmet") != null
                        && !zombie.findArmorPart("helmet").isBroken()
        );
    }

    private void applyBlockheadVisibility(
            PamPlayer.AnimationPart root
    ) {
        setAllMatchingSubtreeVisibility(root, "FLAG", false);
        hideArmorFamily(root, "ZOMBIE_ARMOR_CONE");
        hideArmorFamily(root, "ZOMBIE_ARMOR_BUCKET");
        hideArmorFamily(root, "ZOMBIE_ARMOR_BRICK");

        selectArmorVariant(
                root,
                "ZOMBIE_ROMAN_HELMET_NORM",
                "ZOMBIE_ROMAN_HELMET_DAMAGE_01",
                "ZOMBIE_ROMAN_HELMET_DAMAGE_02",
                armorDamageStage("block")
        );
    }

    private void hideArmorFamily(
            PamPlayer.AnimationPart root,
            String familyToken
    ) {
        setAllMatchingSubtreeVisibility(
                root,
                familyToken,
                false
        );
    }

    private int armorDamageStage(String armorName) {
        ZombieArmorPart part = zombie.findArmorPart(armorName);

        if (part == null || part.isBroken()) {
            return 3;
        }

        return armorDamageStage(
                part.getHealth(),
                part.getMaxHealth()
        );
    }

    private int armorDamageStage(
            int health,
            int maxHealth
    ) {
        if (health <= 0 || maxHealth <= 0) {
            return 3;
        }

        int damage = maxHealth - health;

        if (maxHealth == 370) {
            if (damage < 130) {
                return 0;
            }
            return damage < 260 ? 1 : 2;
        }

        if (maxHealth == 1100) {
            if (damage < 350) {
                return 0;
            }
            return damage < 700 ? 1 : 2;
        }

        if (maxHealth == 1600) {
            if (damage < 534) {
                return 0;
            }
            return damage < 1067 ? 1 : 2;
        }

        double ratio = (double) health / maxHealth;
        if (ratio > 2.0 / 3.0) {
            return 0;
        }
        if (ratio > 1.0 / 3.0) {
            return 1;
        }
        return 2;
    }

    private void setAllMatchingSubtreeVisibility(
            PamPlayer.AnimationPart part,
            String wantedToken,
            boolean visible
    ) {
        if (part == null) {
            return;
        }

        if (normalize(part.name).contains(
                normalize(wantedToken)
        )) {
            setSubtreeVisibility(part, visible);
            return;
        }

        if (part.children != null) {
            for (PamPlayer.AnimationPart child
                    : part.children) {
                setAllMatchingSubtreeVisibility(
                        child,
                        wantedToken,
                        visible
                );
            }
        }
    }

    private boolean setExactPartVisibility(
            PamPlayer.AnimationPart part,
            String wantedName,
            boolean visible
    ) {
        if (part == null) {
            return false;
        }

        if (normalizeSymbol(part.name).equals(
                normalize(wantedName)
        )) {
            setSubtreeVisibility(part, visible);
            return true;
        }

        if (part.children != null) {
            for (PamPlayer.AnimationPart child
                    : part.children) {
                if (setExactPartVisibility(
                        child,
                        wantedName,
                        visible
                )) {
                    if (visible
                            && part.name != null
                            && !part.name.isBlank()) {
                        partsVisibility.put(part.name, true);
                    }
                    return true;
                }
            }
        }

        return false;
    }

    private void selectArmorVariant(
            PamPlayer.AnimationPart root,
            String normalToken,
            String damageOneToken,
            String damageTwoToken
    ) {
        selectArmorVariant(
                root,
                normalToken,
                damageOneToken,
                damageTwoToken,
                armorDamageStage()
        );
    }

    private void selectArmorVariant(
            PamPlayer.AnimationPart root,
            String normalToken,
            String damageOneToken,
            String damageTwoToken,
            int damageStage
    ) {
        setMatchingSubtreeVisibility(root, normalToken, false);
        setMatchingSubtreeVisibility(root, damageOneToken, false);
        setMatchingSubtreeVisibility(root, damageTwoToken, false);

        String selectedToken =
                switch (damageStage) {
                    case 0 -> normalToken;
                    case 1 -> damageOneToken;
                    case 2 -> damageTwoToken;
                    default -> null;
                };

        if (selectedToken != null) {
            setMatchingSubtreeVisibility(
                    root,
                    selectedToken,
                    true
            );
        }
    }

    private boolean setMatchingSubtreeVisibility(
            PamPlayer.AnimationPart part,
            String wantedToken,
            boolean visible
    ) {
        if (part == null) {
            return false;
        }

        if (normalize(part.name).contains(
                normalize(wantedToken)
        )) {
            setSubtreeVisibility(part, visible);
            return true;
        }

        if (part.children != null) {
            for (PamPlayer.AnimationPart child
                    : part.children) {
                if (setMatchingSubtreeVisibility(
                        child,
                        wantedToken,
                        visible
                )) {
                    return true;
                }
            }
        }

        return false;
    }

    private void setSubtreeVisibility(
            PamPlayer.AnimationPart part,
            boolean visible
    ) {
        if (part == null) {
            return;
        }

        if (part.name != null
                && !part.name.isBlank()) {
            partsVisibility.put(part.name, visible);
        }

        if (part.children != null) {
            for (PamPlayer.AnimationPart child
                    : part.children) {
                setSubtreeVisibility(child, visible);
            }
        }
    }

    /**
     * Enables a matching part, every descendant below it, and every ancestor
     * required to reach it. This is important for nested armor/newspaper/barrel
     * PAM hierarchies.
     */
    private boolean enableMatchingHierarchy(
            PamPlayer.AnimationPart part,
            String[] wantedTokens,
            boolean insideMatchedSubtree
    ) {
        if (part == null) {
            return false;
        }

        boolean thisMatches =
                matchesAny(
                        part.name,
                        wantedTokens
                );

        boolean enableWholeSubtree =
                insideMatchedSubtree || thisMatches;

        boolean descendantMatched = false;

        if (part.children != null) {
            for (PamPlayer.AnimationPart child
                    : part.children) {

                descendantMatched |=
                        enableMatchingHierarchy(
                                child,
                                wantedTokens,
                                enableWholeSubtree
                        );
            }
        }

        if ((enableWholeSubtree || descendantMatched)
                && part.name != null
                && !part.name.isBlank()) {

            partsVisibility.put(
                    part.name,
                    true
            );
        }

        return thisMatches || descendantMatched;
    }

    private boolean matchesAny(
            String rawName,
            String[] wantedTokens
    ) {
        String normalizedName =
                normalize(rawName);

        for (String token : wantedTokens) {
            if (normalizedName.contains(
                    normalize(token)
            )) {
                return true;
            }
        }

        return false;
    }

    private String chooseClip() {
        if (zombie == null || animationInfo == null) {
            return null;
        }

        if (!zombie.isDead()
                && zombie.getType() == ZombieType.PIANIST
                && hitReactionRemaining > 0f) {
            return firstAvailable("damage");
        }

        ZombieVisualState state =
                ZombieVisualStateResolver.resolve(zombie);

        String[] candidates =
                ZombieGraphicsRegistry.clipCandidates(
                        zombie,
                        state
                );

        return firstAvailable(candidates);
    }

    private String firstAvailable(
            String... wanted
    ) {
        for (String candidate : wanted) {
            for (String clip : animationInfo.getClips()) {
                if (candidate.equalsIgnoreCase(clip)) {
                    return clip;
                }
            }
        }

        return animationInfo.getClips().isEmpty()
                ? null
                : animationInfo.getClips().get(0);
    }

    private static String normalize(String value) {
        if (value == null) {
            return "";
        }

        return value
                .toUpperCase()
                .replace("_", "")
                .replace("-", "")
                .replace(" ", "");
    }

    private static String normalizeSymbol(String value) {
        if (value == null) {
            return "";
        }

        int imageSeparator = value.indexOf('|');
        String symbol = imageSeparator < 0
                ? value
                : value.substring(0, imageSeparator);

        return normalize(symbol);
    }

    public void setAnimation(
            String pamPath,
            String clip
    ) {
        previewMode = true;
        previewPamPath = pamPath;
        previewClip = clip;
        currentClip = clip;
        stateTime = 0f;
    }

    public void setClip(String clip) {
        previewMode = true;
        previewClip = clip;
        currentClip = clip;
        stateTime = 0f;
    }

    public void restart() {
        stateTime = 0f;
    }

    public void setPlaying(boolean playing) {
        this.playing = playing;
    }

    public boolean isPlaying() {
        return playing;
    }

    public void setScale(float scale) {
        this.scale = scale;
    }

    public float getScale() {
        return scale;
    }

    public void setOffsets(
            float xOffset,
            float yOffset
    ) {
        this.xOffset = xOffset;
        this.yOffset = yOffset;
    }

    public Zombie getZombie() {
        return zombie;
    }

    public long getZombieId() {
        return zombie == null
                ? -1L
                : zombie.getId();
    }

    public String getPamPath() {
        if (previewMode) {
            return previewPamPath;
        }

        return animationInfo == null
                ? null
                : animationInfo.getPath();
    }

    public String getClip() {
        return currentClip;
    }

    public float getStateTime() {
        return stateTime;
    }

    public boolean isZombieDead() {
        return zombie != null && zombie.isDead();
    }

    public void beginDeathAnimation() {
        if (deathAnimationStarted || !isZombieDead()) {
            return;
        }

        deathAnimationStarted = true;
        hitReactionRemaining = 0f;
        visibilityBuilt = false;
        currentClip = chooseClip();
        stateTime = 0f;

        deathAnimationDuration =
                currentClip == null
                        ? 0f
                        : pamPlayer.clipDurationSeconds(
                                animationInfo.getPath(),
                                currentClip
                        );
    }

    public boolean isDeathAnimationComplete() {
        return deathAnimationStarted
                && (currentClip == null
                || stateTime >= deathAnimationDuration);
    }

    public boolean isPreviewMode() {
        return previewMode;
    }
}