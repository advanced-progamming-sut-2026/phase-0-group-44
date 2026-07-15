package view;

import controller.MenuController;
import controller.ShopController;
import model.enums.Command;

import java.util.regex.Matcher;

/** Reads shop commands. */
public class ShopView extends MenuView {
    private final ShopController controller;

    public ShopView(MenuController menuController, ShopController controller) {
        super(menuController);
        this.controller = controller;
    }

    public void checkCommand(String input) {
        if (Command.SHOP_LIST.matches(input)) {
            print(controller.list());
            return;
        }

        if (Command.SHOP_DAILY.matches(input)) {
            print(controller.daily());
            return;
        }

        if (Command.SHOP_BUY.matches(input)) {
            Matcher matcher = Command.SHOP_BUY.getMatcher(input);
            matcher.matches();

            int count;
            try {
                count = Integer.parseInt(matcher.group(2));
            } catch (NumberFormatException tooLarge) {
                System.out.println("count must be a positive number");
                return;
            }

            print(controller.buy(matcher.group(1), count, matcher.group(3)));
            return;
        }

        if (handleCommonCommand(input)) {
            return;
        }

        System.out.println("invalid command");
    }
}
