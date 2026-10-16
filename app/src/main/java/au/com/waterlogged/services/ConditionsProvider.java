package au.com.waterlogged.services;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;

/** Results are delivered incrementally on the main thread for the requested catch time. */
public interface ConditionsProvider {
    enum Field {
        LOCATION,
        TEMPERATURE,
        WIND_SPEED,
        WIND_DIRECTION,
        WIND_GUST,
        SWELL,
        WAVE_HEIGHT,
        TIDE_HEIGHT,
        TIDE_TREND,
        MOON
    }

    void fetch(
            double latitude,
            double longitude,
            long caughtAt,
            boolean coastal,
            String tideStationId,
            Callback callback);

    default void fetchFresh(
            double latitude,
            double longitude,
            long caughtAt,
            boolean coastal,
            String tideStationId,
            Callback callback) {
        fetch(latitude, longitude, caughtAt, coastal, tideStationId, callback);
    }

    interface Callback {
        void update(Snapshot snapshot);
    }

    class Reading {
        public final String value, source;
        public final long validAt, retrievedAt;

        public Reading(Object value, String source, long validAt, long retrievedAt) {
            this.value = String.valueOf(value);
            this.source = source;
            this.validAt = validAt;
            this.retrievedAt = retrievedAt;
        }
    }

    class Snapshot {
        public TideSeries tideSeries;
        public final EnumMap<Field, Reading> readings = new EnumMap<>(Field.class);
        public final List<String> notices = new ArrayList<>();
        public boolean complete;
    }
}
