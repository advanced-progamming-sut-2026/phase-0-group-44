package screen.gameplay;

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
        this.name = name == null ? "" : name;
        this.path = path == null ? "" : path;
        this.clips = clips == null
                ? List.of()
                : List.copyOf(clips);
    }

    public String getName() {
        return name;
    }

    public String getPath() {
        return path;
    }

    public List<String> getClips() {
        return clips;
    }
}
