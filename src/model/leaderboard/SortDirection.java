package model.leaderboard;

public enum SortDirection {
    ASC, DESC;

    public static SortDirection fromToken(String input) {
        if (input == null || input.isBlank()) return DESC;
        if ("asc".equalsIgnoreCase(input) || "ascending".equalsIgnoreCase(input)) return ASC;
        if ("desc".equalsIgnoreCase(input) || "descending".equalsIgnoreCase(input)) return DESC;
        return null;
    }
}
