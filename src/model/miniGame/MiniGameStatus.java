package model.miniGame;

/** Read-only row rendered on the Travel Log minigames page. */
public record MiniGameStatus(
        MiniGameDefinition definition,
        MiniGameProgress progress
) {
}
