package model.miniGame;

import model.Result;
import model.enums.PlantType;
import model.enums.ZombieType;
import model.sim.GameOutcome;
import model.sim.zombie.ZombieCombatSystem;
import model.sim.zombie.ZombieSpecialSystem;
import util.RandomSource;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Three-level Beghouled strategy with match-board state isolated from shared combat. */
public final class Beghouled extends MiniGame {
    private static final Pattern SWAP = Pattern.compile(
            "^swap\\s+plants\\s+-l\\s+\\(\\s*(-?\\d+)\\s*,\\s*(-?\\d+)\\s*\\)"
                    + "\\s+-l\\s+\\(\\s*(-?\\d+)\\s*,\\s*(-?\\d+)\\s*\\)$",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern UPGRADE = Pattern.compile(
            "^upgrade\\s+plant\\s+-t\\s+(.+)$", Pattern.CASE_INSENSITIVE);
    private static final Pattern STATUS = Pattern.compile(
            "^(?:show\\s+beghouled|beghouled\\s+status|show\\s+match\\s+board)$",
            Pattern.CASE_INSENSITIVE);

    private final RandomSource random;

    public Beghouled(RandomSource random) {
        if (random == null) {
            throw new IllegalArgumentException("Beghouled randomness is required.");
        }
        this.random = random;
    }

    @Override
    public MiniGameId getId() {
        return MiniGameId.BEGHOULED;
    }

    public static BeghouledLevelRules rulesFor(int level) {
        List<PlantType> pool = List.of(
                PlantType.PEASHOOTER,
                PlantType.WALL_NUT,
                PlantType.PUFF_SHROOM,
                PlantType.CABBAGE_PULT,
                PlantType.SNOW_PEA);
        return switch (level) {
            case 1 -> new BeghouledLevelRules(
                    1, 12, 120, 12, 2400, pool,
                    List.of(ZombieType.NORMAL, ZombieType.CONEHEAD));
            case 2 -> new BeghouledLevelRules(
                    2, 18, 90, 24, 2100, pool,
                    List.of(ZombieType.NORMAL, ZombieType.CONEHEAD,
                            ZombieType.BUCKETHEAD, ZombieType.NEWSPAPER_ZOMBIE));
            case 3 -> new BeghouledLevelRules(
                    3, 24, 60, 40, 1800, pool,
                    List.of(ZombieType.CONEHEAD, ZombieType.BUCKETHEAD,
                            ZombieType.KNIGHT, ZombieType.ALL_STAR,
                            ZombieType.GARGANTUAR));
            default -> throw new IllegalArgumentException("Beghouled level must be 1 through 3.");
        };
    }

    public static Map<PlantType, BeghouledUpgrade> upgrades() {
        Map<PlantType, BeghouledUpgrade> result = new LinkedHashMap<>();
        add(result, PlantType.PEASHOOTER, PlantType.REPEATER, 500);
        add(result, PlantType.REPEATER, PlantType.MEGA_GATLING_PEA, 1500);
        add(result, PlantType.WALL_NUT, PlantType.TALL_NUT, 500);
        add(result, PlantType.PUFF_SHROOM, PlantType.FUME_SHROOM, 250);
        add(result, PlantType.CABBAGE_PULT, PlantType.MELON_PULT, 1000);
        add(result, PlantType.MELON_PULT, PlantType.WINTER_MELON, 750);
        return Map.copyOf(result);
    }

    private static void add(
            Map<PlantType, BeghouledUpgrade> target,
            PlantType from,
            PlantType to,
            int cost
    ) {
        target.put(from, new BeghouledUpgrade(from, to, cost));
    }

    @Override
    public void configure(MiniGameSession session) {
        super.configure(session);
        BeghouledLevelRules rules = rulesFor(session.getConfig().getLevelNumber());
        if (session.getConfig().isSkySunEnabled()
                || session.getConfig().getStartingResources() != 0
                || session.getConfig().getObjectiveTarget() != rules.getTargetMatches()
                || session.getConfig().getThreatBudget() != rules.getDangerScore()
                || session.getConfig().getTimeLimitTicks() != rules.getTimeLimitTicks()) {
            throw new IllegalStateException("Beghouled catalog and strategy configuration differ.");
        }
        BeghouledState state = new BeghouledState(
                rules, random, session.getSimulation().getWorld(), upgrades());
        session.setStrategyState(state);
        session.getSimulation().register(state.preCombatSystem());
        session.getSimulation().register(new ZombieSpecialSystem());
        session.getSimulation().register(new ZombieCombatSystem());
        session.getSimulation().register(state.postCombatSystem());
    }

    @Override
    public GameOutcome evaluate(MiniGameSession session) {
        BeghouledState state = session.getStrategyState(BeghouledState.class);
        return state == null
                ? session.getSimulation().getWorld().getOutcome()
                : state.evaluate(session.getSimulation().getWorld());
    }

    @Override
    public List<MiniGameCommandExtension> commandExtensions() {
        return List.of(new Commands());
    }

    private static final class Commands implements MiniGameCommandExtension {
        @Override
        public boolean supports(String input) {
            if (input == null) return false;
            String value = input.trim();
            return SWAP.matcher(value).matches()
                    || UPGRADE.matcher(value).matches()
                    || STATUS.matcher(value).matches();
        }

        @Override
        public Result<String> execute(MiniGameSession session, String input) {
            BeghouledState state = session.getStrategyState(BeghouledState.class);
            if (state == null) {
                Result<String> missing = new Result<>();
                missing.appendToMessage("Beghouled state is not initialized");
                return missing;
            }
            Matcher matcher = SWAP.matcher(input.trim());
            if (matcher.matches()) {
                return state.swap(
                        session,
                        Integer.parseInt(matcher.group(1)),
                        Integer.parseInt(matcher.group(2)),
                        Integer.parseInt(matcher.group(3)),
                        Integer.parseInt(matcher.group(4)));
            }
            matcher = UPGRADE.matcher(input.trim());
            if (matcher.matches()) {
                return state.upgrade(session, matcher.group(1));
            }
            Result<String> result = new Result<>();
            String status = state.status(session);
            result.setStatus(true);
            result.setData(status);
            result.appendToMessage(status);
            return result;
        }
    }
}
