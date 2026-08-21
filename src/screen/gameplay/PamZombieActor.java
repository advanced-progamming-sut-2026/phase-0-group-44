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

    private boolean visibilityBuilt;
    private int damageVisibilitySignature = Integer.MIN_VALUE;
    private int hitReactionSequence;
    private int gargantuarSmashSequence;
    private float hitReactionRemaining;

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
            stateTime += delta;
        }
    }

    private void updateHitReaction(float delta) {
        int nextSequence =
                zombie.getIntState("HIT_REACTION_SEQUENCE", 0);

        if (nextSequence != hitReactionSequence) {
            hitReactionSequence = nextSequence;
            if (supportsHitReaction(zombie.getType())) {
                hitReactionRemaining = HIT_REACTION_SECONDS;
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
                || type == ZombieType.ARCADE_ZOMBIE;
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
            drawGameplay(batch);
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

    private void drawGameplay(Batch batch) {
        if (zombie == null
                || animationInfo == null
                || currentClip == null) {
            return;
        }

        float centerX =
                getX() + getWidth() * 0.5f + xOffset;

        float centerY =
                getY() + getHeight() * 0.5f + yOffset;

        if (hitReactionRemaining > 0f) {
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

        Color originalColor =
                new Color(batch.getColor());

        if (hitReactionRemaining > 0f) {
            batch.setColor(
                    1f,
                    0.55f,
                    0.55f,
                    originalColor.a
            );
        }

        pamPlayer.draw(
                batch,
                animationInfo.getPath(),
                currentClip,
                stateTime,
                centerX,
                centerY,
                true,
                partsVisibility.isEmpty()
                        ? null
                        : partsVisibility
        );

        batch.setTransformMatrix(
                originalTransform
        );
        batch.setColor(originalColor);
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
                        zombie.getType()
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

        int signature = bodyDamageStage();

        for (ZombieArmorPart part : zombie.getArmorParts()) {
            signature = 31 * signature + part.getHealth();
        }

        signature = 31 * signature
                + (zombie.getBooleanState("IMP_THROWN") ? 1 : 0);

        return signature;
    }

    private int bodyDamageStage() {
        if (zombie == null || zombie.getMaxHealth() <= 0) {
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

        double ratio = (double) health / maxHealth;
        if (ratio > 2.0 / 3.0) {
            return 0;
        }
        if (ratio > 1.0 / 3.0) {
            return 1;
        }
        return 2;
    }

    private void applyBasicZombieDamageVisibility(
            PamPlayer.AnimationPart root
    ) {
        ZombieType type = zombie.getType();

        if (type == ZombieType.CONEHEAD) {
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
        } else if (type == ZombieType.GARGANTUAR
                && zombie.getBooleanState("IMP_THROWN")) {
            setAllMatchingSubtreeVisibility(
                    root,
                    "ZOMBIE_IMP",
                    false
            );
        }

        if (type != ZombieType.NORMAL
                && type != ZombieType.CONEHEAD
                && type != ZombieType.BUCKETHEAD
                && type != ZombieType.KNIGHT
                && type != ZombieType.BLOCKHEAD
                && type != ZombieType.GARGANTUAR
                && type != ZombieType.IMP
                && type != ZombieType.ALL_STAR
                && type != ZombieType.ARCADE_ZOMBIE) {
            return;
        }

        int bodyStage = bodyDamageStage();

        if (bodyStage >= 1) {
            boolean armHidden =
                    setFirstMatchingBodyPart(
                            root,
                            "ARM1",
                            false
                    );

            if (!armHidden) {
                armHidden =
                        setFirstMatchingBodyPart(
                                root,
                                "ARM",
                                false
                        );
            }

            if (!armHidden) {
                System.out.println(
                        "[ZombieGraphics] "
                                + type
                                + " could not find a body-arm PAM part"
                );
            }
        }

        if (bodyStage >= 2) {
            boolean headHidden =
                    setFirstMatchingBodyPart(
                            root,
                            "HEAD",
                            false
                    );

            if (!headHidden) {
                headHidden =
                        setFirstMatchingBodyPart(
                                root,
                                "SKULL",
                                false
                        );
            }

            if (!headHidden) {
                System.out.println(
                        "[ZombieGraphics] "
                                + type
                                + " could not find a head PAM part"
                );
            }
        }
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
                armorDamageStage("shoulderArmor")
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

        double ratio =
                (double) part.getHealth()
                        / part.getMaxHealth();

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

    private boolean setFirstMatchingBodyPart(
            PamPlayer.AnimationPart part,
            String token,
            boolean visible
    ) {
        if (part == null) {
            return false;
        }

        String normalizedName = normalize(part.name);
        String normalizedToken = normalize(token);

        boolean carriedImpPart =
                zombie != null
                        && zombie.getType() == ZombieType.GARGANTUAR
                        && normalizedName.contains("IMP");

        if (!normalizedName.contains("ARMOR")
                && !normalizedName.contains("PARTICLE")
                && !carriedImpPart
                && normalizedName.contains(normalizedToken)) {
            setSubtreeVisibility(part, visible);
            return true;
        }

        if (part.children != null) {
            for (PamPlayer.AnimationPart child
                    : part.children) {
                if (setFirstMatchingBodyPart(
                        child,
                        token,
                        visible
                )) {
                    return true;
                }
            }
        }

        return false;
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

    public boolean isPreviewMode() {
        return previewMode;
    }
}