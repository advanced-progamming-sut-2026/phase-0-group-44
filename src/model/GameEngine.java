package model;

import model.enums.DamageType;
import model.enums.ObstacleType;
import model.enums.PlantCategory;
import model.enums.PlantType;
import model.enums.TerrainType;
import model.enums.ZombieType;
import model.inGame.GameMap;
import model.inGame.plant.Plant;
import model.inGame.plant.PlantDefinition;
import model.inGame.plant.PlantFactory;
import model.inGame.plant.PlantRegistry;
import model.inGame.projectile.FireEffect;
import model.inGame.projectile.Projectile;
import model.inGame.zombie.Zombie;
import model.inGame.zombie.ZombieFactory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

@SuppressWarnings("PMD.ExcessiveClassLength")
public class GameEngine {
    private final PlantRegistry plantRegistry;
    private final PlantFactory plantFactory;
    private final ZombieFactory zombieFactory;
    private final GameMap gameMap;
    private final List<Zombie> zombies = new ArrayList<>();
    private final List<Projectile> projectiles = new ArrayList<>();
    private final Map<PlantType, Double> seedCooldowns = new EnumMap<>(PlantType.class);
    private final Map<PlantCategory, Double> familyBoosts = new EnumMap<>(PlantCategory.class);
    private final List<String> events = new ArrayList<>();
    private final Map<Position, Integer> groundSuns = new LinkedHashMap<>();
    private final Random random;
    private int sun = 50;
    private double elapsedSeconds;

    public GameEngine() {
        this(PlantRegistry.getDefault(), new GameMap(), new Random(0));
    }

    public GameEngine(PlantRegistry registry, GameMap gameMap, Random random) {
        this.plantRegistry = registry;
        this.gameMap = gameMap;
        this.random = random == null ? new Random(0) : random;
        this.plantFactory = new PlantFactory(registry);
        this.zombieFactory = new ZombieFactory();
    }

    public Plant plant(PlantType type, int level, Position position) {
        Plant plant = plantFactory.create(type, level);
        if (getCooldown(type) > 0.0) {
            throw new IllegalStateException(type + " is recharging for " + getCooldown(type) + " more seconds.");
        }
        if (sun < plant.getCost()) {
            throw new IllegalStateException("Not enough sun.");
        }
        Plant stacked = stackPeaPod(type, position, plant);
        if (stacked != null) {
            return stacked;
        }
        sun -= plant.getCost();
        placePlantInternal(plant, position, true);
        return plant;
    }

    private Plant stackPeaPod(PlantType type, Position position, Plant newHead) {
        if (type != PlantType.PEA_POD || !gameMap.isInside(position)) {
            return null;
        }
        Plant existing = gameMap.getTile(position).getPrimaryPlant();
        if (existing == null || existing.getEffectiveType() != PlantType.PEA_POD) {
            return null;
        }
        int heads = existing.getState("PEA_POD_HEADS", Integer.class, 1);
        if (heads >= 5) {
            throw new IllegalStateException("Pea Pod already has five heads.");
        }
        sun -= newHead.getCost();
        existing.putState("PEA_POD_HEADS", heads + 1);
        seedCooldowns.put(type, newHead.getStats().getRecharge());
        recordEvent("Stacked Pea Pod head " + (heads + 1) + " at " + position + ".");
        return existing;
    }

    public Plant plantImitater(PlantType copiedType, int imitaterLevel, Position position) {
        Plant plant = plantFactory.createImitater(copiedType, imitaterLevel);
        if (getCooldown(PlantType.IMITATER) > 0.0) {
            throw new IllegalStateException("Imitater is recharging.");
        }
        if (sun < plant.getCost()) {
            throw new IllegalStateException("Not enough sun.");
        }
        sun -= plant.getCost();
        placePlantInternal(plant, position, true);
        return plant;
    }

