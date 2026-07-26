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

/** Three-level mandatory I, Zombie strategy. */
public final class IZombie extends MiniGame {
    private static final Pattern PLACE = Pattern.compile(
            "^place\\s+zombie\\s+-t\\s+([a-zA-Z0-9 _-]+?)\\s+-l\\s+\\(\\s*(-?\\d+)\\s*,\\s*(-?\\d+)\\s*\\)$",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern STATUS = Pattern.compile(
            "^(?:show\\s+i[- ]?zombie|i[- ]?zombie\\s+status|show\\s+brains)$",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern SHOW_MAP = Pattern.compile(
            "^show\\s+map$", Pattern.CASE_INSENSITIVE);

    private final RandomSource random;

    public IZombie(RandomSource random) {
        if (random == null) {
            throw new IllegalArgumentException("I, Zombie requires a random source.");
        }
        this.random = random;
    }

    @Override
    public MiniGameId getId() {
        return MiniGameId.I_ZOMBIE;
    }

    public static IZombieLevelRules rulesFor(int level) {
        return switch (level) {
            case 1 -> new IZombieLevelRules(
                    1, 5, 8, 16, 1800,
                    240, 120, 600, 20, 25,
                    List.of(PlantType.PEASHOOTER, PlantType.WALL_NUT,
                            PlantType.CABBAGE_PULT, PlantType.SNOW_PEA),
                    prices(
                            ZombieType.NORMAL, 50,
                            ZombieType.CONEHEAD, 75,
                            ZombieType.IMP, 50,
                            ZombieType.BUCKETHEAD, 125,
                            ZombieType.PROSPECTOR, 100));
            case 2 -> new IZombieLevelRules(
                    2, 6, 12, 30, 1500,
                    260, 130, 600, 20, 25,
                    List.of(PlantType.PEASHOOTER, PlantType.REPEATER,
                            PlantType.WALL_NUT, PlantType.CABBAGE_PULT,
                            PlantType.SNOW_PEA, PlantType.BONK_CHOY),
                    prices(
                            ZombieType.NORMAL, 50,
                            ZombieType.CONEHEAD, 75,
                            ZombieType.BUCKETHEAD, 125,
                            ZombieType.NEWSPAPER_ZOMBIE, 125,
                            ZombieType.EXPLORER, 100));
            case 3 -> new IZombieLevelRules(
                    3, 7, 16, 48, 1200,
                    280, 140, 600, 20, 25,
                    List.of(PlantType.REPEATER, PlantType.TALL_NUT,
                            PlantType.MELON_PULT, PlantType.SNOW_PEA,
                            PlantType.BONK_CHOY, PlantType.CABBAGE_PULT),
                    prices(
                            ZombieType.IMP, 50,
                            ZombieType.KNIGHT, 175,
                            ZombieType.ALL_STAR, 200,
                            ZombieType.PARASOL_ZOMBIE, 100,
                            ZombieType.WIZARD, 175));
            default -> throw new IllegalArgumentException("I, Zombie level must be 1 through 3.");
        };
    }

    private static Map<ZombieType, Integer> prices(Object... entries) {
        Map<ZombieType, Integer> result = new LinkedHashMap<>();
        for (int index = 0; index < entries.length; index += 2) {
            result.put((ZombieType) entries[index], (Integer) entries[index + 1]);
        }
        return result;
    }

    @Override
    public void configure(MiniGameSession session) {
        super.configure(session);
        IZombieLevelRules rules = rulesFor(session.getConfig().getLevelNumber());
        if (session.getConfig().getStartingResources() != 150
                || session.getConfig().isSkySunEnabled()
                || session.getSimulation().getWorld().getSunBalance() != 150
                || session.getConfig().getObjectiveTarget() != rules.getPreplacedPlantCount()
                || session.getConfig().getThreatBudget() != rules.getPlantPressure()
                || session.getConfig().getTimeLimitTicks() != rules.getTimeLimitTicks()) {
            throw new IllegalStateException("I, Zombie catalog and strategy configuration differ.");
        }
        IZombieState state = new IZombieState(rules, session.getSimulation().getWorld(), random);
        session.setStrategyState(state);
        session.getSimulation().register(state.tickSystem());
        session.getSimulation().register(new ZombieSpecialSystem());
        session.getSimulation().register(new ZombieCombatSystem() {
            @Override
            protected void reachEndOfLane(
                    model.sim.TickContext context,
                    model.sim.SimulationWorld world,
                    model.sim.zombie.ZombieInstance zombie
            ) {
                state.consumeBrain(context, world, zombie);
            }
        });
    }

    @Override
    public GameOutcome evaluate(MiniGameSession session) {
        IZombieState state = session.getStrategyState(IZombieState.class);
        return state == null ? session.getSimulation().getWorld().getOutcome()
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
            return PLACE.matcher(value).matches() || STATUS.matcher(value).matches()
                    || SHOW_MAP.matcher(value).matches();
        }

        @Override
        public Result<String> execute(MiniGameSession session, String input) {
            IZombieState state = session.getStrategyState(IZombieState.class);
            if (state == null) {
                Result<String> missing = new Result<>();
                missing.appendToMessage("I, Zombie state is not initialized");
                return missing;
            }
            Matcher matcher = PLACE.matcher(input.trim());
            if (matcher.matches()) {
                return state.placeZombie(
                        session,
                        matcher.group(1),
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
