package model.menu;

import model.enums.MenuName;

/** The outcome that {@code menu exit} has in one menu. */
public final class MenuExit {

    /** Kinds of exit a menu can have. */
    public enum Kind {
        /** Leaving this menu moves to another menu. */
        RETURN_TO,
        /** Leaving this menu ends the program, after progress is saved. */
        TERMINATE,
        /** This menu cannot be left with {@code menu exit}. */
        FORBIDDEN
    }

    private final Kind kind;
    private final MenuName destination;

    private MenuExit(Kind kind, MenuName destination) {
        this.kind = kind;
        this.destination = destination;
    }

    public static MenuExit returnTo(MenuName destination) {
        return new MenuExit(Kind.RETURN_TO, destination);
    }

    public static MenuExit terminate() {
        return new MenuExit(Kind.TERMINATE, null);
    }

    public static MenuExit forbidden() {
        return new MenuExit(Kind.FORBIDDEN, null);
    }

    public Kind getKind() {
        return kind;
    }

    /** The menu returned to, or {@code null} unless the kind is RETURN_TO. */
    public MenuName getDestination() {
        return destination;
    }
}