    public void placePlantForFree(Plant plant, Position position) {
        placePlantInternal(plant, position, false);
    }

    private void placePlantInternal(Plant plant, Position position, boolean startCooldown) {
        gameMap.placePlant(plant, position);
        if (startCooldown) {
            seedCooldowns.put(plant.getType(), plant.getStats().getRecharge());
        }
        recordEvent("Planted " + plant.getType() + " at " + position + ".");
        plant.onPlant(this);
        cleanupDeadPlants();
    }

    public void tick() {
        tick(1.0);
    }

    public void tick(double deltaSeconds) {
        if (deltaSeconds <= 0.0) {
            throw new IllegalArgumentException("Tick duration must be positive.");
        }
        elapsedSeconds += deltaSeconds;
        updateDurations(seedCooldowns, deltaSeconds);
        updateDurations(familyBoosts, deltaSeconds);

        // Zombie status/special transitions happen before plant attacks so a
        // freshly applied effect keeps its full duration until the next update.
        for (Zombie zombie : new ArrayList<>(zombies)) {
            zombie.tick(this, deltaSeconds);
        }
        cleanupDeadZombies();

        for (Plant plant : new ArrayList<>(gameMap.getPlants())) {
            plant.tick(this, deltaSeconds);
        }
        for (Projectile projectile : new ArrayList<>(projectiles)) {
            projectile.tick(this, deltaSeconds);
        }
        projectiles.removeIf(projectile -> !projectile.isActive());
        cleanupDeadZombies();
        cleanupDeadPlants();
    }

    private <K> void updateDurations(Map<K, Double> durations, double deltaSeconds) {
        for (Map.Entry<K, Double> entry : new ArrayList<>(durations.entrySet())) {
            double remaining = Math.max(0.0, entry.getValue() - deltaSeconds);
            if (remaining <= 0.0) {
                durations.remove(entry.getKey());
            } else {
                durations.put(entry.getKey(), remaining);
            }
        }
    }


    private void cleanupDeadZombies() {
        for (Zombie zombie : new ArrayList<>(zombies)) {
            if (zombie.isDead()) {
                zombie.handleDeath(this);
                zombies.remove(zombie);
            }
        }
    }

    private void cleanupDeadPlants() {
        for (Plant plant : new ArrayList<>(gameMap.getPlants())) {
            if (plant.isDead()) {
                gameMap.removePlant(plant);
            }
        }
    }

    public void addZombie(Zombie zombie) {
        if (zombie == null || zombie.getRow() < 0 || zombie.getRow() >= gameMap.getRows()) {
            throw new IllegalArgumentException("Zombie is null or outside the map.");
        }
        zombies.add(zombie);
    }

    public Zombie spawnZombie(ZombieType type, int row, double x) {
        if (row < 0 || row >= gameMap.getRows() || x < 0.0 || x > gameMap.getColumns()) {
            throw new IllegalArgumentException("Zombie spawn position is outside the map.");
        }
        Zombie zombie = zombieFactory.create(type, row, x);
        addZombie(zombie);
        return zombie;
    }

    public void spawnProjectile(Projectile projectile) {
        if (projectile != null) {
            projectiles.add(projectile);
        }
    }

    public List<Zombie> getZombiesInLane(int row) {
        List<Zombie> result = new ArrayList<>();
        for (Zombie zombie : zombies) {
            if (!zombie.isDead() && !zombie.isHypnotized() && zombie.getRow() == row) {
                result.add(zombie);
            }
        }
        result.sort(Comparator.comparingDouble(Zombie::getX));
        return result;
    }

    public Zombie getFirstZombieAhead(Plant plant, double range) {
        if (plant == null || plant.getPosition() == null) {
            return null;
        }
        double sourceX = plant.getPosition().getColumn() + 0.5;
        for (Zombie zombie : getZombiesInLane(plant.getPosition().getRow())) {
            double distance = zombie.getX() - sourceX;
            if (distance >= 0.0 && distance <= range) {
                return zombie;
            }
        }
        return null;
    }

