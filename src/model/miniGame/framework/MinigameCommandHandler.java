package model.miniGame.framework;

public interface MinigameCommandHandler {
    boolean supports(String command);
    String execute(MinigameSession session, String command);
}
