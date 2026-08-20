package screen.gameplay;

import model.enums.ZombieType;
import model.inGame.zombie.Zombie;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Single source of truth for zombie presentation.
 *
 * Gameplay code must not live here. This class only answers:
 *  - which PAM should render a model Zombie,
 *  - which optional PAM parts should be visible,
 *  - which clip best represents the model's current runtime state.
 */
public final class ZombieGraphicsRegistry {

    private ZombieGraphicsRegistry() {
    }

    private record Profile(
            String[] exactPamNames,
            String[][] fallbackTokenGroups,
            String[] forbiddenPamTokens,
            String[] visiblePartTokens
    ) {
    }

    private static final Map<ZombieType, Profile> PROFILES =
            new EnumMap<>(ZombieType.class);

    static {
        Profile egyptBasic = profile(
                names("ZOMBIE_EGYPT_BASIC"),
                groups(group("EGYPT", "BASIC")),
                none(),
                none()
        );

        register(ZombieType.NORMAL, egyptBasic);

        register(
                ZombieType.CONEHEAD,
                profile(
                        egyptBasic.exactPamNames(),
                        egyptBasic.fallbackTokenGroups(),
                        none(),
                        tokens("ARMOR1", "CONE")
                )
        );

        register(
                ZombieType.BUCKETHEAD,
                profile(
                        egyptBasic.exactPamNames(),
                        egyptBasic.fallbackTokenGroups(),
                        none(),
                        tokens("ARMOR2", "BUCKET")
                )
        );

        register(
                ZombieType.KNIGHT,
                profile(
                        names("ZOMBIE_DARK_BASIC", "ZOMBIE_DARK"),
                        groups(group("DARK", "BASIC"), group("DARK")),
                        tokens(
                                "JESTER", "JUGGLER", "WIZARD", "KING",
                                "GARGANTUAR", "IMP", "FLAG", "BOSS"
                        ),
                        tokens("ARMOR3", "KNIGHT")
                )
        );

        register(
                ZombieType.BLOCKHEAD,
                profile(
                        names("ZOMBIE_ICEAGE_BASIC", "ZOMBIE_ICEAGE"),
                        groups(group("ICEAGE", "BASIC"), group("ICEAGE")),
                        tokens(
                                "GARGANTUAR", "IMP", "HUNTER",
                                "DODO", "TROGLOBITE", "WEASEL", "BOSS"
                        ),
                        tokens(
                                "ARMOR3", "BLOCKHEAD",
                                "ICEBLOCK", "ICEBLOCKHEAD"
                        )
                )
        );

        register(
                ZombieType.GARGANTUAR,
                profile(
                        names(
                                "EGYPT_GARGANTUAR",
                                "ZOMBIE_EGYPT_GARGANTUAR"
                        ),
                        groups(
                                group("EGYPT", "GARGANTUAR"),
                                group("GARGANTUAR")
                        ),
                        tokens("IMP"),
                        none()
                )
        );

        register(
                ZombieType.IMP,
                profile(
                        names("ZOMBIE_EGYPT_IMP"),
                        groups(group("EGYPT", "IMP")),
                        tokens(
                                "GARGANTUAR", "DRAGON",
                                "LOSTCITY", "ICEAGE"
                        ),
                        none()
                )
        );

        register(
                ZombieType.ALL_STAR,
                profile(
                        names(
                                "ZOMBIE_MODERN_ALLSTAR",
                                "ZOMBIE_MODERN_ALL_STAR"
                        ),
                        groups(
                                group("MODERN", "ALLSTAR"),
                                group("ALLSTAR")
                        ),
                        tokens("BIGHEAD", "HERO"),
                        none()
                )
        );

        register(
                ZombieType.FOOTBALL,
                PROFILES.get(ZombieType.ALL_STAR)
        );

        register(
                ZombieType.ARCADE_ZOMBIE,
                profile(
                        names(
                                "ZOMBIE_80S_ARCADE",
                                "ZOMBIE_EIGHTIES_ARCADE"
                        ),
                        groups(
                                group("EIGHTIES", "ARCADE"),
                                group("80S", "ARCADE")
                        ),
                        tokens("CABINET"),
                        none()
                )
        );

        register(
                ZombieType.PARASOL_ZOMBIE,
                profile(
                        names(
                                "ZOMBIE_LOSTCITY_JANE",
                                "ZOMBIE_LOSTCITY_PARASOL"
                        ),
                        groups(
                                group("LOSTCITY", "JANE"),
                                group("LOSTCITY", "PARASOL")
                        ),
                        none(),
                        none()
                )
        );

        register(
                ZombieType.TURQUOISE_ZOMBIE,
                profile(
                        names(
                                "ZOMBIE_LOSTCITY_CRYSTALSKULL",
                                "ZOMBIE_LOSTCITY_TURQUOISE"
                        ),
                        groups(
                                group("LOSTCITY", "CRYSTALSKULL"),
                                group("LOSTCITY", "TURQUOISE")
                        ),
                        none(),
                        none()
                )
        );

        register(
                ZombieType.PROSPECTOR,
                profile(
                        names(
                                "ZOMBIE_WEST_PROSPECTOR",
                                "ZOMBIE_PROSPECTOR"
                        ),
                        groups(group("PROSPECTOR")),
                        tokens("SPRING", "UNCHARTED"),
                        none()
                )
        );

        register(
                ZombieType.PIANIST,
                profile(
                        names(
                                "PIANO",
                                "ZOMBIE_WEST_PIANO",
                                "ZOMBIE_COWBOY_PIANO"
                        ),
                        groups(group("PIANO")),
                        tokens("ALMANAC", "FEASTIVUS", "HOLIDAY"),
                        none()
                )
        );

        Profile newspaper = profile(
                names(
                        "ZOMBIE_MODERN_NEWSPAPER",
                        "MODERN_NEWSPAPER",
                        "ZOMBIE_NEWSPAPER"
                ),
                groups(
                        group("MODERN", "NEWSPAPER"),
                        group("NEWSPAPER")
                ),
                tokens(
                        "BIGHEAD", "VETERAN",
                        "SUNDAY", "ALMANAC"
                ),
                tokens("NEWSPAPER", "PAPER")
        );

        register(ZombieType.NEWSPAPER_ZOMBIE, newspaper);
        register(ZombieType.NEWSPAPER, newspaper);

        register(
                ZombieType.BARREL_ROLLER,
                profile(
                        names(
                                "ZOMBIE_PIRATE_BARRELROLLER",
                                "ZOMBIE_PIRATE_BARREL_ROLLER",
                                "ZOMBIE_PIRATE_BARREL",
                                "BARRELROLLER",
                                "ZOMBIE_BARRELROLLER"
                        ),
                        groups(
                                group("PIRATE", "BARRELROLLER"),
                                group("BARRELROLLER"),
                                group("PIRATE", "BARREL")
                        ),
                        tokens(
                                "ALMANAC", "BIRTHDAY",
                                "CANNON", "CAPTAIN"
                        ),
                        tokens("BARREL", "ROLLER", "DRUM")
                )
        );

        register(
                ZombieType.RA_ZOMBIE,
                profile(
                        names("ZOMBIE_EGYPT_RA"),
                        groups(group("EGYPT", "RA")),
                        tokens("GARGANTUAR", "IMP", "ALMANAC"),
                        none()
                )
        );

        register(
                ZombieType.EXPLORER,
                profile(
                        names(
                                "ZOMBIE_EXPLORER",
                                "ZOMBIE_EGYPT_EXPLORER"
                        ),
                        groups(group("EXPLORER")),
                        tokens("WOLF", "VETERAN", "ALMANAC"),
                        none()
                )
        );

        register(
                ZombieType.TOMBRAISER,
                profile(
                        names(
                                "ZOMBIE_EGYPT_TOMBRAISER",
                                "ZOMBIE_TOMBRAISER"
                        ),
                        groups(
                                group("EGYPT", "TOMBRAISER"),
                                group("TOMBRAISER")
                        ),
                        tokens("FEST", "VETERAN", "ALMANAC"),
                        none()
                )
        );

        /*
         * FROSTBITE CAVES
         */

        register(
                ZombieType.DODO_RIDER,
                profile(
                        names(
                                "ZOMBIE_ICEAGE_DODO",
                                "ZOMBIE_ICEAGE_DODORIDER",
                                "ZOMBIE_ICEAGE_DODO_RIDER"
                        ),
                        groups(
                                group("ICEAGE", "DODO"),
                                group("DODO")
                        ),
                        tokens(
                                "ALMANAC",
                                "BOSS",
                                "GARGANTUAR"
                        ),
                        none()
                )
        );

        register(
                ZombieType.HUNTER,
                profile(
                        names(
                                "ZOMBIE_ICEAGE_HUNTER",
                                "ZOMBIE_HUNTER"
                        ),
                        groups(
                                group("ICEAGE", "HUNTER"),
                                group("HUNTER")
                        ),
                        tokens(
                                "ALMANAC",
                                "WOLF",
                                "VETERAN"
                        ),
                        none()
                )
        );

        register(
                ZombieType.TROGLOBITE,
                profile(
                        names(
                                "ZOMBIE_ICEAGE_TROGLOBITE",
                                "ZOMBIE_TROGLOBITE"
                        ),
                        groups(
                                group("ICEAGE", "TROGLOBITE"),
                                group("TROGLOBITE")
                        ),
                        tokens(
                                "ALMANAC",
                                "GARGANTUAR"
                        ),
                        none()
                )
        );

        /*
         * BIG WAVE BEACH
         */

        register(
                ZombieType.FISHERMAN,
                profile(
                        names(
                                "ZOMBIE_BEACH_FISHERMAN"
                        ),
                        groups(
                                group("BEACH", "FISHERMAN"),
                                group("FISHERMAN")
                        ),
                        tokens(
                                "ALMANAC",
                                "VETERAN"
                        ),
                        none()
                )
        );

        register(
                ZombieType.SNORKEL,
                profile(
                        names(
                                "ZOMBIE_BEACH_SNORKELER",
                                "ZOMBIE_BEACH_SNORKEL"
                        ),
                        groups(
                                group("BEACH", "SNORKEL"),
                                group("SNORKEL")
                        ),
                        tokens(
                                "ALMANAC",
                                "VETERAN"
                        ),
                        none()
                )
        );

        register(
                ZombieType.OCTOPUS_ZOMBIE,
                profile(
                        names(
                                "ZOMBIE_BEACH_OCTOPUS"
                        ),
                        groups(
                                group("BEACH", "OCTOPUS"),
                                group("OCTOPUS")
                        ),
                        tokens(
                                "ALMANAC",
                                "VETERAN"
                        ),
                        none()
                )
        );

        /*
         * DARK AGES
         */

        register(
                ZombieType.JESTER,
                profile(
                        names(
                                "RK_JESTER",
                                "ZOMBIE_DARK_JESTER",
                                "ZOMBIE_JESTER"
                        ),
                        groups(
                                group("JESTER"),
                                group("DARK", "JESTER")
                        ),
                        tokens(
                                "ALMANAC",
                                "VETERAN"
                        ),
                        none()
                )
        );

        register(
                ZombieType.WIZARD,
                profile(
                        names(
                                "RK_WIZARD",
                                "ZOMBIE_DARK_WIZARD",
                                "ZOMBIE_WIZARD"
                        ),
                        groups(
                                group("DARK", "WIZARD"),
                                group("WIZARD")
                        ),
                        tokens(
                                "ALMANAC",
                                "VETERAN",
                                "BOSS"
                        ),
                        none()
                )
        );

        register(
                ZombieType.KING,
                profile(
                        names(
                                "RK_KING",
                                "ZOMBIE_DARK_KING",
                                "ZOMBIE_KING"
                        ),
                        groups(
                                group("DARK", "KING"),
                                group("KING")
                        ),
                        tokens(
                                "ALMANAC",
                                "BOSS"
                        ),
                        none()
                )
        );

        register(
                ZombieType.DRAGON_IMP,
                profile(
                        names(
                                "ZOMBIE_DARK_DRAGON_IMP",
                                "ZOMBIE_DARK_DRAGONIMP",
                                "DRAGON_IMP"
                        ),
                        groups(
                                group("DARK", "DRAGON", "IMP"),
                                group("DRAGON", "IMP")
                        ),
                        tokens(
                                "GARGANTUAR",
                                "ALMANAC",
                                "BOSS"
                        ),
                        none()
                )
        );
    }