    public Zombie getFirstZombieBehind(Plant plant, double range) {
        if (plant == null || plant.getPosition() == null) {
            return null;
        }
        double sourceX = plant.getPosition().getColumn() + 0.5;
        Zombie result = null;
        for (Zombie zombie : getZombiesInLane(plant.getPosition().getRow())) {
            double distance = sourceX - zombie.getX();
            if (distance >= 0.0 && distance <= range) {
                result = zombie;
            }
        }
        return result;
    }

    public Zombie findNearestZombie(int preferredRow, double sourceX, boolean acrossBoard) {
        Zombie result = null;
        double best = Double.MAX_VALUE;
        for (Zombie zombie : zombies) {
            if (zombie.isDead() || zombie.isHypnotized() || (!acrossBoard && zombie.getRow() != preferredRow)) {
                continue;
            }
            double distance = Math.abs(zombie.getX() - sourceX)
                    + Math.abs(zombie.getRow() - preferredRow) * 2.0;
            if (distance < best) {
                result = zombie;
                best = distance;
            }
        }
        return result;
    }

    public Zombie findNearestMetalZombie(Position source, double range) {
        Zombie result = null;
        double best = Double.MAX_VALUE;
        for (Zombie zombie : zombies) {
            if (zombie.isDead() || zombie.isHypnotized() || !zombie.hasMetalArmor()) {
                continue;
            }
            double distance = Math.abs(zombie.getX() - source.getColumn())
                    + Math.abs(zombie.getRow() - source.getRow());
            if (distance <= range && distance < best) {
                result = zombie;
                best = distance;
            }
        }
        return result;
    }

    public List<Zombie> getZombiesInArea(Position center, int rowRadius, int columnRadius) {
        List<Zombie> result = new ArrayList<>();
        for (Zombie zombie : zombies) {
            if (!zombie.isDead() && !zombie.isHypnotized()
                    && Math.abs(zombie.getRow() - center.getRow()) <= rowRadius
                    && Math.abs(zombie.getColumn() - center.getColumn()) <= columnRadius) {
                result.add(zombie);
            }
        }
        return result;
    }

    public List<Zombie> getRandomZombies(int count) {
        List<Zombie> candidates = new ArrayList<>();
        for (Zombie zombie : zombies) {
            if (!zombie.isDead() && !zombie.isHypnotized()) {
                candidates.add(zombie);
            }
        }
        java.util.Collections.shuffle(candidates, random);
        return new ArrayList<>(candidates.subList(0, Math.min(count, candidates.size())));
    }

    public List<Zombie> getRandomWaterZombies(int count) {
        List<Zombie> candidates = new ArrayList<>();
        for (Zombie zombie : zombies) {
            int column = Math.max(0, Math.min(gameMap.getColumns() - 1, zombie.getColumn()));
            Position position = new Position(zombie.getRow(), column);
            if (!zombie.isDead() && !zombie.isHypnotized()
                    && gameMap.getTile(position).getTerrain() == model.enums.TerrainType.WATER) {
                candidates.add(zombie);
            }
        }
        java.util.Collections.shuffle(candidates, random);
        return new ArrayList<>(candidates.subList(0, Math.min(count, candidates.size())));
    }

    public List<Zombie> getZombiesCrossed(int row, double oldX, double newX, int direction,
                                          Set<Long> excludedIds) {
        List<Zombie> result = new ArrayList<>();
        double minimum = Math.min(oldX, newX);
        double maximum = Math.max(oldX, newX);
        for (Zombie zombie : getZombiesInLane(row)) {
            if (!excludedIds.contains(zombie.getId()) && zombie.getX() >= minimum && zombie.getX() <= maximum) {
                result.add(zombie);
            }
        }
        result.sort(Comparator.comparingDouble(Zombie::getX));
        if (direction < 0) {
            result.sort(Comparator.comparingDouble(Zombie::getX).reversed());
        }
        return result;
    }

