package screen.gameplay;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ZombieAnimationInfo {

    private final String name;
    private final String path;
    private final List<String> clips;

    public ZombieAnimationInfo(
            String name,
            String path,
            List<String> clips
    ) {
        this.name = name;
        this.path = path;
        this.clips = new ArrayList<>(clips);
    }

    public String getName() {
        return name;
    }

    public String getPath() {
        return path;
    }

    public List<String> getClips() {
        return Collections.unmodifiableList(clips);
    }

    @Override
    public String toString() {
        return name;
    }
}