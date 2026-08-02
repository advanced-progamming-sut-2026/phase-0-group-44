package service;

import model.Result;
import model.Store;
import model.enums.MenuName;
import model.scored.ScoredGameSession;
import model.user.User;
import util.SeededRandomSource;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Owns the current scored-game attempt and persists only the user's best result. */
public final class ScoredGameService {
    private final UserService users;
    private final Clock clock;

    public ScoredGameService(UserService users, Clock clock) {
        if (users == null || clock == null) {
            throw new IllegalArgumentException("User service and clock are required.");
        }
        this.users = users;
        this.clock = clock;
    }

    public Result<String> open(User user) {
        Result<String> result = new Result<>();
        if (user == null) {
            result.appendToMessage("no user is logged in");
            return result;
        }
        Store.setCurrentMenu(MenuName.SCORED_GAME);
        result.setStatus(true);
        result.setData("scored-game");
        result.appendToMessage("entered scored-game menu; use scored game start");
        return result;
    }

    public Result<String> cheatAddSuns(User user, int count) {
        Result<String> result = new Result<>();
        ScoredGameSession session = ownedRunning(user, result);
        if (session == null) return result;
        return session.cheatAddSun(count);
    }

    public Result<List<String>> cheatReleaseNuke(User user) {
        Result<List<String>> result = new Result<>();
        ScoredGameSession session = ownedRunning(user, result);
        if (session == null) return result;
        Result<List<String>> nuked = session.cheatReleaseNuke();
        if (nuked.getStatus()) {
            Result<List<String>> advanced = session.advance(1);
            List<String> combined = new ArrayList<>(nuked.getData());
            if (advanced.getStatus()) {
                combined.addAll(advanced.getData());
            }
            nuked.setData(combined);
            settleIfTerminal(user, session);
        }
        return nuked;
    }

    public Result<String> start(User user) {
        Result<String> result = new Result<>();
        if (user == null) {
            result.appendToMessage("no user is logged in");
            return result;
        }
        ScoredGameSession active = Store.getActiveScoredGameSession();
        if (active != null && active.getState() == ScoredGameSession.State.RUNNING) {
            result.appendToMessage("finish the running scored game first");
            return result;
        }
        LocalDate today = LocalDate.now(clock);
        ScoredGameSession session = new ScoredGameSession(
                user, today, new SeededRandomSource(seedForAttempt(today)));
        Store.setActiveScoredGameSession(session);
        return session.start();
    }

    public Result<List<String>> advance(User user, int ticks) {
        Result<List<String>> result = new Result<>();
        ScoredGameSession session = ownedRunning(user, result);
        if (session == null) return result;
        Result<List<String>> advanced = session.advance(ticks);
        if (advanced.getStatus()) settleIfTerminal(user, session);
        return advanced;
    }

    public Result<String> plant(User user, String type, int x, int y) {
        Result<String> result = new Result<>();
        ScoredGameSession session = ownedRunning(user, result);
        if (session == null) return result;
        Result<String> planted = session.plant(type, x, y);
        if (planted.getStatus()) settleIfTerminal(user, session);
        return planted;
    }

    public Result<String> collectSun(User user, int x, int y) {
        Result<String> result = new Result<>();
        ScoredGameSession session = ownedRunning(user, result);
        if (session == null) return result;
        return session.collectSun(x, y);
    }

    public Result<String> status(User user) {
        Result<String> result = new Result<>();
        ScoredGameSession session = ownedAny(user, result);
        if (session == null) return result;
        result.setStatus(true);
        result.setData(session.status());
        result.appendToMessage(session.status());
        return result;
    }

    public Result<String> forfeit(User user) {
        Result<String> result = new Result<>();
        ScoredGameSession session = ownedRunning(user, result);
        if (session == null) return result;
        Result<String> forfeited = session.forfeit();
        settleIfTerminal(user, session);
        return forfeited;
    }

    private <T> ScoredGameSession ownedRunning(User user, Result<T> result) {
        ScoredGameSession session = ownedAny(user, result);
        if (session != null && session.getState() != ScoredGameSession.State.RUNNING) {
            result.appendToMessage("no scored game is running");
            return null;
        }
        return session;
    }

    private <T> ScoredGameSession ownedAny(User user, Result<T> result) {
        if (user == null) {
            result.appendToMessage("no user is logged in");
            return null;
        }
        ScoredGameSession session = Store.getActiveScoredGameSession();
        if (session == null) {
            result.appendToMessage("no scored game has been started");
            return null;
        }
        if (!user.getUsername().equals(session.getOwnerUsername())) {
            result.appendToMessage("the scored game belongs to another user");
            return null;
        }
        return session;
    }

    public boolean recordHighestScore(User user, int finalScore) {
        if (user == null || finalScore < 0 || finalScore <= user.getHighestMewPoint()) {
            return false;
        }
        user.setHighestMewPoint(finalScore);
        users.updateUser(user);
        return true;
    }

    private void settleIfTerminal(User user, ScoredGameSession session) {
        if (!session.isTerminal() || !session.markSettled()) return;
        recordHighestScore(user, session.getScore().total());
    }

    private long seedForAttempt(LocalDate date) {
        return 31L * date.toEpochDay() + 0x5C0EEDL;
    }
}
