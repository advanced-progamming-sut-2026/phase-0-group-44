package controller;

import model.Result;
import model.Store;
import model.enums.MenuName;

public class MenuController {

    public Result<String> enterMenu(String menuName) {
        Result<String> result = new Result<>();
        MenuName destination = parseMenu(menuName);

        if (destination == null) {
            result.setStatus(false);
            result.appendToMessage("menu not found");
            return result;
        }

        MenuName currentMenu = Store.getCurrentMenu();

        if (!canEnter(currentMenu, destination)) {
            result.setStatus(false);
            result.appendToMessage(
                    "you cannot enter " + formatMenuName(destination)
                            + " from " + formatMenuName(currentMenu)
            );
            return result;
        }

        Store.setCurrentMenu(destination);

        result.setStatus(true);
        result.setData(formatMenuName(destination));
        result.appendToMessage(
                "entered " + formatMenuName(destination)
        );
        return result;
    }

    public Result<String> showCurrentMenu() {
        Result<String> result = new Result<>();
        MenuName currentMenu = Store.getCurrentMenu();
        String currentMenuName = formatMenuName(currentMenu);

        result.setStatus(true);
        result.setData(currentMenuName);
        result.appendToMessage(
                "current menu: " + currentMenuName
        );
        return result;
    }

    public Result<String> exitMenu() {
        Result<String> result = new Result<>();
        MenuName currentMenu = Store.getCurrentMenu();

        if (currentMenu == MenuName.REGISTER) {
            Store.setRunning(false);

            result.setStatus(true);
            result.setData("exit");
            result.appendToMessage("program finished");
            return result;
        }

        if (currentMenu == MenuName.MAIN) {
            result.setStatus(false);
            result.appendToMessage(
                    "use menu logout to exit the main menu"
            );
            return result;
        }

        MenuName destination = getExitDestination(currentMenu);

        if (destination == null) {
            result.setStatus(false);
            result.appendToMessage(
                    "this menu cannot be exited"
            );
            return result;
        }

        Store.setCurrentMenu(destination);

        result.setStatus(true);
        result.setData(formatMenuName(destination));
        result.appendToMessage(
                "returned to " + formatMenuName(destination)
        );
        return result;
    }

    private boolean canEnter(
            MenuName source,
            MenuName destination
    ) {
        switch (source) {
            case REGISTER:
                return destination == MenuName.LOGIN;

            case LOGIN:
                return destination == MenuName.MAIN
                        && Store.getLoggedInUser() != null;

            case MAIN:
                return destination == MenuName.GAME
                        || destination == MenuName.SETTINGS
                        || destination == MenuName.NEWS
                        || destination == MenuName.PROFILE;

            case GAME:
                return destination == MenuName.COLLECTION;

            default:
                return false;
        }
    }

    private MenuName getExitDestination(MenuName source) {
        switch (source) {
            case LOGIN:
                return MenuName.REGISTER;

            case GAME:
            case SETTINGS:
            case NEWS:
            case PROFILE:
                return MenuName.MAIN;

            case COLLECTION:
                return MenuName.GAME;

            default:
                return null;
        }
    }

    private MenuName parseMenu(String menuName) {
        if (menuName == null) {
            return null;
        }

        String normalized = menuName.trim()
                .toUpperCase()
                .replace("-", "")
                .replace("_", "")
                .replace(" ", "");

        if (normalized.endsWith("MENU")) {
            normalized = normalized.substring(
                    0,
                    normalized.length() - 4
            );
        }

        for (MenuName menu : MenuName.values()) {
            String enumName = menu.name()
                    .replace("_", "");

            if (enumName.equals(normalized)) {
                return menu;
            }
        }

        return null;
    }

    private String formatMenuName(MenuName menu) {
        return menu.name()
                .toLowerCase()
                .replace("_", " ")
                + " menu";
    }
}
