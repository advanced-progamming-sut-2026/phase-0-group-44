package controller;

import model.Result;
import model.Store;
import service.ScoredGameService;

import java.util.List;

/** Command-facing adapter for the daily scored game. */
public final class ScoredGameController {
    private final ScoredGameService service;

    public ScoredGameController(ScoredGameService service) {
        if (service == null) throw new IllegalArgumentException("Scored-game service is required.");
        this.service = service;
    }

    public Result<String> open() { return service.open(Store.getLoggedInUser()); }
    public Result<String> start() { return service.start(Store.getLoggedInUser()); }
    public Result<List<String>> advance(int ticks) { return service.advance(Store.getLoggedInUser(), ticks); }
    public Result<String> plant(String type, int x, int y) {
        return service.plant(Store.getLoggedInUser(), type, x, y);
    }
    public Result<String> collectSun(int x, int y) {
        return service.collectSun(Store.getLoggedInUser(), x, y);
    }
    public Result<String> status() { return service.status(Store.getLoggedInUser()); }
    public Result<String> forfeit() { return service.forfeit(Store.getLoggedInUser()); }
    public Result<String> cheatAddSuns(int count) {
        return service.cheatAddSuns(Store.getLoggedInUser(), count);
    }

    public Result<List<String>> cheatReleaseNuke() {
        return service.cheatReleaseNuke(Store.getLoggedInUser());
    }
}
