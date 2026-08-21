package screen.gameplay;

import model.enums.PlantType;

import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;

public final class PlantAnimationSpec {
    private final String folder;
    private final PlantAnimationTier tier;
    private final Map<PlantAnimationState, String> clipOverrides;

    private PlantAnimationSpec(String folder, PlantAnimationTier tier,
                               Map<PlantAnimationState, String> clipOverrides) {
        this.folder = folder;
        this.tier = tier;
        this.clipOverrides = clipOverrides;
    }

    public String folder() {
        return folder;
    }

    public PlantAnimationTier tier() {
        return tier;
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
        String lower = state.name().toLowerCase(Locale.ROOT);
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }

    public static Builder builder(PlantAnimationTier tier) {
        return new Builder(tier);
    }

    public static final class Builder {
        private final PlantAnimationTier tier;
        private String folder;
        private final Map<PlantAnimationState, String> clips = new EnumMap<>(PlantAnimationState.class);

        private Builder(PlantAnimationTier tier) {
            this.tier = tier;
        }

        /** Only needed when the asset folder doesn't match the normalized enum name. */
        public Builder folder(String folder) {
            this.folder = folder;
            return this;
        }

        /** Only needed when the clip name doesn't match {@link #defaultClipName}. */
        public Builder clip(PlantAnimationState state, String clipName) {
            clips.put(state, clipName);
            return this;
        }

        public PlantAnimationSpec build(PlantType type) {
            String resolved = folder != null ? folder : type.name().replace("_", "");
            return new PlantAnimationSpec(resolved, tier, clips);
        }
    }
}