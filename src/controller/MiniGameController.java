package controller;

import model.miniGame.framework.MinigameDefinition;
import model.miniGame.framework.MinigameId;
import model.miniGame.framework.MinigameStrategy;
import model.user.User;
import service.minigame.MinigameService;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MiniGameController {
    private static final Pattern SELECT = Pattern.compile("^minigame\\s+select\\s+(\\S+)\\s+level\\s+([1-3])$", Pattern.CASE_INSENSITIVE);
    private final MinigameService service;
    private final MinigameStrategy defaultStrategy;

    public MiniGameController(MinigameService service, MinigameStrategy defaultStrategy) {
        this.service = service;
        this.defaultStrategy = defaultStrategy;
    }

    public List<MinigameDefinition> travelLogPage(User user) { return service.travelLogPage(user); }

    public String execute(User user, String command) {
        String value = command == null ? "" : command.trim();
        Matcher matcher = SELECT.matcher(value);
        if (matcher.matches()) {
            MinigameId id = MinigameId.fromToken(matcher.group(1));
            if (id == null) throw new IllegalArgumentException("Unknown mandatory minigame.");
            int level = Integer.parseInt(matcher.group(2));
            service.select(user, id, level, defaultStrategy);
            return "selected " + id.getDisplayName() + " level " + level;
        }
        if (value.equalsIgnoreCase("minigame start")) { service.start(); return "minigame started"; }
        if (value.equalsIgnoreCase("minigame tick")) { service.tick(); return "minigame ticked"; }
        if (value.equalsIgnoreCase("minigame win")) { service.win(user); return "minigame won"; }
        if (value.equalsIgnoreCase("minigame lose")) { service.lose(user); return "minigame lost"; }
        if (value.toLowerCase().startsWith("minigame command ")) {
            return service.extensionCommand(value.substring("minigame command ".length()));
        }
        if (value.toLowerCase().startsWith("minigame break vase ")) {
            return service.extensionCommand(value.substring("minigame ".length()));
        }
        if (value.toLowerCase().startsWith("minigame plant packet ")) {
            return service.extensionCommand(value.substring("minigame ".length()));
        }
        if (value.equalsIgnoreCase("minigame show vasebreaker")) {
            return service.extensionCommand("show vasebreaker");
        }
        throw new IllegalArgumentException("Unknown minigame command.");
    }
}
