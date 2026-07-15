package controller;

import model.Result;
import model.Store;
import model.enums.MenuName;
import model.inGame.GameSession;
import model.sim.GameOutcome;
import model.sim.Simulation;
import model.sim.SimulationWorld;
import model.sim.sun.SunCollector;
import model.sim.zombie.ZombieDeath;
import model.sim.zombie.ZombieInstance;
import model.user.User;
import service.GameConclusionService;
import service.RewardService;

import java.util.ArrayList;
import java.util.List;

/**
 * The gameplay command context entered after {@code start game}: driving the
 * clock and the sun/board/zombie commands against the active {@link Simulation}.
 */
public class GameplayController {

    /** Winning line printed when the player clears every wave. */
    public static final String WIN_MESSAGE =
            "Dear humanz, zis is not done yet; we will come back to eat your brainz, humanz.";

    private final Simulation simulation;
    private final BoardController board;
    private final RewardService rewardService;
    private final GameConclusionService conclusionService;
    private final GameSession session;
    private final User user;

    public GameplayController(Simulation simulation) {
        this(simulation, null);
    }

    public GameplayController(Simulation simulation, BoardController board) {
        this(simulation, board, null, null, null, null);
    }

    public GameplayController(
            Simulation simulation,
            BoardController board,
            RewardService rewardService,
            GameConclusionService conclusionService,
            GameSession session,
            User user
    ) {
        this.simulation = simulation;
        this.board = board;
        this.rewardService = rewardService;
        this.conclusionService = conclusionService;
        this.session = session;
        this.user = user;
    }

    public BoardController getBoard() {
        return board;
    }

    public Simulation getSimulation() {
        return simulation;
    }

    /** Handles {@code advance time -t <count> ticks}. */
    public Result<List<String>> advanceTime(int ticks) {
        Result<List<String>> result = new Result<>();

        if (ticks <= 0) {
            result.appendToMessage("tick count must be a positive integer");
            return result;
        }

        List<String> events = simulation.advance(ticks);
        List<String> messages = new ArrayList<>(events);

        messages.addAll(drainRewards());
        messages.addAll(resolveOutcome());

        result.setStatus(true);
        result.setData(messages);

        if (messages.isEmpty()) {
            result.appendToMessage("advanced " + ticks + " tick(s)");
            return result;
        }

        for (int i = 0; i < messages.size(); i++) {
            result.appendToMessage(messages.get(i));

            if (i < messages.size() - 1) {
                result.appendToMessage("\n");
            }
        }

        return result;
    }

    /** Handles {@code release the nuke}: kill every zombie, with normal cleanup. */
    public Result<List<String>> releaseNuke() {
        Result<List<String>> result = new Result<>();
        SimulationWorld world = simulation.getWorld();
        List<String> messages = new ArrayList<>();

        for (ZombieInstance zombie : world.getZombieInstances()) {
            if (zombie.isDead()) {
                continue;
            }

            zombie.kill();
            messages.add("Zombie of type " + zombie.getSpec().getName()
                    + " is dead at (" + zombie.getTileX() + ", " + zombie.getRow() + ")");
            world.recordDeath(new ZombieDeath(
                    zombie.getSpec(), zombie.getTileX(), zombie.getRow(),
                    zombie.isGlowing(), true));
        }

        world.getZombies().removeIf(z -> z instanceof ZombieInstance && ((ZombieInstance) z).isDead());

        messages.addAll(drainRewards());

        result.setStatus(true);
        result.setData(messages);
        result.appendToMessage(messages.isEmpty()
                ? "no zombies to nuke" : String.join("\n", messages));

        return result;
    }

    /**
     * Applies rewards for zombies that died. Cheat kills (the nuke) are cleaned
     * up like any death but grant no rewards — a deliberate choice, since no
     * repository test or asset says a cheat should farm drops, and letting a
     * cheat produce coins/diamonds/plant food would be exploitable.
     */
    private List<String> drainRewards() {
        List<String> messages = new ArrayList<>();
        SimulationWorld world = simulation.getWorld();

        for (ZombieDeath death : world.getPendingDeaths()) {
            if (death.isCheatKill() || rewardService == null || user == null) {
                continue;
            }

            messages.addAll(rewardService.onZombieDeath(death.isGlowing(), user, world));
        }

        world.getPendingDeaths().clear();

        return messages;
    }

    private List<String> resolveOutcome() {
        List<String> messages = new ArrayList<>();
        GameOutcome outcome = simulation.getWorld().getOutcome();

        if (outcome == GameOutcome.WON) {
            if (conclusionService != null) {
                messages.addAll(conclusionService.onWin(user, session));
            }

            Store.setCurrentMenu(MenuName.GAME);
            Store.setActiveSimulation(null);
        } else if (outcome == GameOutcome.LOST) {
            if (conclusionService != null) {
                conclusionService.onLoss(user, session);
            }

            Store.setCurrentMenu(MenuName.GAME);
            Store.setActiveSimulation(null);
        }

        return messages;
    }

    /** Handles {@code collect sun -l (<x>, <y>)}. */
    public Result<Integer> collectSun(int x, int y) {
        Result<Integer> result = new Result<>();
        SunCollector.Outcome outcome = simulation.collectSun(x, y);

        if (!outcome.isCollected()) {
            result.appendToMessage("no sun at (" + x + ", " + y + ")");
            result.setData(simulation.getSunAmount());
            return result;
        }

        result.setStatus(true);
        result.setData(simulation.getSunAmount());

        if (outcome.isExploded()) {
            result.appendToMessage("radioactive sun exploded at (" + x + ", " + y + ")");
            return result;
        }

        result.appendToMessage("collected " + outcome.getGained()
                + " sun; total " + simulation.getSunAmount());

        return result;
    }

    /** Handles {@code show sun amount}. */
    public Result<Integer> showSunAmount() {
        Result<Integer> result = new Result<>();

        result.setStatus(true);
        result.setData(simulation.getSunAmount());
        result.appendToMessage("sun: " + simulation.getSunAmount());

        return result;
    }

    /** Handles {@code cheat add -n <count> suns}. */
    public Result<Integer> cheatAddSuns(int count) {
        Result<Integer> result = new Result<>();

        if (count <= 0) {
            result.appendToMessage("count must be a positive integer");
            result.setData(simulation.getSunAmount());
            return result;
        }

        simulation.addSun(count);

        result.setStatus(true);
        result.setData(simulation.getSunAmount());
        result.appendToMessage("sun: " + simulation.getSunAmount());

        return result;
    }
}
