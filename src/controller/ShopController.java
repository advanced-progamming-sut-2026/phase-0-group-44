package controller;

import model.Result;
import model.Store;
import model.enums.PlantType;
import model.inGame.plant.PlantDefinition;
import model.inGame.plant.PlantRepository;
import model.miniGame.GreenHouse;
import model.shop.ShopItem;
import model.user.DailyShopState;
import model.user.PlantCard;
import model.user.User;
import service.UserService;
import util.RandomSource;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Handles the shop commands reachable from the greenhouse.
 *
 * <p>Every purchase is validated completely before any state changes; a
 * rejected command never charges or grants anything. The daily offer is
 * derived from the injected clock, so "today" and "midnight" follow whatever
 * clock the surrounding phase uses, and the offer identity is persisted on the
 * user so restarting the program neither rerolls the plant nor allows a second
 * purchase.</p>
 */
public class ShopController {

    private final PlantRepository plantRepository;
    private final UserService userService;
    private final RandomSource random;

    public ShopController(
            PlantRepository plantRepository,
            UserService userService,
            RandomSource random
    ) {
        if (plantRepository == null || userService == null || random == null) {
            throw new IllegalArgumentException(
                    "Plant repository, user service, and random source are required."
            );
        }
        this.plantRepository = plantRepository;
        this.userService = userService;
        this.random = random;
    }

    /** Handles {@code shop list}. */
    public Result<String> list() {
        Result<String> result = new Result<>();
        User user = requireUser(result);
        if (user == null) {
            return result;
        }

        ensureDailyOffer(user);

        StringBuilder output = new StringBuilder();
        for (ShopItem item : ShopItem.values()) {
            if (item == ShopItem.DAILY) {
                continue;
            }
            output.append(formatCatalogLine(item)).append('\n');
        }
        output.append(formatDailyLine(user));

        result.setStatus(true);
        result.setData(output.toString());
        result.appendToMessage(output.toString());
        return result;
    }

    /** Handles {@code shop daily}. */
    public Result<String> daily() {
        Result<String> result = new Result<>();
        User user = requireUser(result);
        if (user == null) {
            return result;
        }

        ensureDailyOffer(user);

        String line = formatDailyLine(user);
        result.setStatus(true);
        result.setData(line);
        result.appendToMessage(line);
        return result;
    }

    /** Handles {@code shop buy -i <item_id> -n <count> [-t <plant_type>]}. */
    public Result<String> buy(String itemToken, int count, String plantToken) {
        Result<String> result = new Result<>();
        User user = requireUser(result);
        if (user == null) {
            return result;
        }

        ensureDailyOffer(user);

        if (count <= 0) {
            result.appendToMessage("count must be a positive number");
            return result;
        }

        ShopItem item = ShopItem.fromId(itemToken);
        if (item == null) {
            result.appendToMessage("no shop item with id \"" + itemToken + "\"");
            return result;
        }

        switch (item) {
            case POT:
                return buyPots(result, user, count);
            case PLANT_FOOD:
                return buyPlantFood(result, user, count);
            case RANDOM_SEEDS:
                return buyRandomSeeds(result, user, count);
            case SELECTED_SEEDS:
                return buySelectedSeeds(result, user, count, plantToken);
            case COIN_PACK:
                return buyCoinPack(result, user, count);
            case DAILY:
                return buyDaily(result, user, count);
            default:
                result.appendToMessage("no shop item with id \"" + itemToken + "\"");
                return result;
        }
    }

    // ------------------------------------------------------------------
    // Individual items. Each method validates everything first and only
    // then mutates the user, so a purchase is all-or-nothing.
    // ------------------------------------------------------------------

    private Result<String> buyPots(Result<String> result, User user, int count) {
        GreenHouse greenHouse = user.getGreenHouse();
        greenHouse.applyDefaults();

        int owned = greenHouse.getSlotCount();
        if (owned + (long) count > GreenHouse.TOTAL_SLOTS) {
            result.appendToMessage(
                    "cannot own more than " + GreenHouse.TOTAL_SLOTS + " pots"
            );
            return result;
        }

        Long total = chargeableTotal(result, user, ShopItem.POT, count);
        if (total == null) {
            return result;
        }

        user.setCoins(user.getCoins() - total.intValue());
        for (int i = 0; i < count; i++) {
            greenHouse.unlockNextSlot();
        }
        userService.updateUser(user);

        return success(result, "purchased " + count + " pot" + plural(count)
                + "; pots: " + greenHouse.getSlotCount()
                + "; coins: " + user.getCoins());
    }

