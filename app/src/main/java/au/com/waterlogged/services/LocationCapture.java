package au.com.waterlogged.services;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

/** Foreground one-shot capture, with timeout and permission/provider failure handling. */
public class LocationCapture {
    public interface Callback {
        void success(Location location);

        void failure(String message);
    }

    private final Context context;
    private final LocationManager manager;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private LocationListener listener;
    private Runnable timeout;
    private Runnable settle;
    private Location networkCandidate;

    public LocationCapture(Context c) {
        context = c.getApplicationContext();
        manager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
    }

    public void capture(Callback callback) {
        cancel();
        if (manager == null) {
            callback.failure("This device does not provide location services.");
            return;
        }
        boolean fine =
                ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                        == PackageManager.PERMISSION_GRANTED;
        boolean coarse =
                ContextCompat.checkSelfPermission(
                                context, Manifest.permission.ACCESS_COARSE_LOCATION)
                        == PackageManager.PERMISSION_GRANTED;
        if (!fine && !coarse) {
            callback.failure(
                    "Location permission was not granted. You can still enter a location name.");
            return;
        }
        List<String> providers = new ArrayList<>();
        if (fine && manager.isProviderEnabled(LocationManager.GPS_PROVIDER))
            providers.add(LocationManager.GPS_PROVIDER);
        if (manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER))
            providers.add(LocationManager.NETWORK_PROVIDER);
        if (providers.isEmpty()) {
            callback.failure("Turn on device location, then tap Get GPS again.");
            return;
        }
        listener =
                new LocationListener() {
                    @Override
                    public void onLocationChanged(Location l) {
                        // Ignore cached/stale fixes; preserve the timestamp and reported accuracy.
                        if (Math.abs(System.currentTimeMillis() - l.getTime()) > 120000) return;
                        if (LocationManager.GPS_PROVIDER.equals(l.getProvider())
                                || !fine
                                || !providers.contains(LocationManager.GPS_PROVIDER)) {
                            cancel();
                            callback.success(l);
                            return;
                        }
                        // Give a precise GPS fix a short chance to replace a fresh network-area
                        // fix.
                        if (networkCandidate == null
                                || l.getAccuracy() < networkCandidate.getAccuracy())
                            networkCandidate = new Location(l);
                        if (settle == null) {
                            settle =
                                    () -> {
                                        Location chosen = networkCandidate;
                                        cancel();
                                        if (chosen != null) callback.success(chosen);
                                    };
                            handler.postDelayed(settle, 2000);
                        }
                    }

                    @Override
                    public void onStatusChanged(String p, int s, Bundle extras) {}

                    @Override
                    public void onProviderEnabled(String p) {}

                    @Override
                    public void onProviderDisabled(String p) {}
                };
        timeout =
                () -> {
                    cancel();
                    callback.failure(
                            "No fresh location fix. Try outdoors or enter a location name.");
                };
        try {
            for (String p : providers)
                manager.requestLocationUpdates(p, 0, 0, listener, Looper.getMainLooper());
            handler.postDelayed(timeout, 25000);
        } catch (SecurityException | IllegalArgumentException e) {
            cancel();
            callback.failure("Location could not be captured. Check permissions and try again.");
        }
    }

    public void cancel() {
        if (listener != null) {
            try {
                manager.removeUpdates(listener);
            } catch (SecurityException ignored) {
            }
            listener = null;
        }
        if (timeout != null) {
            handler.removeCallbacks(timeout);
            timeout = null;
        }
        if (settle != null) {
            handler.removeCallbacks(settle);
            settle = null;
        }
        networkCandidate = null;
    }
}
