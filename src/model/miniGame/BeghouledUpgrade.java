package model.miniGame;

import model.enums.PlantType;

/** One attempt-local Beghouled conversion bought with match-generated sun. */
public record BeghouledUpgrade(PlantType from, PlantType to, int cost) {
    public BeghouledUpgrade {
        if (from == null || to == null || from == to || cost <= 0) {
            throw new IllegalArgumentException("Invalid Beghouled upgrade.");
        }
    }
}
