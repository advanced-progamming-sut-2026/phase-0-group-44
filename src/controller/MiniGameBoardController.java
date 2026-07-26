package controller;

import model.Result;
import model.enums.PlantType;
import model.enums.TerrainType;
import model.inGame.PlantSelection;
import model.sim.SimulationWorld;
import model.sim.board.Board;
import model.sim.board.PlantInstance;
import model.sim.board.PlantSpec;
import model.sim.board.PlantSpecSource;
import model.sim.board.Tile;

/**
 * The board commands: planting, plucking, the cooldown cheat, feeding plant
 * food, and the three status reports.
 *
 * <p>Planting validates in order — plant is selected, tile permits it, stacking
 * is supported, not on cooldown, enough sun — and deducts sun only after every
 * check passes.</p>
 */
public class MiniGameBoardController {

    private final SimulationWorld world;
    private final PlantSelection selection;
    private final PlantSpecSource specSource;

    public MiniGameBoardController(
            SimulationWorld world,
            PlantSelection selection,
            PlantSpecSource specSource
    ) {
        this.world = world;
        this.selection = selection;
        this.specSource = specSource;
    }

    /** Handles {@code plant plant -t <type> -l (<x>, <y>)}. */
    public Result<String> plantPlant(PlantType type, int x, int y) {
        Result<String> result = new Result<>();

        if (type == null) {
            result.appendToMessage("unknown plant");
            return result;
        }

        if (selection != null && !selection.contains(type)) {
            result.appendToMessage("this plant is not selected for the level");
            return result;
        }

        PlantSpec spec = specSource.specOf(type);

        if (spec == null) {
            result.appendToMessage("no data for this plant");
            return result;
        }

        Board board = world.getBoard();
        Tile tile = board.tileAt(x, y);

        if (tile == null) {
            result.appendToMessage("invalid tile");
            return result;
        }

        Placement placement = placementFor(tile, spec);

        if (placement == Placement.REJECTED) {
            result.appendToMessage("this tile does not permit that plant");
            return result;
        }

        if (world.isOnCooldown(type)) {
            result.appendToMessage("this plant is still recharging");
            return result;
        }

        if (world.getSunBalance() < spec.getSunCost()) {
            result.appendToMessage("not enough sun");
            return result;
        }

        // All validation passed: only now is sun deducted and the plant placed.
        world.addSun(-spec.getSunCost());
        PlantInstance plant = new PlantInstance(spec, x, y);
        place(tile, plant, placement);
        world.addPlant(plant);
        world.startCooldown(type, spec.getRechargeTicks());

        result.setStatus(true);
        result.setData(type.name());
        result.appendToMessage("planted " + type.name() + " at (" + x + ", " + y + ")");

        return result;
    }

    /** Handles {@code pluck plant -l (<x>, <y>)}. */
    public Result<String> pluckPlant(int x, int y) {
        Result<String> result = new Result<>();
        Tile tile = world.getBoard().tileAt(x, y);

        if (tile == null) {
            result.appendToMessage("invalid tile");
            return result;
        }

        if (!tile.hasAnyPlant()) {
            result.appendToMessage("no plant to pluck here");
            return result;
        }

        // Remove the stacked plant first, then the support, matching placement order.
        PlantInstance removed = tile.getStackedPlant() != null
                ? tile.getStackedPlant() : tile.getSupportPlant();

        if (tile.getStackedPlant() != null) {
            tile.setStackedPlant(null);
        } else {
            tile.clearPlants();
        }

        world.getPlants().remove(removed);

        result.setStatus(true);
        result.setData(removed.getType().name());
        result.appendToMessage("plucked " + removed.getType().name() + " at (" + x + ", " + y + ")");

        return result;
    }

    /** Handles {@code cheat remove-cooldown}. */
    public Result<String> cheatRemoveCooldown() {
        Result<String> result = new Result<>();
        world.disableCooldowns();

        result.setStatus(true);
        result.appendToMessage("plant cooldowns disabled for this game");

        return result;
    }

