package com.emergencyroutefinder.util;

/**
 * Haversine formula implementation for great-circle distance calculation.
 *
 * Demonstrates: Utility classes, mathematical computation in Java
 */
public final class HaversineUtil {

    private static final double EARTH_RADIUS_KM = 6371.0;

    private HaversineUtil() {} // Utility class — no instantiation

    /**
     * Calculate the great-circle distance between two geographic points
     * using the Haversine formula.
     *
     * @param lat1 Latitude of point 1 (degrees)
     * @param lng1 Longitude of point 1 (degrees)
     * @param lat2 Latitude of point 2 (degrees)
     * @param lng2 Longitude of point 2 (degrees)
     * @return Distance in kilometers
     */
    public static double distanceKm(double lat1, double lng1, double lat2, double lng2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                 + Math.cos(Math.toRadians(lat1))
                 * Math.cos(Math.toRadians(lat2))
                 * Math.sin(dLng / 2) * Math.sin(dLng / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_KM * c;
    }

    /**
     * Convert km/h to approximate traffic level string.
     */
    public static String speedToTrafficLevel(double speedKmh) {
        if (speedKmh >= 50) return "LOW";
        if (speedKmh >= 25) return "MODERATE";
        if (speedKmh >= 10) return "HIGH";
        return "SEVERE";
    }

    /**
     * Convert traffic level to delay factor (multiplier on base travel time).
     */
    public static double trafficLevelToDelayFactor(String level) {
        return switch (level) {
            case "LOW"      -> 1.1;
            case "MODERATE" -> 1.5;
            case "HIGH"     -> 2.0;
            case "SEVERE"   -> 3.0;
            default         -> 1.0;
        };
    }
}
