package pvz.browser.core;

public class BrowserAppSettings {
    public final String assetsRootPath;
    public final String resourcesJsonPath;
    public final String atlasesPath;
    public final String exportsRootPath;

    public BrowserAppSettings(String imagesRoot, String resourcesJsonPath, String atlasesRootPath,
            String exportsRootPath) {
        this.assetsRootPath = imagesRoot;
        this.resourcesJsonPath = resourcesJsonPath;
        this.atlasesPath = atlasesRootPath;
        this.exportsRootPath = exportsRootPath;
    }
}
