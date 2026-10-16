package au.com.waterlogged.model;

import static org.junit.Assert.*;

import org.junit.Test;

public class InputRulesTest {
    @Test
    public void missingOptionalNumbersRemainUnknown() {
        assertNull(InputRules.optionalNumber("", 0, 1000, "Length"));
        assertNull(InputRules.optionalNumber("   ", 0, 1000, "Length"));
    }

    @Test
    public void validMeasurementsKeepDecimals() {
        assertEquals(48.5, InputRules.optionalNumber("48.5", 0.1, 1000, "Length"), 0);
        assertEquals(-4.0, InputRules.optionalNumber("-4", -90, 65, "Temperature"), 0);
        assertEquals(0.0, InputRules.optionalNumber("0", 0, 400, "Wind"), 0);
    }

    @Test
    public void malformedAndNonfiniteNumbersAreRejected() {
        for (String s : new String[] {"abc", "NaN", "Infinity", "1e999", "-1", "1001", "1,5"}) {
            assertThrows(
                    IllegalArgumentException.class,
                    () -> InputRules.optionalNumber(s, 0, 1000, "Length"));
        }
    }

    @Test
    public void quantityRequiresPositiveWholeNumber() {
        assertEquals(1, InputRules.quantity("1"));
        assertEquals(999, InputRules.quantity("999"));
        for (String s : new String[] {"", "0", "-1", "1.5", "fish", "1000", "99999999999999"}) {
            assertThrows(IllegalArgumentException.class, () -> InputRules.quantity(s));
        }
    }
}
