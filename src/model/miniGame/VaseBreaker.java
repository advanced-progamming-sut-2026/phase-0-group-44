package model.miniGame;

import model.Result;
import model.enums.PlantType;
import model.enums.ZombieType;
import model.sim.GameOutcome;
import model.sim.zombie.ZombieCombatSystem;
import model.sim.zombie.ZombieSpecialSystem;
import util.RandomSource;
import util.SeededRandomSource;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Three-level mandatory Vasebreaker strategy and its command extensions. */
public final class VaseBreaker extends MiniGame {
    private static final Pattern BREAK = Pattern.compile(
            "^break\\s+vase\\s+-l\\s+\\(\\s*(-?\\d+)\\s*,\\s*(-?\\d+)\\s*\\)$",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern COLLECT = Pattern.compile(
            "^collect\\s+packet\\s+-i\\s+(\\d+)$", Pattern.CASE_INSENSITIVE);
    private static final Pattern PLANT = Pattern.compile(
            "^plant\\s+packet\\s+-i\\s+(\\d+)\\s+-l\\s+\\(\\s*(-?\\d+)\\s*,\\s*(-?\\d+)\\s*\\)$",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern STATUS = Pattern.compile(
            "^(?:show\\s+vases|vasebreaker\\s+status)$", Pattern.CASE_INSENSITIVE);
    private static final Pattern SHOW_MAP = Pattern.compile(
            "^show\\s+map$", Pattern.CASE_INSENSITIVE);

    private final RandomSource random;

    public VaseBreaker() {
        this(new SeededRandomSource());
    }

    public VaseBreaker(RandomSource random) {
        if (random == null) {
            throw new IllegalArgumentException("Vasebreaker randomness is required.");
        }
        this.random = random;
    }

    @Override
    public MiniGameId getId() {
        return MiniGameId.VASE_BREAKER;
    }

    public static VaseBreakerLevelRules rulesFor(int level) {
        return switch (level) {
            case 1 -> new VaseBreakerLevelRules(
                    1, 2, 2, 3, 1, 0, 300,
                    List.of(PlantType.PEASHOOTER, PlantType.CABBAGE_PULT,
                            PlantType.WALL_NUT),
                    List.of(ZombieType.NORMAL, ZombieType.CONEHEAD));
            case 2 -> new VaseBreakerLevelRules(
                    2, 2, 4, 3, 2, 1, 240,
                    List.of(PlantType.PEASHOOTER, PlantType.REPEATER,
                            PlantType.CABBAGE_PULT, PlantType.WALL_NUT),
                    List.of(ZombieType.NORMAL, ZombieType.CONEHEAD,
                            ZombieType.BUCKETHEAD));
            case 3 -> new VaseBreakerLevelRules(
                    3, 1, 6, 5, 2, 2, 180,
                    List.of(PlantType.PEASHOOTER, PlantType.REPEATER,
                            PlantType.CABBAGE_PULT, PlantType.SNOW_PEA,
                            PlantType.WALL_NUT),
                    List.of(ZombieType.NORMAL, ZombieType.CONEHEAD,
                            ZombieType.BUCKETHEAD, ZombieType.KNIGHT));
            default -> throw new IllegalArgumentException("Vasebreaker level must be 1 through 3.");
        };
    }

    @Override
    public void configure(MiniGameSession session) {
        super.configure(session);
        VaseBreakerLevelRules rules = rulesFor(session.getConfig().getLevelNumber());
        if (rules.getVaseCount() != session.getConfig().getObjectiveTarget()
                || rules.getDangerScore() != session.getConfig().getThreatBudget()) {
            throw new IllegalStateException("Vasebreaker catalog and strategy configuration differ.");
        }
        VaseBreakerState state = new VaseBreakerState(
                rules, random, session.getSimulation().getWorld());
        session.setStrategyState(state);

        // Packet timers and temporary plants use the shared clock. Standard
        // zombie abilities, movement, lawn mowers, and brain loss remain shared.
        session.getSimulation().register(state.tickSystem());
        session.getSimulation().register(new ZombieSpecialSystem());
        session.getSimulation().register(new ZombieCombatSystem());
    }

    @Override
    public GameOutcome evaluate(MiniGameSession session) {
        VaseBreakerState state = session.getStrategyState(VaseBreakerState.class);
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
            return BREAK.matcher(value).matches()
                    || COLLECT.matcher(value).matches()
                    || PLANT.matcher(value).matches()
                    || STATUS.matcher(value).matches()
                    || SHOW_MAP.matcher(value).matches();
        }

        @Override
        public Result<String> execute(MiniGameSession session, String input) {
            VaseBreakerState state = session.getStrategyState(VaseBreakerState.class);
            if (state == null) {
                Result<String> missing = new Result<>();
                missing.appendToMessage("Vasebreaker state is not initialized");
                return missing;
            }
            Matcher matcher = BREAK.matcher(input.trim());
            if (matcher.matches()) {
                return state.breakVase(session,
                        Integer.parseInt(matcher.group(1)),
                        Integer.parseInt(matcher.group(2)));
            }
            matcher = COLLECT.matcher(input.trim());
            if (matcher.matches()) {
                return state.collectPacket(session, Integer.parseInt(matcher.group(1)));
            }
            matcher = PLANT.matcher(input.trim());
            if (matcher.matches()) {
                return state.plantPacket(session,
                        Integer.parseInt(matcher.group(1)),
                        Integer.parseInt(matcher.group(2)),
                        Integer.parseInt(matcher.group(3)));
            }
            if (SHOW_MAP.matcher(input.trim()).matches()) {
                return new controller.MiniGameBoardController(
                        session.getSimulation().getWorld(), null, null).showMap();
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
