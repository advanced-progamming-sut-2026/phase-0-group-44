package model.scored;

import java.time.LocalDate;
import java.util.List;

/** The deterministic zombie schedule shared by every player on one date. */
public record DailyZombieSequence(LocalDate date, String rulesVersion,
                                  List<DailyZombieSpawn> spawns) {
    public DailyZombieSequence {
        if (date == null || rulesVersion == null || rulesVersion.isBlank()
                || spawns == null || spawns.isEmpty()) {
            throw new IllegalArgumentException("A daily sequence requires a date, version, and spawns.");
        }
        spawns = List.copyOf(spawns);
    }
}
