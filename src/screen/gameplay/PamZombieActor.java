package screen.gameplay;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.scenes.scene2d.Actor;
import model.inGame.zombie.Zombie;
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

    public PamZombieActor(
            PamPlayer pamPlayer,
            Zombie zombie,
            ZombieAnimationInfo animationInfo
    ) {
        this.pamPlayer = pamPlayer;
        this.zombie = zombie;
        this.animationInfo = animationInfo;
        this.currentClip = chooseClip();
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

        if (wantedTokens.length == 0) {
            return;
        }

        PamPlayer.AnimationPart root =
                pamPlayer.getParts(
                        animationInfo.getPath()
                );

        if (root == null) {
            return;
        }

        enableMatchingHierarchy(
                root,
                wantedTokens,
                false
        );

        System.out.println(
                "[ZombieGraphics] "
                        + zombie.getType()
                        + " VISIBLE PARTS = "
                        + partsVisibility.keySet()
        );
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
