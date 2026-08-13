package pvz.browser.core;

import pvz.browser.screens.animations.AnimationsScreen;
import pvz.browser.screens.atlases.AtlasesScreen;


public class Screens {
    public static AnimationsScreen animations;
    public static AtlasesScreen atlases;

    public static void init() {
        animations = new AnimationsScreen();
        atlases = new AtlasesScreen();
    }
}
