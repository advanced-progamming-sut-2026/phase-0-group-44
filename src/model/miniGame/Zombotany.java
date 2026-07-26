package model.miniGame;

import model.Result;
import model.sim.GameOutcome;
import model.sim.zombie.ZombieCombatSystem;
import model.sim.zombie.ZombieSpecialSystem;
import util.RandomSource;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Three-level Zombotany strategy with a normal-style pre-start plant selection. */
public final class Zombotany extends MiniGame {
    private static final Pattern ADD = Pattern.compile(
            "^add\\s+plant\\s+-t\\s+(.+)$", Pattern.CASE_INSENSITIVE);
    private static final Pattern REMOVE = Pattern.compile(
            "^remove\\s+plant\\s+-t\\s+(.+)$", Pattern.CASE_INSENSITIVE);
    private static final Pattern SHOW_AVAILABLE = Pattern.compile(
            "^show\\s+available\\s+plants$", Pattern.CASE_INSENSITIVE);
    private static final Pattern SHOW_SELECTED = Pattern.compile(
            "^show\\s+selected\\s+plants$", Pattern.CASE_INSENSITIVE);
    private static final Pattern PLANT = Pattern.compile(
            "^plant\\s+plant\\s+-t\\s+(.+?)\\s+-l\\s+\\(\\s*(-?\\d+)\\s*,\\s*(-?\\d+)\\s*\\)$",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern COLLECT = Pattern.compile(
            "^collect\\s+sun\\s+-l\\s+\\(\\s*(-?\\d+)\\s*,\\s*(-?\\d+)\\s*\\)$",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern STATUS = Pattern.compile(
            "^(?:show\\s+zombotany|zombotany\\s+status)$", Pattern.CASE_INSENSITIVE);
    private static final Pattern SHOW_MAP = Pattern.compile(
            "^show\\s+map$", Pattern.CASE_INSENSITIVE);

    private final RandomSource random;

    public Zombotany(RandomSource random) {
        if (random == null) {
            throw new IllegalArgumentException("Zombotany randomness is required.");
        }
        this.random = random;
    }

    @Override
    public MiniGameId getId() {
        return MiniGameId.ZOMBOTANY;
    }

    public static ZombotanyLevelRules rulesFor(int level) {
        List<ZombotanyTrait> base = List.of(
                ZombotanyTrait.PEASHOOTER,
                ZombotanyTrait.WALL_NUT,
                ZombotanyTrait.JALAPENO,
                ZombotanyTrait.SQUASH);
        return switch (level) {
            case 1 -> new ZombotanyLevelRules(
                    1, 8, 120, 12, 2400, 8, base);
            case 2 -> new ZombotanyLevelRules(
                    2, 12, 90, 24, 2100, 8,
                    List.of(
                            ZombotanyTrait.PEASHOOTER,
                            ZombotanyTrait.SQUASH,
                            ZombotanyTrait.WALL_NUT,
                            ZombotanyTrait.JALAPENO,
                            ZombotanyTrait.PEASHOOTER,
                            ZombotanyTrait.WALL_NUT));
            case 3 -> new ZombotanyLevelRules(
                    3, 16, 60, 40, 1800, 8,
                    List.of(
                            ZombotanyTrait.SQUASH,
                            ZombotanyTrait.PEASHOOTER,
                            ZombotanyTrait.JALAPENO,
                            ZombotanyTrait.WALL_NUT,
                            ZombotanyTrait.SQUASH,
                            ZombotanyTrait.JALAPENO,
                            ZombotanyTrait.PEASHOOTER,
                            ZombotanyTrait.WALL_NUT));
            default -> throw new IllegalArgumentException("Zombotany level must be 1 through 3.");
        };
    }

    @Override
    public void prepare(MiniGameSession session) {
        session.setStrategyState(new ZombotanyState(
                rulesFor(session.getConfig().getLevelNumber()), random));
    }

    @Override
    public Result<String> validateStart(MiniGameSession session) {
        Result<String> result = new Result<>();
        ZombotanyState state = session.getStrategyState(ZombotanyState.class);
        if (state == null || state.getSelection().isEmpty()) {
            result.appendToMessage("select at least one plant before starting Zombotany");
            return result;
        }
        result.setStatus(true);
        result.setData("ready");
        return result;
    }

    @Override
    public void configure(MiniGameSession session) {
        super.configure(session);
        ZombotanyLevelRules rules = rulesFor(session.getConfig().getLevelNumber());
        if (!session.getConfig().isSkySunEnabled()
                || session.getConfig().getStartingResources() != 50
                || session.getConfig().getObjectiveTarget() != rules.getZombieCount()
                || session.getConfig().getThreatBudget() != rules.getDangerScore()
                || session.getConfig().getTimeLimitTicks() != rules.getTimeLimitTicks()) {
            throw new IllegalStateException("Zombotany catalog and strategy configuration differ.");
        }
        ZombotanyState state = session.getStrategyState(ZombotanyState.class);
        if (state == null) {
            throw new IllegalStateException("Zombotany selection state is missing.");
        }
        state.initialize();
        session.getSimulation().register(state.traitAndPlantSystem());
        session.getSimulation().register(new ZombieSpecialSystem());
        session.getSimulation().register(new ZombieCombatSystem());
        session.getSimulation().register(state.cleanupSystem());
    }

    @Override
    public GameOutcome evaluate(MiniGameSession session) {
        ZombotanyState state = session.getStrategyState(ZombotanyState.class);
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
            return ADD.matcher(value).matches()
                    || REMOVE.matcher(value).matches()
                    || SHOW_AVAILABLE.matcher(value).matches()
                    || SHOW_SELECTED.matcher(value).matches()
                    || PLANT.matcher(value).matches()
                    || COLLECT.matcher(value).matches()
                    || STATUS.matcher(value).matches()
                    || SHOW_MAP.matcher(value).matches();
        }

        @Override
        public boolean availableBeforeStart() {
            return true;
        }

        @Override
        @SuppressWarnings("PMD.ExcessiveMethodLength")
        public Result<String> execute(MiniGameSession session, String input) {
            ZombotanyState state = session.getStrategyState(ZombotanyState.class);
            if (state == null) {
                Result<String> missing = new Result<>();
                missing.appendToMessage("Zombotany state is not initialized");
                return missing;
            }
            String value = input.trim();
            Matcher matcher = ADD.matcher(value);
            if (matcher.matches()) {
                if (session.getState() != MiniGameLifecycleState.SELECTED) {
                    return rejected("plant selection is closed after Zombotany starts");
                }
                return state.addSelectedPlant(matcher.group(1));
            }
            matcher = REMOVE.matcher(value);
            if (matcher.matches()) {
                if (session.getState() != MiniGameLifecycleState.SELECTED) {
                    return rejected("plant selection is closed after Zombotany starts");
                }
                return state.removeSelectedPlant(matcher.group(1));
            }
            if (SHOW_AVAILABLE.matcher(value).matches()) {
                return success(state.availablePlants());
            }
            if (SHOW_SELECTED.matcher(value).matches()) {
                return success(state.selectionStatus());
            }
            matcher = PLANT.matcher(value);
            if (matcher.matches()) {
                if (session.getState() != MiniGameLifecycleState.RUNNING) {
                    return rejected("start Zombotany before planting");
                }
                return state.plant(
                        session,
                        matcher.group(1),
                        Integer.parseInt(matcher.group(2)),
                        Integer.parseInt(matcher.group(3)));
            }
            matcher = COLLECT.matcher(value);
            if (matcher.matches()) {
                if (session.getState() != MiniGameLifecycleState.RUNNING) {
                    return rejected("start Zombotany before collecting sun");
                }
                return state.collectSun(
                        session,
                        Integer.parseInt(matcher.group(1)),
                        Integer.parseInt(matcher.group(2)));
            }
            if (SHOW_MAP.matcher(value).matches()) {
                if (session.getState() != MiniGameLifecycleState.RUNNING) {
                    return rejected("start Zombotany before viewing the map");
                }
                return new controller.MiniGameBoardController(
                        session.getSimulation().getWorld(), null, null).showMap();
            }
            return success(session.getState() == MiniGameLifecycleState.SELECTED
                    ? state.selectionStatus() : state.status(session));
        }

        private Result<String> success(String message) {
            Result<String> result = new Result<>();
            result.setStatus(true);
            result.setData(message);
            result.appendToMessage(message);
            return result;
        }

        private Result<String> rejected(String message) {
            Result<String> result = new Result<>();
            result.appendToMessage(message);
            return result;
        }
    }
}
