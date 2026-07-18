package model.miniGame;

import model.Result;
import model.enums.ZombieType;
import model.sim.GameOutcome;
import model.sim.zombie.ZombieCombatSystem;
import model.sim.zombie.ZombieSpecialSystem;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Three-level mandatory Wall-nut Bowling strategy. */
public final class BowlingWallnut extends MiniGame {
    private static final Pattern PLANT = Pattern.compile(
            "^plant\\s+conveyor\\s+-i\\s+(\\d+)\\s+-l\\s+\\(\\s*(-?\\d+)\\s*,\\s*(-?\\d+)\\s*\\)$",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern STATUS = Pattern.compile(
            "^(?:show\\s+conveyor|bowling\\s+status|wall[- ]?nut\\s+bowling\\s+status)$",
            Pattern.CASE_INSENSITIVE);

    @Override
    public MiniGameId getId() {
        return MiniGameId.BOWLING_WALLNUT;
    }

    @SuppressWarnings("PMD.ExcessiveMethodLength")
    public static WallNutBowlingLevelRules rulesFor(int level) {
        return switch (level) {
            case 1 -> new WallNutBowlingLevelRules(
                    1, 2, 5, 3, 30, 90, 12, 1600, 5.0,
                    List.of(
                            BowlingPlantType.BOWLING_WALL_NUT,
                            BowlingPlantType.EXPLODE_O_NUT,
                            BowlingPlantType.GIANT_WALL_NUT,
                            BowlingPlantType.BOWLING_WALL_NUT),
                    List.of(
                            ZombieType.NORMAL, ZombieType.NORMAL,
                            ZombieType.CONEHEAD, ZombieType.NORMAL,
                            ZombieType.NORMAL, ZombieType.CONEHEAD,
                            ZombieType.NORMAL, ZombieType.CONEHEAD));
            case 2 -> new WallNutBowlingLevelRules(
                    2, 2, 5, 2, 36, 70, 28, 1300, 5.0,
                    List.of(
                            BowlingPlantType.BOWLING_WALL_NUT,
                            BowlingPlantType.BOWLING_WALL_NUT,
                            BowlingPlantType.EXPLODE_O_NUT,
                            BowlingPlantType.BOWLING_WALL_NUT,
                            BowlingPlantType.GIANT_WALL_NUT),
                    List.of(
                            ZombieType.NORMAL, ZombieType.CONEHEAD,
                            ZombieType.NORMAL, ZombieType.BUCKETHEAD,
                            ZombieType.NORMAL, ZombieType.CONEHEAD,
                            ZombieType.BUCKETHEAD, ZombieType.NORMAL,
                            ZombieType.CONEHEAD, ZombieType.BUCKETHEAD,
                            ZombieType.NORMAL, ZombieType.CONEHEAD));
            case 3 -> new WallNutBowlingLevelRules(
                    3, 1, 5, 1, 42, 50, 52, 1000, 5.0,
                    List.of(
                            BowlingPlantType.BOWLING_WALL_NUT,
                            BowlingPlantType.BOWLING_WALL_NUT,
                            BowlingPlantType.BOWLING_WALL_NUT,
                            BowlingPlantType.EXPLODE_O_NUT,
                            BowlingPlantType.BOWLING_WALL_NUT,
                            BowlingPlantType.GIANT_WALL_NUT),
                    List.of(
                            ZombieType.NORMAL, ZombieType.CONEHEAD,
                            ZombieType.BUCKETHEAD, ZombieType.KNIGHT,
                            ZombieType.NORMAL, ZombieType.CONEHEAD,
                            ZombieType.BUCKETHEAD, ZombieType.KNIGHT,
                            ZombieType.CONEHEAD, ZombieType.BUCKETHEAD,
                            ZombieType.KNIGHT, ZombieType.GARGANTUAR,
                            ZombieType.CONEHEAD, ZombieType.BUCKETHEAD,
                            ZombieType.KNIGHT, ZombieType.GARGANTUAR));
            default -> throw new IllegalArgumentException(
                    "Wall-nut Bowling level must be 1 through 3.");
        };
    }

    @Override
    public void configure(MiniGameSession session) {
        super.configure(session);
        WallNutBowlingLevelRules rules = rulesFor(session.getConfig().getLevelNumber());
        if (session.getConfig().getStartingResources() != 0
                || session.getConfig().isSkySunEnabled()
                || session.getSimulation().getWorld().getSunBalance() != 0
                || rules.getZombieCount() != session.getConfig().getObjectiveTarget()
                || rules.getDangerScore() != session.getConfig().getThreatBudget()
                || rules.getTimeLimitTicks() != session.getConfig().getTimeLimitTicks()) {
            throw new IllegalStateException(
                    "Wall-nut Bowling catalog and strategy configuration differ.");
        }
        WallNutBowlingState state = new WallNutBowlingState(
                rules, session.getSimulation().getWorld());
        session.setStrategyState(state);
        session.getSimulation().register(state.tickSystem());
        session.getSimulation().register(new ZombieSpecialSystem());
        session.getSimulation().register(new ZombieCombatSystem());
    }

    @Override
    public GameOutcome evaluate(MiniGameSession session) {
        WallNutBowlingState state = session.getStrategyState(WallNutBowlingState.class);
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
            if (input == null) {
                return false;
            }
            String value = input.trim();
            return PLANT.matcher(value).matches() || STATUS.matcher(value).matches();
        }

        @Override
        public Result<String> execute(MiniGameSession session, String input) {
            WallNutBowlingState state = session.getStrategyState(WallNutBowlingState.class);
            if (state == null) {
                Result<String> missing = new Result<>();
                missing.appendToMessage("Wall-nut Bowling state is not initialized");
                return missing;
            }
            Matcher matcher = PLANT.matcher(input.trim());
            if (matcher.matches()) {
                return state.plantFromConveyor(
                        session,
                        Integer.parseInt(matcher.group(1)),
                        Integer.parseInt(matcher.group(2)),
                        Integer.parseInt(matcher.group(3)));
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
