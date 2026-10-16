package au.com.waterlogged.model;

/** Pure Java validation shared by UI and tests. Empty optional values remain NULL. */
public final class InputRules {
    private InputRules() {}

    public static Double optionalNumber(String value, double min, double max, String label) {
        if (value.trim().isEmpty()) return null;
        try {
            double n = Double.parseDouble(value.trim());
            if (!Double.isFinite(n) || n < min || n > max) throw new NumberFormatException();
            return n;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                    label + " must be between " + min + " and " + max + ".");
        }
    }

    public static int quantity(String value) {
        try {
            int n = Integer.parseInt(value.trim());
            if (n < 1 || n > 999) throw new NumberFormatException();
            return n;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Quantity must be a whole number from 1 to 999.");
        }
    }
}
