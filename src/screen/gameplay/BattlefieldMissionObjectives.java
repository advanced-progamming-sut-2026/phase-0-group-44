package screen.gameplay;

import model.config.GameWorld;
import model.inGame.GameSession;
import model.level.AdventureLevelConfig;
import model.level.SpecialLevelType;
import model.level.TimedWarObjective;
import model.sim.TickContext;

import java.util.ArrayList;
import java.util.List;

/** Builds the exact objective text shown by both the start modal and pause menu. */
public final class BattlefieldMissionObjectives {
    private BattlefieldMissionObjectives() { }

    public record MissionInfo(String levelName, List<String> objectives) { }

    public static MissionInfo forSession(GameSession session, GameWorld previewWorld) {
        if (session == null || session.getLevel() == null) {
            String name = previewWorld == null ? "BATTLEFIELD PREVIEW"
                    : previewWorld.getDisplayName() + " - PREVIEW";
            return new MissionInfo(name, List.of(
                    "Don't let the zombies reach the house."
            ));
        }

        String name = session.getLevel().getName();
        AdventureLevelConfig config = session.getLevel().getAdventureConfig();
        List<String> objectives = new ArrayList<>();

        if (config == null || config.getSpecialType() == null) {
            objectives.add("Don't let the zombies reach the house.");
            return new MissionInfo(name, List.copyOf(objectives));
        }

        SpecialLevelType type = config.getSpecialType();
        switch (type) {
            case SAVE_OUR_SEEDS -> {
                objectives.add("Protect every marked plant.");
                objectives.add("Don't let the zombies reach the house.");
            }
            case DEAD_LINE ->
                    objectives.add("Do not let any zombie cross the red line.");
            case TIMED_WAR -> {
                int seconds = Math.max(1,
                        (config.getTimedWarTicks() + TickContext.TICKS_PER_SECOND - 1)
                                / TickContext.TICKS_PER_SECOND);
                int target = Math.max(1, config.getTimedWarTarget());
                TimedWarObjective objective = config.getTimedWarObjective();
                if (objective == TimedWarObjective.SUN_PRODUCED) {
                    objectives.add("Produce at least " + target + " sun in "
                            + seconds + " seconds.");
                } else {
                    objectives.add("Defeat at least " + target + " zombies in "
                            + seconds + " seconds.");
                }
                objectives.add("Don't let the zombies reach the house.");
            }
            case LOVE_YOUR_PLANTS -> {
                objectives.add("Lose no more than "
                        + Math.max(1, config.getMaximumPlantLosses()) + " plants.");
                objectives.add("Don't let the zombies reach the house.");
            }
            case PLANT_WHAT_YOU_GET -> {
                objectives.add("Set up your defense, then press START WAVE.");
                objectives.add("Don't let the zombies reach the house.");
            }
            case CONVEYOR_BELT -> {
                objectives.add("Plant using seed packets from the conveyor.");
                objectives.add("Don't let the zombies reach the house.");
            }
            case LOCKED_PLANTS -> {
                objectives.add("Play with the level's given / locked plant selection.");
                objectives.add("Don't let the zombies reach the house.");
            }
            case NIGHT_OPS -> {
                objectives.add("Sky sun is disabled; produce sun with plants.");
                objectives.add("Don't let the zombies reach the house.");
            }
        }

        return new MissionInfo(name, List.copyOf(objectives));
    }
}