    public static ZombieAnimationInfo resolveAnimation(
            Zombie zombie,
            List<ZombieAnimationInfo> animations
    ) {
        if (zombie == null || animations == null || animations.isEmpty()) {
            return null;
        }

        Profile profile = PROFILES.get(zombie.getType());
        if (profile == null) {
            return null;
        }

        ZombieAnimationInfo exact =
                findExact(animations, profile.exactPamNames());

        if (exact != null) {
            return exact;
        }

        for (String[] required : profile.fallbackTokenGroups()) {
            ZombieAnimationInfo candidate =
                    findBestByTokens(
                            animations,
                            required,
                            profile.forbiddenPamTokens()
                    );

            if (candidate != null) {
                return candidate;
            }
        }

        return null;
    }

    public static String[] visiblePartTokens(ZombieType type) {
        Profile profile = PROFILES.get(type);
        return profile == null
                ? none()
                : profile.visiblePartTokens();
    }

    /**
     * Returns clip names in priority order.
     * The actor chooses the first clip that actually exists in the PAM.
     */
    public static String[] clipCandidates(
            Zombie zombie,
            ZombieVisualState state
    ) {
        if (zombie == null || state == null) {
            return none();
        }

        ZombieType type = zombie.getType();

        if (type == ZombieType.GARGANTUAR
                && state == ZombieVisualState.EAT) {
            return clips("smash", "attack", "eat", "walk");
        }

        if ((type == ZombieType.ALL_STAR
                || type == ZombieType.FOOTBALL)
                && bool(zombie, "CHARGING")) {
            return clips("tackle", "charge", "run", "walk");
        }

        if (type == ZombieType.ARCADE_ZOMBIE
                && bool(zombie, "PUSHING")) {
            return clips("push", "pushing", "walk");
        }

        if (type == ZombieType.TURQUOISE_ZOMBIE) {
            if (any(
                    zombie,
                    "FIRING",
                    "LASER"
            )) {
                return clips(
                        "fire", "laser", "attack",
                        "power", "idle"
                );
            }

            if (any(
                    zombie,
                    "STEALING_SUN",
                    "STEALING"
            )) {
                return clips(
                        "steal", "power",
                        "charge", "idle"
                );
            }
        }

        if (type == ZombieType.PROSPECTOR
                && any(
                        zombie,
                        "EXPLODING",
                        "FLIPPING",
                        "LAUNCHED"
                )) {
            return clips(
                    "explode", "launch",
                    "flip", "jump", "walk"
            );
        }

        if (type == ZombieType.PIANIST
                && state != ZombieVisualState.DEAD) {
            return clips("play", "playing", "idle");
        }

        if ((type == ZombieType.NEWSPAPER_ZOMBIE
                || type == ZombieType.NEWSPAPER)
                && any(
                        zombie,
                        "ENRAGED",
                        "NEWSPAPER_BROKEN",
                        "PAPER_BROKEN"
                )) {

            if (state == ZombieVisualState.EAT) {
                return clips(
                        "eat_angry",
                        "angry_eat",
                        "eat_fast",
                        "eat",
                        "walk"
                );
            }

            return clips(
                    "walk_angry",
                    "angry_walk",
                    "run",
                    "walk"
            );
        }

        if (type == ZombieType.BARREL_ROLLER
                && !any(
                        zombie,
                        "BARREL_BROKEN",
                        "ROLLER_BROKEN"
                )) {
            return clips(
                    "push",
                    "pushing",
                    "roll",
                    "rolling",
                    "push_walk",
                    "walk",
                    "idle"
            );
        }

        if (type == ZombieType.RA_ZOMBIE
                && any(
                        zombie,
                        "STEALING_SUN",
                        "STEALING",
                        "POWERING"
                )) {
            return clips(
                    "power_up",
                    "power",
                    "steal",
                    "idle"
            );
        }

        if (type == ZombieType.EXPLORER
                && any(
                        zombie,
                        "TORCH_ATTACK",
                        "BURNING_PLANT"
                )) {
            return clips("attack", "eat", "walk");
        }

        if (type == ZombieType.TOMBRAISER
                && any(
                        zombie,
                        "RAISING_TOMB",
                        "RAISING_GRAVE",
                        "SUMMONING",
                        "CASTING"
                )) {
            return clips(
                    "raise",
                    "tomb",
                    "cast",
                    "special",
                    "attack",
                    "idle"
            );
        }

        /*
         * Next ten. These are visual preferences only; no gameplay is changed.
         */

        if (type == ZombieType.DODO_RIDER
                && any(
                        zombie,
                        "FLYING",
                        "JUMPING",
                        "VAULTING"
                )) {
            return clips(
                    "fly",
                    "flying",
                    "jump",
                    "vault",
                    "walk"
            );
        }

        if (type == ZombieType.HUNTER
                && any(
                        zombie,
                        "THROWING",
                        "SHOOTING",
                        "ATTACKING"
                )) {
            return clips(
                    "throw",
                    "shoot",
                    "attack",
                    "idle",
                    "walk"
            );
        }

        if (type == ZombieType.TROGLOBITE
                && any(
                        zombie,
                        "PUSHING",
                        "PUSHING_ICE"
                )) {
            return clips(
                    "push",
                    "pushing",
                    "walk"
            );
        }

        if (type == ZombieType.FISHERMAN) {
            if (any(
                    zombie,
                    "CASTING",
                    "FISHING",
                    "HOOKING",
                    "REELING"
            )) {
                return clips(
                        "cast",
                        "casting",
                        "reel",
                        "pull",
                        "attack",
                        "idle"
                );
            }

            if (state != ZombieVisualState.DEAD) {
                return clips(
                        "idle",
                        "intro",
                        "walk"
                );
            }
        }

        if (type == ZombieType.SNORKEL) {
            if (any(
                    zombie,
                    "UNDERWATER",
                    "SUBMERGED"
            )) {
                return clips(
                        "swim",
                        "underwater",
                        "submerge",
                        "walk",
                        "idle"
                );
            }

            if (any(
                    zombie,
                    "SURFACING",
                    "EMERGING"
            )) {
                return clips(
                        "surface",
                        "emerge",
                        "rise",
                        "walk"
                );
            }
        }

        if (type == ZombieType.OCTOPUS_ZOMBIE
                && any(
                        zombie,
                        "THROWING",
                        "TOSSING",
                        "OCTOPUS_ATTACK"
                )) {
            return clips(
                    "toss",
                    "throw",
                    "attack",
                    "idle",
                    "walk"
            );
        }

        if (type == ZombieType.JESTER
                && state == ZombieVisualState.SPINNING) {
            return clips(
                    "spin_walk",
                    "spin",
                    "spinup",
                    "spindown",
                    "walk"
            );
        }

        if (type == ZombieType.WIZARD
                && any(
                        zombie,
                        "CASTING",
                        "TRANSFORMING",
                        "HEXING"
                )) {
            return clips(
                    "cast",
                    "spell",
                    "attack",
                    "idle",
                    "walk"
            );
        }

        if (type == ZombieType.KING) {
            if (any(
                    zombie,
                    "KNIGHTING",
                    "PROMOTING",
                    "CASTING"
            )) {
                return clips(
                        "king",
                        "promote",
                        "cast",
                        "attack",
                        "idle"
                );
            }

            if (state != ZombieVisualState.DEAD) {
                return clips(
                        "idle",
                        "king",
                        "walk"
                );
            }
        }

        return switch (state) {
            case DEAD ->
                    clips("die", "death", "idle");

            case EAT ->
                    clips("eat", "eating", "attack", "walk");

            case CHARGING ->
                    clips("charge", "run", "walk");

            case SPINNING ->
                    clips("spin", "spinning", "walk");

            case FROZEN, STUNNED ->
                    clips("idle", "walk");

            case IDLE ->
                    clips("idle", "walk");

            case WALK ->
                    clips("walk", "idle");
        };
    }

