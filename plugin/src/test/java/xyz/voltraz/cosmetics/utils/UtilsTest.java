package xyz.voltraz.cosmetics.utils;

import org.bukkit.Color;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class UtilsTest {

    @Test
    public void testHex2RgbValid() {
        Color color = Utils.hex2Rgb("#FF0000");
        assertNotNull(color);
        assertEquals(255, color.getRed());
        assertEquals(0, color.getGreen());
        assertEquals(0, color.getBlue());
    }

    @Test
    public void testHex2RgbWithoutHash() {
        Color color = Utils.hex2Rgb("00FF00");
        assertNotNull(color);
        assertEquals(0, color.getRed());
        assertEquals(255, color.getGreen());
        assertEquals(0, color.getBlue());
    }

    @Test
    public void testHex2RgbInvalidFallback() {
        Color color = Utils.hex2Rgb("invalid");
        assertNotNull(color);
        assertEquals(Color.WHITE, color);

        Color nullColor = Utils.hex2Rgb(null);
        assertNotNull(nullColor);
        assertEquals(Color.WHITE, nullColor);
    }

    @Test
    public void testGetTimeFormatting() {
        assertEquals("05 seconds", Utils.getTime(5));
        assertEquals("01 second", Utils.getTime(1));
        assertEquals("02 minutes 00 seconds", Utils.getTime(120));
    }
}
