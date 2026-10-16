package au.com.waterlogged.services;

/** SunCalc 1.9.0 lunar illumination calculation, BSD-2-Clause; see third_party_notices.txt. */
public final class MoonPhase {
    private static final double RAD = Math.PI / 180, E = RAD * 23.4397;

    private MoonPhase() {}

    private static double ra(double l, double b) {
        return Math.atan2(Math.sin(l) * Math.cos(E) - Math.tan(b) * Math.sin(E), Math.cos(l));
    }

    private static double dec(double l, double b) {
        return Math.asin(Math.sin(b) * Math.cos(E) + Math.cos(b) * Math.sin(E) * Math.sin(l));
    }

    public static double fraction(long time) {
        double d = time / 86400000.0 - 0.5 + 2440588 - 2451545;
        double m = RAD * (357.5291 + 0.98560028 * d);
        double l =
                m
                        + RAD
                                * (1.9148 * Math.sin(m)
                                        + 0.02 * Math.sin(2 * m)
                                        + 0.0003 * Math.sin(3 * m))
                        + RAD * 102.9372
                        + Math.PI;
        double sr = ra(l, 0), sd = dec(l, 0);
        double mm = RAD * (134.963 + 13.064993 * d);
        double ml = RAD * (218.316 + 13.176396 * d) + RAD * 6.289 * Math.sin(mm);
        double mb = RAD * 5.128 * Math.sin(RAD * (93.272 + 13.229350 * d));
        double mr = ra(ml, mb), md = dec(ml, mb), dist = 385001 - 20905 * Math.cos(mm);
        double cosPhi =
                Math.sin(sd) * Math.sin(md) + Math.cos(sd) * Math.cos(md) * Math.cos(sr - mr);
        double phi = Math.acos(Math.max(-1, Math.min(1, cosPhi)));
        double inc = Math.atan2(149598000 * Math.sin(phi), dist - 149598000 * Math.cos(phi));
        double angle =
                Math.atan2(
                        Math.cos(sd) * Math.sin(sr - mr),
                        Math.sin(sd) * Math.cos(md)
                                - Math.cos(sd) * Math.sin(md) * Math.cos(sr - mr));
        return 0.5 + 0.5 * inc * (angle < 0 ? -1 : 1) / Math.PI;
    }

    public static String name(long time) {
        String[] names = {
            "New moon",
            "Waxing crescent",
            "First quarter",
            "Waxing gibbous",
            "Full moon",
            "Waning gibbous",
            "Last quarter",
            "Waning crescent"
        };
        return names[(int) Math.round(fraction(time) * 8) % 8];
    }
}
