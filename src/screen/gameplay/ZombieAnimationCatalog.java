package screen.gameplay;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import model.inGame.zombie.Zombie;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Loads PAM metadata once and delegates zombie-specific presentation choices
 * to ZombieGraphicsRegistry.
 */
public final class ZombieAnimationCatalog {

    private static final String INTERNAL_PATH =
            "pvz-asset-browser/pvz-assets/animations.json";

    private ZombieAnimationCatalog() {
    }

    public static List<ZombieAnimationInfo> load() {
        FileHandle file = Gdx.files.internal(INTERNAL_PATH);

        if (!file.exists()) {
            file = Gdx.files.local(INTERNAL_PATH);
        }

        if (!file.exists()) {
            throw new IllegalStateException(
                    "Could not find animations.json at "
                            + INTERNAL_PATH
            );
        }

        JsonValue root =
                new JsonReader().parse(file);

        JsonValue animations =
                root.get("animations");

        if (animations == null || !animations.isArray()) {
            throw new IllegalStateException(
                    "animations.json does not contain an animations array"
            );
        }

        List<ZombieAnimationInfo> result =
                new ArrayList<>();

        for (JsonValue entry = animations.child;
             entry != null;
             entry = entry.next) {

            ZombieAnimationInfo info =
                    parseZombieAnimation(entry);

            if (info != null) {
                result.add(info);
            }
        }

        result.sort(
                Comparator.comparing(
                        ZombieAnimationInfo::getName,
                        String.CASE_INSENSITIVE_ORDER
                )
        );

        System.out.println(
                "[ZombieAnimationCatalog] loaded "
                        + result.size()
                        + " zombie PAMs"
        );

        return result;
    }

    public static ZombieAnimationInfo findForZombie(
            Zombie zombie,
            List<ZombieAnimationInfo> animations
    ) {
        ZombieAnimationInfo info =
                ZombieGraphicsRegistry.resolveAnimation(
                        zombie,
                        animations
                );

        if (zombie == null) {
            return info;
        }

        if (info == null) {
            System.err.println(
                    "[ZombieGraphics] NO PAM for "
                            + zombie.getType()
            );
            return null;
        }

        System.out.println(
                "[ZombieGraphics] "
                        + zombie.getType()
                        + " BASE PAM = "
                        + info.getName()
                        + " | "
                        + info.getPath()
                        + " | clips="
                        + info.getClips()
        );

        return info;
    }

    private static ZombieAnimationInfo parseZombieAnimation(
            JsonValue entry
    ) {
        String name =
                entry.getString("name", "");

        String path =
                entry.getString("path", "");

        if (name.isBlank() || path.isBlank()) {
            return null;
        }

        String identity =
                (name + " " + path).toUpperCase();

        if (!path.toUpperCase().endsWith(".PAM")) {
            return null;
        }

        if (!identity.contains("ZOMBIE")
                && !identity.contains("GARGANTUAR")
                && !"PIANO".equalsIgnoreCase(name)) {
            return null;
        }

        JsonValue clipsObject =
                entry.get("clips");

        if (clipsObject == null) {
            return null;
        }

        List<String> clips =
                new ArrayList<>();

        for (JsonValue clip = clipsObject.child;
             clip != null;
             clip = clip.next) {

            if (clip.name != null
                    && !clip.name.isBlank()) {
                clips.add(clip.name);
            }
        }

        if (clips.isEmpty()) {
            return null;
        }

        return new ZombieAnimationInfo(
                name,
                path,
                clips
        );
    }
}