    public Zombie findZombieAtLanding(int row, double landingX) {
        Zombie result = null;
        double best = 0.76;
        for (Zombie zombie : getZombiesInLane(row)) {
            double distance = Math.abs(zombie.getX() - landingX);
            if (distance < best) {
                result = zombie;
                best = distance;
            }
        }
        return result;
    }

    public Zombie findBounceTarget(Zombie from, int direction, Set<Long> excludedIds) {
        Zombie result = null;
        double best = Double.MAX_VALUE;
        for (Zombie zombie : zombies) {
            if (zombie.isDead() || zombie.isHypnotized() || excludedIds.contains(zombie.getId())) {
                continue;
            }
            if (Math.abs(zombie.getRow() - from.getRow()) > 1) {
                continue;
            }
            double forward = direction > 0 ? zombie.getX() - from.getX() : from.getX() - zombie.getX();
            if (forward < -0.25) {
                continue;
            }
            double distance = Math.abs(forward) + Math.abs(zombie.getRow() - from.getRow());
            if (distance < best) {
                best = distance;
                result = zombie;
            }
        }
        return result;
    }

    public int damageArea(Position center, int rowRadius, int columnRadius, int damage, DamageType type) {
        int hit = 0;
        for (Zombie zombie : getZombiesInArea(center, rowRadius, columnRadius)) {
            zombie.receiveDamage(damage, type, this);
            if (type == DamageType.FIRE) {
                zombie.onFireHit(this);
                zombie.thaw();
            } else if (type == DamageType.ICE) {
                zombie.onIceHit(this);
            }
            hit++;
        }
        return hit;
    }

    public int damageLane(int row, int damage, DamageType type) {
        int hit = 0;
        for (Zombie zombie : getZombiesInLane(row)) {
            zombie.receiveDamage(damage, type, this);
            if (type == DamageType.FIRE) {
                zombie.onFireHit(this);
                zombie.thaw();
            } else if (type == DamageType.ICE) {
                zombie.onIceHit(this);
            }
            hit++;
        }
        return hit;
    }

    public void freezeAll(double seconds) {
        for (Zombie zombie : zombies) {
            if (!zombie.isDead() && !zombie.isHypnotized()) {
                zombie.applyFreeze(seconds);
            }
        }
    }

    public void moveZombieToAdjacentLane(Zombie zombie) {
        if (zombie == null) {
            return;
        }
        int row = zombie.getRow();
        if (row > 0) {
            zombie.moveToRow(row - 1);
        } else if (row + 1 < gameMap.getRows()) {
            zombie.moveToRow(row + 1);
        }
    }

    public void transformProjectileAlongPath(Projectile projectile, double oldX, double newX) {
        if (!projectile.isPeaProjectile()) {
            return;
        }
        int start = Math.max(0, (int) Math.floor(Math.min(oldX, newX)));
        int end = Math.min(gameMap.getColumns() - 1, (int) Math.floor(Math.max(oldX, newX)));
        for (int column = start; column <= end; column++) {
            Tile tile = gameMap.getTile(projectile.getRow(), column);
            Plant torchwood = null;
            if (tile.getPrimaryPlant() != null && tile.getPrimaryPlant().getEffectiveType() == PlantType.TORCHWOOD) {
                torchwood = tile.getPrimaryPlant();
            }
            if (torchwood != null && projectile.markTransformedAt(column)) {
                double multiplier = torchwood.getBooleanState("BLUE_FLAME") ? 3.0 : 2.0;
                projectile.multiplyDamage(multiplier);
                projectile.setEffect(new FireEffect());
            }
        }
    }

