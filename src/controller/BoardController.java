package controller;

import model.GameEngine;
import model.Position;
import model.Result;
import model.Tile;
import model.enums.*;
import model.events.DomainEventType;
import model.inGame.GameMap;
import model.inGame.PlantSelection;
import model.inGame.GameSession;
import model.inGame.plant.Plant;
import model.inGame.plant.PlantDefinition;
import model.inGame.plant.PlantRegistry;
import model.inGame.zombie.Zombie;
import model.sim.SimulationWorld;
import model.sim.adventure.AdventureRuntimeState;
import model.level.SpecialLevelType;
import model.sim.board.Board;
import model.sim.board.PlantInstance;
import model.sim.board.PlantSpec;
import model.sim.board.PlantSpecSource;
import model.sim.sun.SunProducer;
import model.user.User;
import service.DomainEventPublisher;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The board commands: planting, plucking, the cooldown cheat, feeding plant
 * food, and the three status reports.
 *
 * <p>Planting validates in order — plant is selected, tile permits it, stacking
 * is supported, not on cooldown, enough sun — and deducts sun only after every
 * check passes.</p>
 */
public class BoardController {

    private final GameEngine engine;
    private final PlantSelection selection;
    private final AdventureRuntimeState adventureState;
    private final DomainEventPublisher events;
    private final User user;
    private final GameSession session;

    public BoardController(GameEngine engine, PlantSelection selection,
                           AdventureRuntimeState adventureState) {
        this(engine, selection, adventureState, null, null, null);
    }

    public BoardController(GameEngine engine, PlantSelection selection,
                           AdventureRuntimeState adventureState,
                           DomainEventPublisher events, User user, GameSession session) {
        this.engine = engine;
        this.selection = selection;
        this.adventureState = adventureState;
        this.events = events;
        this.user = user;
        this.session = session;
    }

    @SuppressWarnings("PMD.ExcessiveMethodLength")
    public Result<String> plantPlant(PlantType type, int x, int y) {
        Result<String> result = new Result<>();

        if (type == null) {
            result.appendToMessage("unknown plant");
            return result;
        }

        boolean conveyor = isConveyorLevel();
        if (!conveyor && selection != null && !selection.contains(type)) {
            result.appendToMessage("this plant is not selected for the level");
            return result;
        }
        if (conveyor && adventureState.getConveyorPacketCount(type) <= 0) {
            result.appendToMessage("no conveyor packet for this plant");
            return result;
        }

        Position position;
        try {
            position = new Position(y, x); // (x,y) ورودی = (column,row) → Position(row, column)
        } catch (IllegalArgumentException e) {
            result.appendToMessage("invalid tile");
            return result;
        }
        if (!engine.getGameMap().isInside(position)) {
            result.appendToMessage("invalid tile");
            return result;
        }

        // Pea Pod stacking: چک کن آیا از قبل یک Pea Pod روی این تایل هست
        Plant existingPrimary = engine.getGameMap().getTile(position).getPrimaryPlant();
        if (type == PlantType.PEA_POD && existingPrimary != null
                && existingPrimary.getEffectiveType() == PlantType.PEA_POD) {
            return stackPeaPod(result, existingPrimary, conveyor, type, x, y, position);
        }

        boolean freePreWave = isFreePreWavePlanting();
        Plant plant;
        try {
            plant = engine.plant(type, 1, position, !conveyor, !conveyor && !freePreWave);
        } catch (IllegalStateException e) {
            result.appendToMessage(translatePlantError(e.getMessage()));
            return result;
        }

        if (conveyor) {
            adventureState.consumeConveyorPacket(type);
        }
        if (session != null) {
            session.recordPlantUsed(type);
        }
        publishPlantEvent(type, x, y);

        result.setStatus(true);
        result.setData(type.name());
        result.appendToMessage("planted " + type.name() + " at (" + x + ", " + y + ")");
        return result;
    }

    private Result<String> stackPeaPod(Result<String> result, Plant existing, boolean conveyor,
                                       PlantType type, int x, int y, Position position) {
        int heads = existing.getState("PEA_POD_HEADS", Integer.class, 1);
        if (heads >= 5) {
            result.appendToMessage("pea pod already has five heads");
            return result;
        }
        if (!conveyor && engine.isOnCooldown(type)) {
            result.appendToMessage("this plant is still recharging");
            return result;
        }
        int cost = PlantRegistry.getDefault().require(type).statsAtLevel(1).getCost();
        if (!conveyor && engine.getSun() < cost) {
            result.appendToMessage("not enough sun");
            return result;
        }
        if (conveyor) {
            adventureState.consumeConveyorPacket(type);
        } else {
            engine.removeSun(cost);
        }
        existing.putState("PEA_POD_HEADS", heads + 1);
        if (session != null) {
            session.recordPlantUsed(type);
        }
        publishPlantEvent(type, x, y);
        result.setStatus(true);
        result.setData(type.name());
        result.appendToMessage("stacked PEA_POD head " + (heads + 1) + " at (" + x + ", " + y + ")");
        return result;
    }

