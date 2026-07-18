package integration;

import model.enums.Command;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Parser contract for every command whose exact form is prescribed by Phase 1. */
class Phase1CommandContractTest {

    @Test
    @SuppressWarnings("PMD.ExcessiveMethodLength")
    void everyDocumentedExactCommandFormIsRegistered() {
        Map<Command, String> samples = new LinkedHashMap<>();
        samples.put(Command.MENU_ENTER, "menu enter game");
        samples.put(Command.MENU_SHOW_CURRENT, "menu show current");
        samples.put(Command.MENU_EXIT, "menu exit");
        samples.put(Command.REGISTER,
                "register -u user-1 -p Abcdef1! Abcdef1! -n Player One "
                        + "-e one@example.com -g male");
        samples.put(Command.PICK_QUESTION, "pick question -q 1 -a answer -c answer");
        samples.put(Command.LOGIN, "login -u user-1 -p Abcdef1! -stay-logged-in");
        samples.put(Command.FORGET_PASSWORD,
                "forget password -u user-1 -e one@example.com");
        samples.put(Command.ANSWER, "answer -a answer");
        samples.put(Command.MENU_LOGOUT, "menu logout");
        samples.put(Command.MENU_ENTER_CHAPTER,
                "menu enter chapter -c Ancient Egypt");
        samples.put(Command.MENU_GREENHOUSE, "menu greenhouse");
        samples.put(Command.MENU_TRAVEL_LOG, "menu travel-log");
        samples.put(Command.MENU_LEADERBOARD, "menu leaderboard");
        samples.put(Command.MENU_COIN_WALLET, "menu coin-wallet");
        samples.put(Command.MENU_GEM_WALLET, "menu gem-wallet");
        samples.put(Command.MENU_CHEAT_ADD, "menu cheat add 100 coin");
        samples.put(Command.MENU_SETTINGS_CHANGE_DIFFICULTY,
                "menu settings change-difficulty -l 3");
        samples.put(Command.MENU_NEWS_SHOW_UNREAD, "menu news show-unread");
        samples.put(Command.MENU_NEWS_SHOW_ALL, "menu news show-all");
        samples.put(Command.MENU_PROFILE_CHANGE_USERNAME,
                "menu profile change-username -u user-2");
        samples.put(Command.MENU_PROFILE_CHANGE_NICKNAME,
                "menu profile change-nickname -u Player Two");
        samples.put(Command.MENU_PROFILE_CHANGE_EMAIL,
                "menu profile change-email -e two@example.com");
        samples.put(Command.MENU_PROFILE_CHANGE_PASSWORD,
                "menu profile change-password -p Newpass1! -o Abcdef1!");
        samples.put(Command.MENU_PROFILE_SHOW_INFO, "menu profile show-info");
        samples.put(Command.MENU_COLLECTION_SHOW_PLANTS,
                "menu collection show-plants");
        samples.put(Command.MENU_COLLECTION_SHOW_ALL_PLANTS,
                "menu collection show-all-plants");
        samples.put(Command.MENU_COLLECTION_SHOW_ZOMBIES,
                "menu collection show-zombies");
        samples.put(Command.MENU_COLLECTION_SHOW_ALL_ZOMBIES,
                "menu collection show-all-zombies");
        samples.put(Command.MENU_COLLECTION_SHOW_PLANT,
                "menu collection show-plant -p peashooter");
        samples.put(Command.MENU_COLLECTION_SHOW_ZOMBIE,
                "menu collection show-zombie -z normal zombie");
        samples.put(Command.MENU_COLLECTION_UPGRADE_PLANT,
                "menu collection upgrade-plant -p peashooter");
        samples.put(Command.MENU_COLLECTION_PURCHASE_PLANT,
                "menu collection purchase-plant -p peashooter");
        samples.put(Command.SHOW_ALL_PLANTS, "show all plants");
        samples.put(Command.SHOW_AVAILABLE_PLANTS, "show available plants");
        samples.put(Command.ADD_PLANT, "add plant -t peashooter");
        samples.put(Command.REMOVE_PLANT, "remove plant -t peashooter");
        samples.put(Command.BOOST_PLANT, "boost plant -t peashooter");
        samples.put(Command.START_GAME, "start game");
        samples.put(Command.ADVANCE_TIME, "advance time -t 10 ticks");
        samples.put(Command.START_ZOMBIE_WAVES, "start zombie waves");
        samples.put(Command.COLLECT_SUN, "collect sun -l (1, 2)");
        samples.put(Command.SHOW_SUN_AMOUNT, "show sun amount");
        samples.put(Command.CHEAT_ADD_SUNS, "cheat add -n 25 suns");
        samples.put(Command.PLANT_PLANT,
                "plant plant -t peashooter -l (1, 2)");
        samples.put(Command.PLUCK_PLANT, "pluck plant -l (1, 2)");
        samples.put(Command.CHEAT_REMOVE_COOLDOWN, "cheat remove-cooldown");
        samples.put(Command.FEED_PLANT, "feed plant -l (1, 2)");
        samples.put(Command.CHEAT_ADD_PLANT_FOOD, "cheat add-plant-food");
        samples.put(Command.SHOW_MAP, "show map");
        samples.put(Command.SHOW_PLANTS_STATUS, "show plants status");
        samples.put(Command.SHOW_TILE_STATUS, "show tile status -l (1, 2)");
        samples.put(Command.RELEASE_NUKE, "release the nuke");
        samples.put(Command.ZOMBIES_INFO, "zombies info");
        samples.put(Command.CHEAT_SPAWN_ZOMBIE,
                "cheat spawn-zombie -t normal -l 8, 2");
        samples.put(Command.SHOW_GREENHOUSE, "show greenhouse");
        samples.put(Command.PLANT_POT, "plant pot at (1, 1)");
        samples.put(Command.COLLECT_GREENHOUSE, "collect (1, 1)");
        samples.put(Command.GROW_GREENHOUSE, "grow (1, 1)");
        samples.put(Command.ENTER_SHOP, "enter shop");
        samples.put(Command.SHOP_LIST, "shop list");
        samples.put(Command.SHOP_DAILY, "shop daily");
        samples.put(Command.SHOP_BUY,
                "shop buy -i selected-seed-packet -n 1 -t peashooter");
        samples.put(Command.TRAVEL_LOG_PAGE, "travel log page minigame");

        for (Map.Entry<Command, String> sample : samples.entrySet()) {
            assertTrue(sample.getKey().matches(sample.getValue()),
                    sample.getKey() + " rejected: " + sample.getValue());
        }
    }

    @Test
    void malformedVariantsDoNotAccidentallyMatch() {
        assertFalse(Command.ADVANCE_TIME.matches("advance time -t -1 ticks"));
        assertFalse(Command.PLANT_PLANT.matches("plant peashooter at 1 2"));
        assertFalse(Command.MENU_CHEAT_ADD.matches("menu cheat add coins 100"));
        assertFalse(Command.SHOP_BUY.matches("shop buy selected-seed-packet"));
        assertFalse(Command.MENU_ENTER_CHAPTER.matches("enter chapter Ancient Egypt"));
    }
}
