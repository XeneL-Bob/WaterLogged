package au.com.waterlogged.services;

public final class GeoMath {
    private GeoMath() {}

    public static double rounded(double coordinate) {
        return Math.round(coordinate * 100.0) / 100.0;
    }

    public static double distanceKm(double lat1, double lon1, double lat2, double lon2) {
        double a =
                Math.pow(Math.sin(Math.toRadians(lat2 - lat1) / 2), 2)
                        + Math.cos(Math.toRadians(lat1))
                                * Math.cos(Math.toRadians(lat2))
                                * Math.pow(Math.sin(Math.toRadians(lon2 - lon1) / 2), 2);
        return 6371.0088 * 2 * Math.asin(Math.sqrt(Math.min(1, a)));
    }

    public static String compass(double degrees) {
        String[] points = {
            "N", "NNE", "NE", "ENE", "E", "ESE", "SE", "SSE", "S", "SSW", "SW", "WSW", "W", "WNW",
            "NW", "NNW"
        };
        return points[(int) Math.round(((degrees % 360 + 360) % 360) / 22.5) % 16];
    }
}