    private Result<String> buyPlantFood(Result<String> result, User user, int count) {
        if (user.getPlantFood() + (long) count > ShopItem.MAX_STORED_PLANT_FOOD) {
            result.appendToMessage(
                    "cannot store more than " + ShopItem.MAX_STORED_PLANT_FOOD
                            + " plant foods"
            );
            return result;
        }

        Long total = chargeableTotal(result, user, ShopItem.PLANT_FOOD, count);
        if (total == null) {
            return result;
        }

        user.setGems(user.getGems() - total.intValue());
        user.setPlantFood(user.getPlantFood() + count);
        userService.updateUser(user);

        return success(result, "purchased " + count + " plant food" + plural(count)
                + "; stored: " + user.getPlantFood()
                + "; diamonds: " + user.getGems());
    }

    private Result<String> buyRandomSeeds(Result<String> result, User user, int count) {
        List<PlantDefinition> unlocked = unlockedPlants(user);
        if (unlocked.isEmpty()) {
            result.appendToMessage("no unlocked plant is available");
            return result;
        }

        Long total = chargeableTotal(result, user, ShopItem.RANDOM_SEEDS, count);
        if (total == null) {
            return result;
        }

        user.setCoins(user.getCoins() - total.intValue());
        StringBuilder grants = new StringBuilder();
        for (int i = 0; i < count; i++) {
            PlantDefinition selected = unlocked.get(random.nextInt(unlocked.size()));
            grantPackets(user, selected.getType(), ShopItem.RANDOM_BUNDLE_PACKETS);
            if (grants.length() > 0) {
                grants.append(", ");
            }
            grants.append(selected.getName());
        }
        userService.updateUser(user);

        return success(result, "purchased " + count + " random seed bundle"
                + plural(count) + " (" + grants + ", "
                + ShopItem.RANDOM_BUNDLE_PACKETS + " packets each)"
                + "; coins: " + user.getCoins());
    }

    private Result<String> buySelectedSeeds(
            Result<String> result, User user, int count, String plantToken) {
        if (plantToken == null || plantToken.isBlank()) {
            result.appendToMessage("selected seed bundle requires -t <plant_type>");
            return result;
        }

        PlantDefinition definition = plantRepository.findByName(plantToken.trim());
        if (definition == null) {
            result.appendToMessage("no plant of type \"" + plantToken.trim() + "\"");
            return result;
        }
        if (!user.getCollection().hasPlant(definition.getType())) {
            result.appendToMessage(definition.getName() + " is not unlocked");
            return result;
        }

        Long total = chargeableTotal(result, user, ShopItem.SELECTED_SEEDS, count);
        if (total == null) {
            return result;
        }

        user.setGems(user.getGems() - total.intValue());
        int packets = count * ShopItem.SELECTED_BUNDLE_PACKETS;
        grantPackets(user, definition.getType(), packets);
        userService.updateUser(user);

        return success(result, "purchased " + packets + " seed packets for "
                + definition.getName() + "; diamonds: " + user.getGems());
    }

    private Result<String> buyCoinPack(Result<String> result, User user, int count) {
        Long total = chargeableTotal(result, user, ShopItem.COIN_PACK, count);
        if (total == null) {
            return result;
        }

        long grantedCoins = (long) count * ShopItem.COIN_PACK_COINS;
        if (user.getCoins() + grantedCoins > Integer.MAX_VALUE) {
            result.appendToMessage("coin balance would overflow");
            return result;
        }

        user.setGems(user.getGems() - total.intValue());
        user.setCoins(user.getCoins() + (int) grantedCoins);
        userService.updateUser(user);

        return success(result, "converted " + total + " diamonds into "
                + grantedCoins + " coins; coins: " + user.getCoins()
                + "; diamonds: " + user.getGems());
    }