    private static ZombieAnimationInfo findExact(
            List<ZombieAnimationInfo> animations,
            String[] names
    ) {
        for (String wanted : names) {
            for (ZombieAnimationInfo info : animations) {
                if (wanted.equalsIgnoreCase(info.getName())) {
                    return info;
                }
            }
        }

        return null;
    }

    private static ZombieAnimationInfo findBestByTokens(
            List<ZombieAnimationInfo> animations,
            String[] required,
            String[] forbidden
    ) {
        ZombieAnimationInfo best = null;
        int bestScore = Integer.MIN_VALUE;

        for (ZombieAnimationInfo info : animations) {
            String identity =
                    normalize(
                            info.getName()
                                    + " "
                                    + info.getPath()
                    );

            if (!containsAll(identity, required)) {
                continue;
            }

            if (containsAny(identity, forbidden)) {
                continue;
            }

            int score = animationScore(info);

            if (score > bestScore) {
                bestScore = score;
                best = info;
            }
        }

        return best;
    }

    private static int animationScore(ZombieAnimationInfo info) {
        int score = 0;

        if (hasClip(info, "walk")) score += 50;
        if (hasClip(info, "eat")) score += 30;
        if (hasClip(info, "idle")) score += 20;
        if (hasClip(info, "die")) score += 10;

        return score;
    }