    public Plant findCollidingPlant(Zombie zombie) {
        if (zombie == null || zombie.getRow() < 0 || zombie.getRow() >= gameMap.getRows()) {
            return null;
        }
        int current = Math.max(0, Math.min(gameMap.getColumns() - 1, zombie.getColumn()));
        Plant plant = gameMap.getPlantForZombieAttack(new Position(zombie.getRow(), current));
        if (plant != null && !plant.getBooleanState("TRANSFORMED")) {
            return plant;
        }
        int next = current + zombie.getDirection();
        if (next >= 0 && next < gameMap.getColumns()) {
            double distance = Math.abs(zombie.getX() - (next + 0.5));
            if (distance <= 0.55) {
                plant = gameMap.getPlantForZombieAttack(new Position(zombie.getRow(), next));
                if (plant != null && !plant.getBooleanState("TRANSFORMED")) {
                    return plant;
                }
            }
        }
        return null;
    }

    public Zombie findCollidingHypnotizedZombie(Zombie source) {
        if (source == null) {
            return null;
        }
        for (Zombie zombie : zombies) {
            if (zombie != source && zombie.isHypnotized() && !zombie.isDead()
                    && zombie.getRow() == source.getRow()
                    && Math.abs(zombie.getX() - source.getX()) <= 0.55) {
                return zombie;
            }
        }
        return null;
    }

    public Plant findPlantWithin(int row, double x, int direction, double range) {
        Plant best = null;
        double bestDistance = Double.MAX_VALUE;
        for (Plant plant : gameMap.getPlants()) {
            if (plant.isDead() || plant.getPosition() == null
                    || plant.getPosition().getRow() != row
                    || plant.getBooleanState("TRANSFORMED")) {
                continue;
            }
            double plantX = plant.getPosition().getColumn() + 0.5;
            double signed = direction < 0 ? x - plantX : plantX - x;
            if (signed >= 0.0 && signed <= range && signed < bestDistance) {
                bestDistance = signed;
                best = plant;
            }
        }
        return best;
    }

    public Plant findNearestPlantInLane(int row, double x) {
        Plant result = null;
        double best = Double.MAX_VALUE;
        for (Plant plant : gameMap.getPlants()) {
            if (plant.isDead() || plant.getPosition() == null
                    || plant.getPosition().getRow() != row) {
                continue;
            }
            double distance = Math.abs(plant.getPosition().getColumn() + 0.5 - x);
            if (distance < best) {
                best = distance;
                result = plant;
            }
        }
        return result;
    }

    public void destroyPlantsAhead(Zombie zombie, int tileCount) {
        int start = zombie.getColumn() + zombie.getDirection();
        for (int step = 0; step < tileCount; step++) {
            int column = start + step * zombie.getDirection();
            if (column < 0 || column >= gameMap.getColumns()) {
                continue;
            }
            Tile tile = gameMap.getTile(zombie.getRow(), column);
            for (Plant plant : new ArrayList<>(tile.getPlants())) {
                plant.receiveDamage(Integer.MAX_VALUE, this, zombie);
            }
        }
        cleanupDeadPlants();
    }

    public void addIceHit(Plant plant) {
        if (plant == null || plant.isDead() || plant.getPosition() == null) {
            return;
        }
        int hits = plant.getState("ICE_HITS", Integer.class, 0) + 1;
        plant.putState("ICE_HITS", hits);
        if (hits >= 3 && !plant.getBooleanState("FROZEN")) {
            plant.putState("FROZEN", true);
            placeObstacle(plant.getPosition(), ObstacleType.ICE, 600, "FROZEN_PLANT");
        }
    }

    public boolean applyOctopus(Plant plant) {
        if (plant == null || plant.isDead() || plant.getPosition() == null
                || plant.getBooleanState("OCTOPUSED")) {
            return false;
        }
        plant.putState("OCTOPUSED", true);
        placeObstacle(plant.getPosition(), ObstacleType.OCTOPUS, 600, "OCTOPUS_PLANT");
        return true;
    }