    private String translatePlantError(String message) {
        if (message == null) return "this tile does not permit that plant";
        if (message.contains("recharging")) return "this plant is still recharging";
        if (message.contains("sun")) return "not enough sun";
        if (message.contains("five heads")) return "pea pod already has five heads";
        return "this tile does not permit that plant";
    }

    public Result<String> pluckPlant(int x, int y) {
        Result<String> result = new Result<>();
        Position position;
        try {
            position = new Position(y, x);
        } catch (IllegalArgumentException e) {
            result.appendToMessage("invalid tile");
            return result;
        }
        if (!engine.getGameMap().isInside(position)) {
            result.appendToMessage("invalid tile");
            return result;
        }
        Tile tile = engine.getGameMap().getTile(position);
        if (!hasAnyPlant(tile)) {
            result.appendToMessage("no plant to pluck here");
            return result;
        }
        if (adventureState != null && adventureState.isProtected(x, y)) {
            result.appendToMessage("protected plants cannot be plucked");
            return result;
        }

        Plant removed = tile.getArmorPlant() != null ? tile.getArmorPlant()
                : tile.getPrimaryPlant() != null ? tile.getPrimaryPlant() : tile.getSupportPlant();
        engine.getGameMap().removePlant(removed);

        result.setStatus(true);
        result.setData(removed.getType().name());
        result.appendToMessage("plucked " + removed.getType().name() + " at (" + x + ", " + y + ")");
        return result;
    }

    public Result<String> cheatRemoveCooldown() {
        Result<String> result = new Result<>();
        engine.disableCooldowns();
        result.setStatus(true);
        result.appendToMessage("plant cooldowns disabled for this game");
        return result;
    }

    public Result<String> feedPlant(int x, int y) {
        Result<String> result = new Result<>();
        Position position;
        try {
            position = new Position(y, x);
        } catch (IllegalArgumentException e) {
            result.appendToMessage("invalid tile");
            return result;
        }
        if (!engine.getGameMap().isInside(position)) {
            result.appendToMessage("invalid tile");
            return result;
        }
        Tile tile = engine.getGameMap().getTile(position);
        Plant plant = tile.getPrimaryPlant() != null ? tile.getPrimaryPlant() : tile.getSupportPlant();

        if (plant == null) {
            result.appendToMessage("no plant to feed here");
            return result;
        }
        String effect = plant.getEffectiveDefinition().getPlantFoodEffect();
        if (effect == null || effect.isBlank() || effect.equalsIgnoreCase("none")) {
            result.appendToMessage("this plant has no plant-food effect");
            return result;
        }
        if (engine.getPlantFood() <= 0) {
            result.appendToMessage("you have no plant food");
            return result;
        }

        engine.spendPlantFood();
        plant.usePlantFood(engine); // ← حالا واقعاً اثر گیاه اجرا می‌شه، نه فقط ثبتش

        result.setStatus(true);
        result.setData(plant.getType().name());
        result.appendToMessage("fed " + plant.getType().name() + " at (" + x + ", " + y + ")");
        return result;
    }

    public Result<String> cheatAddPlantFood() {
        Result<String> result = new Result<>();
        boolean added = engine.addPlantFood();
        result.setStatus(true);
        result.setData(String.valueOf(engine.getPlantFood()));
        if (!added) {
            result.appendToMessage("plant food is already at the maximum (" + GameEngine.MAX_PLANT_FOOD + ")");
            return result;
        }
        result.appendToMessage("plant food: " + engine.getPlantFood());
        return result;
    }

    public Result<String> showMap() {
        Result<String> result = new Result<>();
        GameMap map = engine.getGameMap();
        StringBuilder builder = new StringBuilder();

        builder.append("wave: ").append(engine.getCurrentWave()).append('\n');
        builder.append("plant food: ").append(engine.getPlantFood()).append('\n');
        builder.append("sun: ").append(engine.getSun()).append('\n');

        for (int row = 0; row < map.getRows(); row++) {
            builder.append("row ").append(row)
                    .append(" [mower ").append(engine.isLawnMowerUsed(row) ? "used" : "ready").append("]: ");
            for (int column = 0; column < map.getColumns(); column++) {
                builder.append(cellSymbol(map.getTile(row, column))).append(' ');
            }
            builder.append('\n');
        }
        appendZombiePositions(builder);

        result.setStatus(true);
        result.setData(builder.toString());
        result.appendToMessage(builder.toString().trim());
        return result;
    }

