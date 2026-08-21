package screen.gameplay;

public enum PlantAnimationTier {
    INITIAL("768/INITIAL/PLANT/"),
    FULL("768/FULL/PLANT/");

    private final String root;

    PlantAnimationTier(String root) {
        this.root = root;
    }

    public String root() {
        return root;
    }
}

