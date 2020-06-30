package com.scorfield.barfinder;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Intent;
import android.location.Location;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

public class LocationService extends Service {

    private static final String TAG = "LocationService";

    private LocationCallback locationCallback = new LocationCallback() {
        @Override
        public void onLocationResult(LocationResult locationResult) {
            super.onLocationResult(locationResult);

            if (locationResult != null && locationResult.getLastLocation() != null) {
                Location lastLocation = locationResult.getLastLocation();
                double latitude = lastLocation.getLatitude();
                double longitude = lastLocation.getLongitude();
                Log.d(TAG, "onLocationResult: " + longitude + " : " + latitude);

                Intent in = new Intent();
                in.putExtra("lat", latitude);
                in.putExtra("lng", longitude);
                in.setAction("FILTER");
                sendBroadcast(in);
            }
        }
    };

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("not yet");
    }

    @SuppressLint("MissingPermission")
    private void startLocationService() {
//        String channelId = "location_notification_channel";
//        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
//
//        Intent resultIntent = new Intent();
//        PendingIntent pendingIntent = PendingIntent.getActivity(
//                getApplicationContext(),
//                0,
//                resultIntent,
//                PendingIntent.FLAG_CANCEL_CURRENT);
//
//        NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext(), channelId);
//        builder.setSmallIcon(R.drawable.ic_location_service);
//        builder.setContentTitle("Location Service");
//        builder.setDefaults(NotificationCompat.DEFAULT_ALL);
//        builder.setContentText("Running");
//        builder.setContentIntent(pendingIntent);
//        builder.setAutoCancel(false);
//        builder.setPriority(NotificationCompat.PRIORITY_MAX);
//
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            if (notificationManager != null && notificationManager.getNotificationChannel(channelId) == null) {
//                NotificationChannel notificationChannel = new NotificationChannel(
//                        channelId,
//                        "com.scorfield.barfinder.Location Service",
//                        NotificationManager.IMPORTANCE_HIGH
//                );
//                notificationChannel.setDescription("This channel is used by location service");
//                notificationManager.createNotificationChannel(notificationChannel);
//            }
//        }
//        startForeground(GoogleServiceConstants.LOCATION_SERVICE_ID, builder.build());


        LocationRequest locationRequest = new LocationRequest();
        locationRequest.setInterval(4000);
        locationRequest.setFastestInterval(2000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        LocationServices.getFusedLocationProviderClient(this).requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
    }

    private void stopLocationService() {
        LocationServices.getFusedLocationProviderClient(this)
                .removeLocationUpdates(locationCallback);
        stopForeground(true);
        stopSelf();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String action = intent.getAction();
            if (action != null) {
                if (action.equals(GoogleServiceConstants.ACTION_START_LOCATION_SERVICE)) {
                    startLocationService();
                } else if (action.equals(GoogleServiceConstants.ACTION_STOP_LOCATION_SERVICE)) {
                    stopLocationService();
                }
            }
        }
        return super.onStartCommand(intent, flags, startId);
    }
}