    /** Handles {@code feed plant -l (<x>, <y>)}. */
    public Result<String> feedPlant(int x, int y) {
        Result<String> result = new Result<>();
        Tile tile = world.getBoard().tileAt(x, y);

        if (tile == null) {
            result.appendToMessage("invalid tile");
            return result;
        }

        PlantInstance plant = tile.getStackedPlant() != null
                ? tile.getStackedPlant() : tile.getSupportPlant();

        if (plant == null) {
            result.appendToMessage("no plant to feed here");
            return result;
        }

        if (!plant.getSpec().hasPlantFoodEffect()) {
            result.appendToMessage("this plant has no plant-food effect");
            return result;
        }

        if (world.getPlantFood() <= 0) {
            result.appendToMessage("you have no plant food");
            return result;
        }

        world.spendPlantFood();
        // Applying the plant's specific plant-food effect belongs to the plant
        // behaviour layer; this records that it fires.

        result.setStatus(true);
        result.setData(plant.getType().name());
        result.appendToMessage("fed " + plant.getType().name() + " at (" + x + ", " + y + ")");

        return result;
    }

    /** Handles {@code cheat add-plant-food}. */
    public Result<String> cheatAddPlantFood() {
        Result<String> result = new Result<>();
        boolean added = world.addPlantFood();

        result.setStatus(true);
        result.setData(String.valueOf(world.getPlantFood()));

        if (!added) {
            result.appendToMessage("plant food is already at the maximum ("
                    + SimulationWorld.MAX_PLANT_FOOD + ")");
            return result;
        }

        result.appendToMessage("plant food: " + world.getPlantFood());

        return result;
    }

    /** Handles {@code show map}. */
    public Result<String> showMap() {
        Result<String> result = new Result<>();
        Board board = world.getBoard();
        StringBuilder builder = new StringBuilder();

        builder.append("wave: ").append(world.getCurrentWave()).append('\n');
        builder.append("plant food: ").append(world.getPlantFood()).append('\n');
        builder.append("sun: ").append(world.getSunBalance()).append('\n');

        for (int row = 0; row < board.getRows(); row++) {
            builder.append("row ").append(row)
                    .append(" [mower ")
                    .append(world.isLawnMowerUsed(row) ? "used" : "ready")
                    .append("]: ");

            for (int column = 0; column < board.getColumns(); column++) {
                builder.append(cellSymbol(board.tileAt(column, row))).append(' ');
            }

            builder.append('\n');
        }

        appendZombiePositions(builder);

        result.setStatus(true);
        result.setData(builder.toString());
        result.appendToMessage(builder.toString().trim());

        return result;
    }

    /** Handles {@code show plants status}. */
    public Result<String> showPlantsStatus() {
        Result<String> result = new Result<>();
        Board board = world.getBoard();
        StringBuilder builder = new StringBuilder();

        for (int row = 0; row < board.getRows(); row++) {
            for (int column = 0; column < board.getColumns(); column++) {
                Tile tile = board.tileAt(column, row);
                appendPlantStatus(builder, tile.getSupportPlant());
                appendPlantStatus(builder, tile.getStackedPlant());
            }
        }

        result.setStatus(true);

        if (builder.length() == 0) {
            result.setData("");
            result.appendToMessage("no plants on the board");
            return result;
        }

        result.setData(builder.toString());
        result.appendToMessage(builder.toString().trim());

        return result;
    }

    /** Handles {@code show tile status -l (<x>, <y>)}. */
    public Result<String> showTileStatus(int x, int y) {
        Result<String> result = new Result<>();
        Tile tile = world.getBoard().tileAt(x, y);

        if (tile == null) {
            result.appendToMessage("invalid tile");
            return result;
        }

        StringBuilder builder = new StringBuilder();
        builder.append("tile (").append(x).append(", ").append(y).append(")\n");
        builder.append("terrain: ").append(tile.getTerrain());

        if (tile.isGravestone()) {
            builder.append(" (health ").append(tile.getTerrainHealth()).append(')');
        }

        if (tile.isFrozen()) {
            builder.append(" [frozen, ice ").append(tile.getIceHealth()).append(']');
        }

        builder.append('\n');
        appendTilePlant(builder, "support", tile.getSupportPlant());
        appendTilePlant(builder, "stacked", tile.getStackedPlant());

        result.setStatus(true);
        result.setData(builder.toString());
        result.appendToMessage(builder.toString().trim());

        return result;
    }