    private static boolean hasClip(
            ZombieAnimationInfo info,
            String wanted
    ) {
        for (String clip : info.getClips()) {
            if (wanted.equalsIgnoreCase(clip)) {
                return true;
            }
        }

        return false;
    }

    private static boolean containsAll(
            String identity,
            String[] tokens
    ) {
        for (String token : tokens) {
            if (!identity.contains(normalize(token))) {
                return false;
            }
        }

        return true;
    }

    private static boolean containsAny(
            String identity,
            String[] tokens
    ) {
        for (String token : tokens) {
            if (identity.contains(normalize(token))) {
                return true;
            }
        }

        return false;
    }

    private static boolean any(
            Zombie zombie,
            String... flags
    ) {
        for (String flag : flags) {
            if (bool(zombie, flag)) {
                return true;
            }
        }

        return false;
    }

    private static boolean bool(
            Zombie zombie,
            String flag
    ) {
        return zombie.getBooleanState(flag);
    }

    private static void register(
            ZombieType type,
            Profile profile
    ) {
        PROFILES.put(type, profile);
    }

    private static Profile profile(
            String[] exactPamNames,
            String[][] fallbackTokenGroups,
            String[] forbiddenPamTokens,
            String[] visiblePartTokens
    ) {
        return new Profile(
                exactPamNames,
                fallbackTokenGroups,
                forbiddenPamTokens,
                visiblePartTokens
        );
    }

    private static String[] names(String... values) {
        return values;
    }

    private static String[] tokens(String... values) {
        return values;
    }

    private static String[] clips(String... values) {
        return values;
    }

    private static String[] group(String... values) {
        return values;
    }

    private static String[][] groups(String[]... values) {
        return values;
    }

    private static String[] none() {
        return new String[0];
    }

    private static String normalize(String value) {
        if (value == null) {
            return "";
        }

        return value
                .toUpperCase()
                .replace("_", "")
                .replace("-", "")
                .replace(" ", "")
                .replace("/", "")
                .replace("\\", "");
    }
}
