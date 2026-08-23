package screen.gameplay;

public enum PlantAnimationTier {
    INITIAL("768/INITIAL/PLANT/"),
    FULL("768/FULL/PLANT/"),
    MINT("768/INITIAL/EMPOWERMINTS/PLANT/");

    private final String root;

    PlantAnimationTier(String root) {
        this.root = root;
    }

    public String root() {
        return root;
    }
}

