package model.sim.zombie;

import model.inGame.zombie.ZombieDefinition;

import java.util.ArrayList;
import java.util.List;

/** Restricts a canonical spec source to common and current-chapter zombies. */
public final class ChapterZombieSpecSource implements ZombieSpecSource {
    private final ZombieSpecSource delegate;
    private final String levelName;

    public ChapterZombieSpecSource(ZombieSpecSource delegate, String levelName) {
        this.delegate = delegate;
        this.levelName = levelName;
    }

    @Override
    public List<ZombieSpec> availableSpecs() {
        List<ZombieSpec> result = new ArrayList<>();
        for (ZombieSpec spec : delegate.availableSpecs()) {
            ZombieDefinition definition = spec.getDefinition();
            if (definition == null || definition.getChapter().isAllowedIn(levelName)) {
                result.add(spec);
            }
        }
        return result;
    }
}
