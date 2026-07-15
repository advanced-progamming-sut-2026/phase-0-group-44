package model.enums;

/** The two gender values the project accepts, with the tokens typed on the command line. */
public enum Gender {
    MALE("male"),
    FEMALE("female");

    private final String token;

    Gender(String token) {
        this.token = token;
    }

    public String getToken() {
        return token;
    }

    /** @return the gender, or {@code null} when the token is not one of the two accepted values */
    public static Gender fromToken(String input) {
        if (input == null) {
            return null;
        }

        String normalized = input.trim().toLowerCase();

        for (Gender gender : values()) {
            if (gender.token.equals(normalized)) {
                return gender;
            }
        }

        return null;
    }
}
