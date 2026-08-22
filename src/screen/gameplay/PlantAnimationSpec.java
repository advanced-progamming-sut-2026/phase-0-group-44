package screen.gameplay;

import model.enums.PlantType;

import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;

public final class PlantAnimationSpec {
    private final String folder;
    private final PlantAnimationTier tier;
    private final Map<PlantAnimationState, String> clipOverrides;
    private final int idleStageCount;
    private final int damageStageCount;

    private PlantAnimationSpec(String folder, PlantAnimationTier tier,
                               Map<PlantAnimationState, String> clipOverrides,
                               int idleStageCount, int damageStageCount) {
        this.folder = folder;
        this.tier = tier;
        this.clipOverrides = clipOverrides;
        this.idleStageCount = idleStageCount;
        this.damageStageCount = damageStageCount;
    }

    public String folder() {
        return folder;
    }

    public PlantAnimationTier tier() {
        return tier;
    }

    public int idleStageCount() {
        return idleStageCount;
    }

    public int damageStageCount() {
        return damageStageCount;
    }

    public static Builder builder(PlantAnimationTier tier) {
        return new Builder(tier);
    }

    public static final class Builder {
        private final PlantAnimationTier tier;
        private String folder;
        private final Map<PlantAnimationState, String> clips = new EnumMap<>(PlantAnimationState.class);
        private int idleStageCount = 1;
        private int damageStageCount = 0;

        private Builder(PlantAnimationTier tier) {
            this.tier = tier;
        }

        public Builder idleStages(int count) {
            this.idleStageCount = count;
            return this;
        }

        /** Number of progressive "cracking" clips (DAMAGE, DAMAGE2, DAMAGE3...) this plant has. */
        public Builder damageStages(int count) {
            this.damageStageCount = count;
            return this;
        }

        public Builder folder(String folder) {
            this.folder = folder;
            return this;
        }

        public Builder clip(PlantAnimationState state, String clipName) {
            clips.put(state, clipName);
            return this;
        }

        public PlantAnimationSpec build(PlantType type) {
            String resolved = folder != null ? folder : type.name().replace("_", "");
            return new PlantAnimationSpec(resolved, tier, clips, idleStageCount, damageStageCount);
        }
    }

    public String clipName(PlantAnimationState state) {
        String override = clipOverrides.get(state);
        return override != null ? override : defaultClipName(state);
    }

    /**
     * Derives "Plantfood_on", "Idle2", "Attack_start_damage" style names
     * straight from the enum constant. Matches every plain-cased clip in
     * the reference data — only genuine typos/oddities need an override.
     */
    static String defaultClipName(PlantAnimationState state) {
        return state.name().toLowerCase(Locale.ROOT);
    }

}