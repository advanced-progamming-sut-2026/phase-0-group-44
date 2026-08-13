package pvz.browser;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;

import pvz.browser.core.PvzAssetBrowser;

/** Desktop entry point for the {@link BrowserAppaa} asset browser. Run with {@code ./gradlew run}. */
public final class BrowserLauncher {
    public static void main(String[] args) {
        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setTitle("PvzAnimKit — Asset Browser");
        config.setWindowedMode(1280, 760);
        config.useVsync(true);
        config.setForegroundFPS(60);
        // config.setBackBufferConfig(8, 8, 8, 16, 8, 0, 4);
        new Lwjgl3Application(new PvzAssetBrowser(), config);
    }
}