    private enum Placement {
        AS_SUPPORT,
        ON_SUPPORT,
        REJECTED
    }

    private Placement placementFor(Tile tile, PlantSpec spec) {
        if (tile.isFrozen()) {
            return Placement.REJECTED;
        }

        TerrainType terrain = tile.getTerrain();

        if (terrain.requiresWaterCapablePlant()) {
            return waterPlacement(tile, spec);
        }

        if (!terrain.isPlantableByDefault()) {
            return Placement.REJECTED;
        }

        return landPlacement(tile, spec);
    }

    private Placement waterPlacement(Tile tile, PlantSpec spec) {
        if (tile.getSupportPlant() != null) {
            if (tile.getSupportPlant().getSpec().providesSupport()
                    && spec.stacksOnSupport()
                    && tile.getStackedPlant() == null) {
                return Placement.ON_SUPPORT;
            }

            return Placement.REJECTED;
        }

        if (spec.isWaterCapable() || spec.providesSupport()) {
            return Placement.AS_SUPPORT;
        }

        return Placement.REJECTED;
    }

    private Placement landPlacement(Tile tile, PlantSpec spec) {
        if (tile.getSupportPlant() == null) {
            return Placement.AS_SUPPORT;
        }

        if (tile.getSupportPlant().getSpec().providesSupport()
                && spec.stacksOnSupport()
                && tile.getStackedPlant() == null) {
            return Placement.ON_SUPPORT;
        }

        return Placement.REJECTED;
    }

    private void place(Tile tile, PlantInstance plant, Placement placement) {
        if (placement == Placement.ON_SUPPORT) {
            plant.setStackedOnSupport(true);
            tile.setStackedPlant(plant);
        } else {
            tile.setSupportPlant(plant);
        }
    }

    private String cellSymbol(Tile tile) {
        if (tile.hasAnyPlant()) {
            return "P";
        }

        if (tile.isFrozen()) {
            return "*";
        }

        switch (tile.getTerrain()) {
            case WATER:
                return "~";
            case GRAVESTONE:
            case DARK_AGES_GRAVESTONE:
                return "#";
            case SLIPPERY_UP:
                return "^";
            case SLIPPERY_DOWN:
                return "v";
            case LOW_TIDE:
                return "_";
            default:
                return ".";
        }
    }

    private void appendZombiePositions(StringBuilder builder) {
        if (world.getZombies().isEmpty()) {
            return;
        }

        builder.append("zombies:");

        for (var zombie : world.getZombies()) {
            builder.append(" (").append(zombie.getTileX())
                    .append(", ").append(zombie.getTileY()).append(')');
        }

        builder.append('\n');
    }

    private void appendPlantStatus(StringBuilder builder, PlantInstance plant) {
        if (plant == null) {
            return;
        }

        PlantSpec spec = plant.getSpec();
        int cooldown = world.getCooldownRemaining(spec.getType());

        builder.append(spec.getType().name())
                .append(" at (").append(plant.getTileX()).append(", ")
                .append(plant.getTileY()).append(")")
                .append(" | sun cost ").append(spec.getSunCost())
                .append(" | ").append(world.isOnCooldown(spec.getType())
                        ? "recharging, " + cooldown + " ticks left" : "plantable")
                .append('\n');
    }

    private void appendTilePlant(StringBuilder builder, String slot, PlantInstance plant) {
        if (plant == null) {
            return;
        }

        builder.append(slot).append(" plant: ").append(plant.getType().name())
                .append(" (health ").append(plant.getHp()).append(")\n");
    }
}
