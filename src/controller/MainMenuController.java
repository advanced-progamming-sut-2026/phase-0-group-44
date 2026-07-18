package controller;

import model.Result;
import model.Store;
import model.enums.MenuName;
import model.user.User;
import model.utility.Leaderboard;
import model.utility.LeaderboardColumn;
import model.utility.SortDirection;
import service.LeaderboardService;
import service.UserService;

/** The main menu's own command: logging out. */
public class MainMenuController {

    private final UserService userService;
    private final LeaderboardService leaderboardService;

    public MainMenuController(UserService userService) {
        this(userService, new LeaderboardService(userService));
    }

    public MainMenuController(
            UserService userService,
            LeaderboardService leaderboardService
    ) {
        if (userService == null || leaderboardService == null) {
            throw new IllegalArgumentException("User and leaderboard services are required.");
        }
        this.userService = userService;
        this.leaderboardService = leaderboardService;
    }

    /** Handles {@code menu logout}: clears the session and returns to registration. */
    public Result<String> logout() {
        Result<String> result = new Result<>();
        User user = Store.getLoggedInUser();

        if (user == null) {
            result.appendToMessage("no user is logged in");
            return result;
        }

        user.setStayLoggedIn(false);
        userService.updateUser(user);

        Store.setLoggedInUser(null);
        Store.setCurrentMenu(MenuName.REGISTER);

        result.setStatus(true);
        result.setData("register menu");
        result.appendToMessage("logged out successfully");

        return result;
    }
    /** Compatibility route: the same leaderboard command is accepted from Main. */
    public Result<Leaderboard> leaderboard() {
        return leaderboard(LeaderboardColumn.PROGRESS, SortDirection.DESCENDING);
    }

    public Result<Leaderboard> leaderboard(String columnToken, String directionToken) {
        Result<Leaderboard> result = new Result<>();
        LeaderboardColumn column = LeaderboardColumn.fromToken(columnToken);
        if (column == null) {
            result.appendToMessage("unknown leaderboard column; use username, progress, "
                    + "minigames, daily-quests, non-daily-quests, or highest-score");
            return result;
        }
        SortDirection direction = SortDirection.fromToken(directionToken);
        if (direction == null) {
            result.appendToMessage("leaderboard direction must be asc or desc");
            return result;
        }
        return leaderboard(column, direction);
    }

    public Result<Leaderboard> leaderboard(
            LeaderboardColumn column,
            SortDirection direction
    ) {
        Result<Leaderboard> result = new Result<>();
        User user = requireUser(result);
        if (user == null) {
            return result;
        }
        Leaderboard leaderboard = leaderboardService.getLeaderboard(column, direction);
        result.setStatus(true);
        result.setData(leaderboard);
        result.appendToMessage(leaderboard.format());
        return result;
    }

    private <T> User requireUser(Result<T> result) {
        User user = Store.getLoggedInUser();
        if (user == null) {
            result.appendToMessage("no user is logged in");
        }
        return user;
    }

}