    public void placeObstacle(Position position, ObstacleType type, int health, String payload) {
        gameMap.setObstacle(position, type, health, payload);
    }

    public int damageObstacleAt(int row, int column, int damage, DamageType type) {
        if (row < 0 || row >= gameMap.getRows() || column < 0 || column >= gameMap.getColumns()) {
            return 0;
        }
        Tile tile = gameMap.getTile(row, column);
        if (tile.getObstacle() == ObstacleType.NONE) {
            return 0;
        }
        String payload = tile.getObstaclePayload();
        ObstacleType obstacle = tile.getObstacle();
        int dealt;
        if (type == DamageType.FIRE && obstacle == ObstacleType.ICE) {
            dealt = tile.getObstacleHealth();
            tile.damageObstacle(Integer.MAX_VALUE);
        } else {
            dealt = tile.damageObstacle(damage);
        }
        if (tile.isObstacleDestroyed()) {
            gameMap.clearObstacle(new Position(row, column));
            for (Plant plant : tile.getPlants()) {
                if ("FROZEN_PLANT".equals(payload)) {
                    plant.putState("FROZEN", false);
                    plant.putState("ICE_HITS", 0);
                } else if ("OCTOPUS_PLANT".equals(payload)) {
                    plant.putState("OCTOPUSED", false);
                }
            }
            if ("RELEASE_TWO_IMPS".equals(payload)) {
                for (int i = 0; i < 2; i++) {
                    addZombie(zombieFactory.create(ZombieType.IMP, row, column + 0.2 + i * 0.1));
                }
            }
        }
        return dealt;
    }

    public void damagePlantAt(Position position, int damage, Zombie attacker) {
        Plant target = gameMap.getPlantForZombieAttack(position);
        if (target != null && !target.getBooleanState("TRANSFORMED")) {
            target.receiveDamage(damage, this, attacker);
            cleanupDeadPlants();
        }
    }

    public boolean movePlant(Plant plant, Position destination) {
        if (plant == null || plant.getPosition() == null || !gameMap.isInside(destination)) {
            return false;
        }
        Position original = plant.getPosition();
        if (gameMap.getTile(destination).getPrimaryPlant() != null
                || gameMap.getTile(destination).blocksPlanting()) {
            return false;
        }
        gameMap.removePlant(plant);
        try {
            gameMap.placePlant(plant, destination);
            return true;
        } catch (IllegalStateException exception) {
            gameMap.placePlant(plant, original);
            return false;
        }
    }

    public boolean transformPlantToCat(Plant plant, Zombie wizard) {
        if (plant == null || wizard == null || plant.isDead()
                || plant.getBooleanState("TRANSFORMED")) {
            return false;
        }
        plant.putState("TRANSFORMED", true);
        plant.putState("TRANSFORMED_BY", wizard.getId());
        recordEvent("Wizard transformed " + plant.getEffectiveType() + " into a cat.");
        return true;
    }

    public void restoreWizardTransformations(long wizardId) {
        for (Plant plant : gameMap.getPlants()) {
            Long transformedBy = plant.getState("TRANSFORMED_BY", Long.class, -1L);
            if (transformedBy == wizardId) {
                plant.putState("TRANSFORMED", false);
                plant.putState("TRANSFORMED_BY", null);
                recordEvent(plant.getEffectiveType() + " returned from cat form.");
            }
        }
    }


    public void addFamilyBoost(PlantCategory category, double durationSeconds) {
        familyBoosts.put(category, Math.max(familyBoosts.getOrDefault(category, 0.0), durationSeconds));
    }

    public boolean hasFamilyBoost(PlantCategory category) {
        return familyBoosts.getOrDefault(category, 0.0) > 0.0;
    }

    public double familyDamageMultiplier(PlantCategory category) {
        return hasFamilyBoost(category) ? 1.5 : 1.0;
    }