    public Result<String> showPlantsStatus() {
        Result<String> result = new Result<>();
        StringBuilder builder = new StringBuilder();
        GameMap map = engine.getGameMap();
        for (int row = 0; row < map.getRows(); row++) {
            for (int column = 0; column < map.getColumns(); column++) {
                Tile tile = map.getTile(row, column);
                appendPlantStatus(builder, tile.getSupportPlant(), column, row);
                appendPlantStatus(builder, tile.getPrimaryPlant(), column, row);
                appendPlantStatus(builder, tile.getArmorPlant(), column, row);
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

    public Result<String> showTileStatus(int x, int y) {
        Result<String> result = new Result<>();
        Position position;
        try {
            position = new Position(y, x);
        } catch (IllegalArgumentException e) {
            result.appendToMessage("invalid tile");
            return result;
        }
        if (!engine.getGameMap().isInside(position)) {
            result.appendToMessage("invalid tile");
            return result;
        }
        Tile tile = engine.getGameMap().getTile(position);
        StringBuilder builder = new StringBuilder();
        builder.append("tile (").append(x).append(", ").append(y).append(")\n");
        builder.append("terrain: ").append(tile.getTerrain());
        if (tile.getObstacle() == ObstacleType.GRAVE) {
            String payload = tile.getObstaclePayload();
            builder.append(" (health ").append(tile.getObstacleHealth()).append(')');
            if (!payload.isBlank()) builder.append(" [reward: ").append(payload).append(']');
        }
        if (tile.getObstacle() == ObstacleType.ICE) {
            builder.append(" [frozen, ice ").append(tile.getObstacleHealth()).append(']');
        }
        builder.append('\n');
        appendTilePlant(builder, "support", tile.getSupportPlant());
        appendTilePlant(builder, "primary", tile.getPrimaryPlant());
        appendTilePlant(builder, "armor", tile.getArmorPlant());
        result.setStatus(true);
        result.setData(builder.toString());
        result.appendToMessage(builder.toString().trim());
        return result;
    }

    private void publishPlantEvent(PlantType type, int x, int y) {
        if (events == null || user == null) return;
        PlantDefinition definition = PlantRegistry.getDefault().findByType(type);
        Map<String, String> attributes = new LinkedHashMap<>();
        attributes.put("plant", type.name());
        attributes.put("column", String.valueOf(x));
        attributes.put("row", String.valueOf(y));
        if (definition != null) {
            PlantCategory category = definition.getBehaviorCategory();
            attributes.put("family", category == null ? "" : category.name());
            attributes.put("explosive", String.valueOf(
                    category == PlantCategory.EXPLOSIVE || definition.hasTag(PlantTag.EXPLOSIVE)));
            attributes.put("sunProducer", String.valueOf(
                    category == PlantCategory.SUN_PRODUCER || definition.hasTag(PlantTag.SUN)));
        }
        events.publish(DomainEventType.PLANT_PLANTED, user, attributes);
    }

    private boolean isConveyorLevel() {
        return adventureState != null
                && adventureState.getConfig().getSpecialType() == SpecialLevelType.CONVEYOR_BELT;
    }

    private boolean isFreePreWavePlanting() {
        return adventureState != null
                && adventureState.getConfig().getSpecialType() == SpecialLevelType.PLANT_WHAT_YOU_GET
                && !engine.areWavesStarted();
    }

    private boolean hasAnyPlant(Tile tile) {
        return tile.getSupportPlant() != null || tile.getPrimaryPlant() != null || tile.getArmorPlant() != null;
    }

    private String cellSymbol(Tile tile) {
        if (hasAnyPlant(tile)) {
            return "P";
        }
        if (tile.getObstacle() == ObstacleType.ICE) {
            return "*";
        }
        if (tile.getObstacle() == ObstacleType.GRAVE) {
            return "#"; // پاداش (SUN_50/PLANT_FOOD) در payload نگه‌داری می‌شه، نه در نماد
        }
        return switch (tile.getTerrain()) {
            case WATER -> "~";
            case SLIPPERY_UP -> "^";
            case SLIPPERY_DOWN -> "v";
            case LOW_TIDE -> "_";
            default -> ".";
        };
    }

    private void appendZombiePositions(StringBuilder builder) {
        List<Zombie> zombies = engine.getZombies();
        if (zombies.isEmpty()) return;
        builder.append("zombies:");
        for (Zombie zombie : zombies) {
            builder.append(" (").append(zombie.getColumn()).append(", ").append(zombie.getRow()).append(')');
        }
        builder.append('\n');
    }

    private void appendPlantStatus(StringBuilder builder, Plant plant, int column, int row) {
        if (plant == null) return;
        double cooldown = engine.getCooldown(plant.getType());
        builder.append(plant.getType().name())
                .append(" at (").append(column).append(", ").append(row).append(")")
                .append(" | sun cost ").append(plant.getCost())
                .append(" | ").append(engine.isOnCooldown(plant.getType())
                        ? "recharging, " + cooldown + "s left" : "plantable")
                .append('\n');
    }

    private void appendTilePlant(StringBuilder builder, String slot, Plant plant) {
        if (plant == null) return;
        builder.append(slot).append(" plant: ").append(plant.getType().name())
                .append(" (health ").append(plant.getHp()).append(")\n");
    }
}