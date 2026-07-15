package shop;

import model.enums.Command;
import org.junit.jupiter.api.Test;

import java.util.regex.Matcher;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShopCommandTest {

    @Test
    void exactShopCommandsParse() {
        assertTrue(Command.ENTER_SHOP.matches("enter shop"));
        assertTrue(Command.SHOP_LIST.matches("shop list"));
        assertTrue(Command.SHOP_DAILY.matches("shop daily"));
        assertTrue(Command.SHOP_BUY.matches("shop buy -i pot -n 2"));
        assertTrue(Command.SHOP_BUY.matches(
                "shop buy -i selected-seeds -n 1 -t Snow Pea"));

        assertFalse(Command.SHOP_BUY.matches("shop buy pot 2"));
        assertFalse(Command.SHOP_BUY.matches("shop buy -i pot"));
        assertFalse(Command.SHOP_LIST.matches("shop  listx"));
    }

    @Test
    void buyCommandCapturesItemCountAndOptionalPlant() {
        Matcher withPlant = Command.SHOP_BUY.getMatcher(
                "shop buy -i selected-seeds -n 3 -t Peashooter");
        assertTrue(withPlant.matches());
        assertEquals("selected-seeds", withPlant.group(1));
        assertEquals("3", withPlant.group(2));
        assertEquals("Peashooter", withPlant.group(3));

        Matcher withoutPlant = Command.SHOP_BUY.getMatcher("shop buy -i daily -n 1");
        assertTrue(withoutPlant.matches());
        assertEquals("daily", withoutPlant.group(1));
        assertEquals("1", withoutPlant.group(2));
        assertNull(withoutPlant.group(3));

        Matcher negative = Command.SHOP_BUY.getMatcher("shop buy -i pot -n -4");
        assertTrue(negative.matches());
        assertEquals("-4", negative.group(2));
    }
}