    public void applyPlantFoodToFamily(PlantCategory category, Plant excluded) {
        for (Plant plant : new ArrayList<>(gameMap.getPlants())) {
            if (plant != excluded && plant.getCategory() == category && !plant.getEffectiveDefinition().isMint()) {
                plant.usePlantFood(this);
            }
        }
    }

    public void resetFamilyCooldowns(PlantCategory category) {
        for (PlantDefinition definition : plantRegistry.findByCategory(category, true)) {
            seedCooldowns.remove(definition.getType());
        }
    }

    public boolean clearIce(Position center, int radius) {
        boolean cleared = false;
        for (Position position : gameMap.positionsInArea(center, radius, radius)) {
            if (gameMap.getTile(position).getObstacle() == ObstacleType.ICE) {
                damageObstacleAt(position.getRow(), position.getColumn(), Integer.MAX_VALUE, DamageType.FIRE);
                cleared = true;
            }
        }
        return cleared;
    }

    public void clearIceInLane(int row) {
        for (int column = 0; column < gameMap.getColumns(); column++) {
            Position position = new Position(row, column);
            if (gameMap.getTile(position).getObstacle() == ObstacleType.ICE) {
                damageObstacleAt(row, column, Integer.MAX_VALUE, DamageType.FIRE);
            }
        }
    }

    public List<Position> emptyPositions(boolean waterOnly) {
        List<Position> result = new ArrayList<>();
        for (int row = 0; row < gameMap.getRows(); row++) {
            for (int column = 0; column < gameMap.getColumns(); column++) {
                Position position = new Position(row, column);
                Tile tile = gameMap.getTile(position);
                boolean water = tile.getTerrain() == model.enums.TerrainType.WATER;
                if (waterOnly != water || tile.getPrimaryPlant() != null || tile.blocksPlanting()) {
                    continue;
                }
                result.add(position);
            }
        }
        return result;
    }

    public void recordEvent(String event) {
        if (event != null && !event.isBlank()) {
            events.add(event);
        }
    }

    public void addSun(int amount) {
        if (amount > 0) {
            sun += amount;
            recordEvent("Produced " + amount + " sun.");
        }
    }

    public int removeSun(int amount) {
        int removed = Math.min(Math.max(0, amount), sun);
        sun -= removed;
        return removed;
    }

    public void addGroundSun(Position position, int amount) {
        if (position != null && amount > 0) {
            groundSuns.merge(position, amount, Integer::sum);
        }
    }

    public int stealNearestGroundSun(int row, double x) {
        Position best = null;
        double distance = Double.MAX_VALUE;
        for (Position position : groundSuns.keySet()) {
            if (position.getRow() == row) {
                double current = Math.abs(position.getColumn() + 0.5 - x);
                if (current < distance) {
                    distance = current;
                    best = position;
                }
            }
        }
        return best == null ? 0 : groundSuns.remove(best);
    }

    public Map<Position, Integer> getGroundSuns() {
        return Map.copyOf(groundSuns);
    }

    public void setSun(int sun) {
        this.sun = Math.max(0, sun);
    }

    public int getSun() {
        return sun;
    }

    public double getCooldown(PlantType type) {
        return seedCooldowns.getOrDefault(type, 0.0);
    }

    public double getElapsedSeconds() {
        return elapsedSeconds;
    }

    public PlantRegistry getPlantRegistry() {
        return plantRegistry;
    }

    public PlantFactory getPlantFactory() {
        return plantFactory;
    }

    public ZombieFactory getZombieFactory() {
        return zombieFactory;
    }

    public GameMap getGameMap() {
        return gameMap;
    }

    public List<Zombie> getZombies() {
        return List.copyOf(zombies);
    }

    public List<Projectile> getProjectiles() {
        return List.copyOf(projectiles);
    }

    public List<String> getEvents() {
        return List.copyOf(events);
    }

    public Random getRandom() {
        return random;
    }
}