    private Result<String> buyDaily(Result<String> result, User user, int count) {
        DailyShopState state = user.getDailyShop();

        if (count != 1 || state.isPurchased(state.getOfferId())) {
            result.appendToMessage("the daily offer can be purchased only once per day");
            return result;
        }

        PlantDefinition plant = offerPlant(state);
        if (plant == null || !user.getCollection().hasPlant(plant.getType())) {
            result.appendToMessage("no daily offer is available");
            return result;
        }

        Long total = chargeableTotal(result, user, ShopItem.DAILY, 1);
        if (total == null) {
            return result;
        }

        user.setCoins(user.getCoins() - total.intValue());
        grantPackets(user, plant.getType(), ShopItem.DAILY_PACKETS);
        state.markPurchased(state.getOfferId());
        userService.updateUser(user);

        return success(result, "purchased daily offer: " + ShopItem.DAILY_PACKETS
                + " seed packets for " + plant.getName()
                + "; coins: " + user.getCoins());
    }

    // ------------------------------------------------------------------
    // Daily offer lifecycle
    // ------------------------------------------------------------------

    /**
     * Rolls a fresh offer whenever the persisted one is missing or belongs to a
     * previous day. The stored date flips at 00:00 of the injected clock, and
     * an unchanged date leaves the persisted offer (and its purchase mark)
     * untouched, so restarts neither reroll nor re-enable the offer.
     */
    private void ensureDailyOffer(User user) {
        DailyShopState state = user.getDailyShop();
        String today = userService.today().toString();

        if (today.equals(state.getOfferDate()) && state.getOfferPlant() != null) {
            return;
        }

        List<PlantDefinition> unlocked = unlockedPlants(user);
        state.setOfferDate(today);
        state.setOfferId("daily-" + user.getUsername() + "-" + today);
        state.getPurchasedItemIds().clear();
        state.setOfferPlant(unlocked.isEmpty()
                ? null
                : unlocked.get(random.nextInt(unlocked.size())).getType().name());

        userService.updateUser(user);
    }

    private PlantDefinition offerPlant(DailyShopState state) {
        if (state.getOfferPlant() == null) {
            return null;
        }
        try {
            return plantRepository.findByType(PlantType.valueOf(state.getOfferPlant()));
        } catch (IllegalArgumentException unknownType) {
            return null;
        }
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    /**
     * The overflow-safe total price when the user can afford it, otherwise
     * {@code null} with the rejection message already appended.
     */
    private Long chargeableTotal(
            Result<String> result, User user, ShopItem item, int count) {
        long total;
        try {
            total = Math.multiplyExact((long) item.getUnitPrice(), count);
        } catch (ArithmeticException overflow) {
            result.appendToMessage("total cost is too large");
            return null;
        }

        long balance = item.getCurrency() == ShopItem.Currency.COINS
                ? user.getCoins() : user.getGems();
        if (total > balance) {
            result.appendToMessage("not enough " + item.getCurrency().getLabel());
            return null;
        }
        return total;
    }

    private void grantPackets(User user, PlantType type, int amount) {
        PlantCard card = user.getCollection().getPlantCard(type);
        card.addSeedPackets(amount);
    }

    private List<PlantDefinition> unlockedPlants(User user) {
        List<PlantDefinition> unlocked = new ArrayList<>();
        for (PlantDefinition definition : plantRepository.findAll()) {
            if (!definition.isBonus()
                    && user.getCollection().hasPlant(definition.getType())) {
                unlocked.add(definition);
            }
        }
        return unlocked;
    }

    private String formatCatalogLine(ShopItem item) {
        return item.getId() + ": " + item.getUnitPrice() + " "
                + item.getCurrency().getLabel() + " - " + item.getDescription();
    }

    private String formatDailyLine(User user) {
        DailyShopState state = user.getDailyShop();
        PlantDefinition plant = offerPlant(state);
        if (plant == null) {
            return "daily: no offer available (no unlocked plant)";
        }

        String status = state.isPurchased(state.getOfferId())
                ? "already purchased today" : "available";
        return "daily: " + ShopItem.DAILY_PACKETS + " seed packets for "
                + plant.getName() + " - " + ShopItem.DAILY.getUnitPrice()
                + " coins (20% off " + ShopItem.DAILY_BASE_PRICE + ") - "
                + status;
    }

    private Result<String> success(Result<String> result, String message) {
        result.setStatus(true);
        result.setData(message);
        result.appendToMessage(message);
        return result;
    }

    private String plural(int count) {
        return count == 1 ? "" : "s";
    }

    private <T> User requireUser(Result<T> result) {
        User user = Store.getLoggedInUser();
        if (user == null) {
            result.appendToMessage("no user is logged in");
        }
        return user;
    }
}
