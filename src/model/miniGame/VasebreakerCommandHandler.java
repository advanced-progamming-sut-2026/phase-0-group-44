package model.miniGame;

import model.miniGame.framework.MinigameCommandHandler;
import model.miniGame.framework.MinigameSession;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class VasebreakerCommandHandler implements MinigameCommandHandler {
    private static final Pattern BREAK = Pattern.compile("^break\\s+vase\\s+(\\d+)\\s+(\\d+)$", Pattern.CASE_INSENSITIVE);
    private static final Pattern PLANT = Pattern.compile("^plant\\s+packet\\s+(packet-\\d+)\\s+(\\d+)\\s+(\\d+)$", Pattern.CASE_INSENSITIVE);
    private final VasebreakerStrategy strategy;
    public VasebreakerCommandHandler(VasebreakerStrategy strategy) { this.strategy = strategy; }
    @Override public boolean supports(String command) {
        String value = command == null ? "" : command.trim();
        return BREAK.matcher(value).matches() || PLANT.matcher(value).matches() || value.equalsIgnoreCase("show vasebreaker");
    }
    @Override public String execute(MinigameSession session, String command) {
        String value = command.trim();
        long tick = session.getSimulation() == null ? 0 : session.getSimulation().getCurrentTick();
        Matcher broken = BREAK.matcher(value);
        if (broken.matches()) return strategy.getGame().breakVase(Integer.parseInt(broken.group(1)), Integer.parseInt(broken.group(2)), tick);
        Matcher planted = PLANT.matcher(value);
        if (planted.matches()) return strategy.getGame().plantPacket(planted.group(1), Integer.parseInt(planted.group(2)), Integer.parseInt(planted.group(3)), tick);
        VaseBreaker game = strategy.getGame();
        return "vases " + game.getVases().stream().filter(v -> !v.isBroken()).count() + ", packets " + game.getPackets().size() + ", threats " + game.getReleasedThreats();
    }
}
