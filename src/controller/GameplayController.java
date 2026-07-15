package controller;

import model.Result;
import model.sim.Simulation;
import model.sim.sun.SunCollector;

import java.util.List;

/**
 * The gameplay command context entered after {@code start game}: driving the
 * clock and the sun commands against the active {@link Simulation}.
 */
public class GameplayController {

    private final Simulation simulation;

    public GameplayController(Simulation simulation) {
        this.simulation = simulation;
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

        result.setStatus(true);
        result.setData(events);

        if (events.isEmpty()) {
            result.appendToMessage("advanced " + ticks + " tick(s)");
            return result;
        }

        for (int i = 0; i < events.size(); i++) {
            result.appendToMessage(events.get(i));

            if (i < events.size() - 1) {
                result.appendToMessage("\n");
            }
        }

        return result;
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
