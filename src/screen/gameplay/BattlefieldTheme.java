package screen.gameplay;

import model.config.GameWorld;

/**
 * Visual calibration for the four Phase-2 Adventure battlefields.
 *
 * <p>The background PNGs are clean 16:9 crops taken from the supplied official
 * PVZ2 atlas sheets. Board coordinates use the crop's top-left pixel space;
 * {@link BattlefieldLayout} converts them into the 1280x720 Scene2D space.</p>
 */
public enum BattlefieldTheme {
    ANCIENT_EGYPT(
            GameWorld.ANCIENT_EGYPT,
            "ui/gameplay/backgrounds/egypt.png",
            1024f, 576f,
            251f, 80f, 990f, 569f,
            "768/INITIAL/MOWERS/MOWER_EGYPT/MOWER_EGYPT.PAM",
            0.58f
    ),
    FROSTBITE_CAVES(
            GameWorld.FROSTBITE_CAVES,
            "ui/gameplay/backgrounds/frostbite.png",
            1024f, 576f,
            258f, 87f, 987f, 560f,
            "768/FULL/MOWERS/MOWER_ICEAGE/MOWER_ICEAGE.PAM",
            0.50f
    ),
    BIG_WAVE_BEACH(
            GameWorld.BIG_WAVE_BEACH,
            "ui/gameplay/backgrounds/big_wave_beach.png",
            1365f, 768f,
            259f, 205f, 991f, 682f,
            "768/FULL/MOWERS/MOWER_BEACH/MOWER_BEACH.PAM",
            0.54f
    ),
    DARK_AGES(
            GameWorld.DARK_AGES,
            "ui/gameplay/backgrounds/dark_ages.png",
            1024f, 576f,
            255f, 83f, 991f, 565f,
            "768/FULL/MOWERS/MOWER_DARK/MOWER_DARK.PAM",
            0.54f
    );

    private final GameWorld world;
    private final String backgroundAsset;
    private final float sourceWidth;
    private final float sourceHeight;
    private final float boardLeft;
    private final float boardTop;
    private final float boardRight;
    private final float boardBottom;
    private final String mowerPam;
    private final float mowerScale;

    BattlefieldTheme(
            GameWorld world,
            String backgroundAsset,
            float sourceWidth,
            float sourceHeight,
            float boardLeft,
            float boardTop,
            float boardRight,
            float boardBottom,
            String mowerPam,
            float mowerScale
    ) {
        this.world = world;
        this.backgroundAsset = backgroundAsset;
        this.sourceWidth = sourceWidth;
        this.sourceHeight = sourceHeight;
        this.boardLeft = boardLeft;
        this.boardTop = boardTop;
        this.boardRight = boardRight;
        this.boardBottom = boardBottom;
        this.mowerPam = mowerPam;
        this.mowerScale = mowerScale;
    }

    public GameWorld world() {
        return world;
    }

    public String backgroundAsset() {
        return backgroundAsset;
    }

    public float sourceWidth() {
        return sourceWidth;
    }

    public float sourceHeight() {
        return sourceHeight;
    }

    public float boardLeft() {
        return boardLeft;
    }

    public float boardTop() {
        return boardTop;
    }

    public float boardRight() {
        return boardRight;
    }

    public float boardBottom() {
        return boardBottom;
    }

    public String mowerPam() {
        return mowerPam;
    }

    public float mowerScale() {
        return mowerScale;
    }

    public static BattlefieldTheme forWorld(GameWorld world) {
        if (world == null) {
            return ANCIENT_EGYPT;
        }
        for (BattlefieldTheme theme : values()) {
            if (theme.world == world) {
                return theme;
            }
        }
        return ANCIENT_EGYPT;
    }
}
