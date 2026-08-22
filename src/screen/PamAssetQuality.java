package screen;

import com.badlogic.gdx.files.FileHandle;

/** Picks the highest PvZ2 texture tier actually present in the extracted asset pack. */
public final class PamAssetQuality {
    private PamAssetQuality() {
    }

    public static String bestResolution(FileHandle assets) {
        if (assets == null) {
            return "768";
        }

        FileHandle images1536 = assets.child("IMAGES").child("1536");
        FileHandle atlases = assets.child("ATLASES");

        if (images1536.exists() || has1536Atlas(atlases)) {
            return "1536";
        }

        return "768";
    }

    private static boolean has1536Atlas(FileHandle atlases) {
        if (atlases == null || !atlases.exists() || !atlases.isDirectory()) {
            return false;
        }

        for (FileHandle file : atlases.list()) {
            if (file.name().contains("_1536_")) {
                return true;
            }
        }

        return false;
    }
}
