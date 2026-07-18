package model.miniGame;

import model.Result;

/** Extension point for commands owned by one minigame strategy. */
public interface MiniGameCommandExtension {
    boolean supports(String input);

    /** Whether this command may run while the attempt is selected but not started. */
    default boolean availableBeforeStart() {
        return false;
    }

    Result<String> execute(MiniGameSession session, String input);
}
