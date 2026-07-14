package model.user;

import java.util.ArrayList;
import java.util.List;

/**
 * Persisted identity of the daily shop offer and what the player already bought
 * from it. Holds state only; refreshing the offer belongs to the shop service,
 * which decides "today" from an injected clock.
 */
public class DailyShopState {
    private String offerId;
    private String offerDate;
    private List<String> purchasedItemIds = new ArrayList<>();

    public String getOfferId() {
        return offerId;
    }

    public void setOfferId(String offerId) {
        this.offerId = offerId;
    }

    /** ISO-8601 local date (yyyy-MM-dd) the current offer was generated for. */
    public String getOfferDate() {
        return offerDate;
    }

    public void setOfferDate(String offerDate) {
        this.offerDate = offerDate;
    }

    public List<String> getPurchasedItemIds() {
        if (purchasedItemIds == null) {
            purchasedItemIds = new ArrayList<>();
        }

        return purchasedItemIds;
    }

    public boolean isPurchased(String itemId) {
        return getPurchasedItemIds().contains(itemId);
    }

    public void markPurchased(String itemId) {
        if (itemId != null && !isPurchased(itemId)) {
            getPurchasedItemIds().add(itemId);
        }
    }
}
