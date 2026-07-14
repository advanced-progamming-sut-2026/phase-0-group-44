package view;

import controller.MenuController;

/**
 * Placeholder view used while the individual menu views are still empty: it
 * offers the shared menu commands and nothing else.
 */
public class CommonMenuView extends MenuView {

    public CommonMenuView(MenuController menuController) {
        super(menuController);
    }

    /** Reads one line of input and reports whether it was understood. */
    public void checkCommand(String input) {
        if (!handleCommonCommand(input)) {
            System.out.println("invalid command");
        }
    }
}
