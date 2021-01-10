package com.scorfield.barfinder;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.LinearInterpolator;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.GroundOverlay;
import com.google.android.gms.maps.model.GroundOverlayOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.scorfield.barfinder.mapsdirection.FetchURL;
import com.scorfield.barfinder.mapsdirection.TaskLoadedCallback;
import com.scorfield.barfinder.utils.AnimUtils;

import org.json.JSONObject;

import java.util.List;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, TaskLoadedCallback, View.OnClickListener {

    RelativeLayout mapActivityContainer;
    private GoogleMap mMap;
    private Polyline currentPolyline;
    private GroundOverlay groundOverlay;
    LatLng myLatLng, storeLatLng;
    String placeId;

    AnimUtils animUtils;
    CardView cardView;
    ImageButton btnExpand, btnBack, btnCall, btnWeb;

    TextView storeName, address, closed, rating;

    BroadcastReceiver br;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        mapActivityContainer = findViewById(R.id.map_activity_container);


        animUtils = new AnimUtils();

        storeName = findViewById(R.id.nameToolbar);
        address = findViewById(R.id.txt_map_address);
        closed = findViewById(R.id.txt_map_closed);
        rating = findViewById(R.id.txt_map_rating);

        cardView = findViewById(R.id.card_view);
        btnBack = findViewById(R.id.btn_map_back);
        btnExpand = findViewById(R.id.btn_expand);
        btnCall = findViewById(R.id.btn_map_call);
        btnWeb = findViewById(R.id.btn_map_web);

        btnBack.setOnClickListener(this);
        btnExpand.setOnClickListener(this);

        Intent in = getIntent();
        myLatLng = (LatLng) in.getExtras().get("me");
        storeLatLng = (LatLng) in.getExtras().get("store");
        placeId = in.getExtras().getString("placeId");

        br = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                double lat = intent.getExtras().getDouble("lat");
                double lng = intent.getExtras().getDouble("lng");
                myLatLng = new LatLng(lat, lng);
                addOverlay(myLatLng);
            }
        };
        startLocationService();
        getPlaceDetails(placeId);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    private void getPlaceDetails(String placeId) {
        String params = "place_id=" + placeId + "&fields=formatted_address,name,url,rating,formatted_phone_number,opening_hours";
        String url = "https://maps.googleapis.com/maps/api/place/details/json?" + params + "&key=" + getString(R.string.google_maps_key);
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, url.replace(" ", "%20"), null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try {
                    JSONObject result = (JSONObject) response.get("result");
                    storeName.setText(result.getString("name"));
                    address.setText(result.getString("formatted_address"));

                    boolean open;
                    if (result.opt("opening_hours") != null) {
                        JSONObject open_hours = (JSONObject) result.get("opening_hours");
                        open = open_hours.optBoolean("open_now");
                    } else {
                        open = false;
                    }

                    closed.setText(open ? "Open" : "Closed");
                    closed.setTextColor(open ?
                            ContextCompat.getColor(MapsActivity.this, R.color.colorGreen) :
                            ContextCompat.getColor(MapsActivity.this, R.color.colorRed));

                    rating.setText(String.valueOf(result.getDouble("rating")));

                    final String number = result.optString("formatted_phone_number");

                    btnCall.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            if (number != null) {
                                Intent callIntent = new Intent(Intent.ACTION_CALL);
                                callIntent.setData(Uri.parse("tel:" + number));//change the number
                                startActivity(callIntent);
                            } else {
                                Toast.makeText(MapsActivity.this, "Telephone number not registered!", Toast.LENGTH_LONG).show();
                            }
                        }
                    });

                    final String website = result.getString("url");
                    btnWeb.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Uri uri = Uri.parse(website);
                            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                            startActivity(intent);
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                error.printStackTrace();
            }
        });

        jsonObjectRequest.setRetryPolicy(new DefaultRetryPolicy(
                10000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT)
        );

        RequestQueue requestQueue = Volley.newRequestQueue(MapsActivity.this);
        requestQueue.add(jsonObjectRequest);
    }


    @SuppressLint("MissingPermission")
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setMyLocationEnabled(true);
        mMap.getUiSettings().setMyLocationButtonEnabled(false);
        mMap.getUiSettings().setRotateGesturesEnabled(false);
        mMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(this, R.raw.maps_theme));

        String url = getDirectionsUrl(myLatLng, storeLatLng, "driving");
        new FetchURL(MapsActivity.this).execute(url, "walking");
    }


    public void addOverlay(LatLng place) {
        if (groundOverlay != null) {
            groundOverlay.remove();
        }
        groundOverlay = mMap.addGroundOverlay(new GroundOverlayOptions()
                .position(place, 200)
                .transparency(0.8f)
                .zIndex(3)
                .image(BitmapDescriptorFactory.fromBitmap(drawableToBitmap(getDrawable(R.drawable.map_overlay)))));

        startOverlayAnimation(groundOverlay);
    }

    private void startOverlayAnimation(final GroundOverlay groundOverlay) {
        AnimatorSet animatorSet = new AnimatorSet();

        ValueAnimator vAnimator = ValueAnimator.ofInt(0, 100);
        vAnimator.setRepeatCount(ValueAnimator.INFINITE);
        vAnimator.setRepeatMode(ValueAnimator.RESTART);
        vAnimator.setInterpolator(new LinearInterpolator());
        vAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                final Integer val = (Integer) valueAnimator.getAnimatedValue();
                groundOverlay.setDimensions(val);
            }
        });

        ValueAnimator tAnimator = ValueAnimator.ofFloat(0, 1);
        tAnimator.setRepeatCount(ValueAnimator.INFINITE);
        tAnimator.setRepeatMode(ValueAnimator.RESTART);
        tAnimator.setInterpolator(new LinearInterpolator());
        tAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                Float val = (Float) valueAnimator.getAnimatedValue();
                groundOverlay.setTransparency(val);
            }
        });

        animatorSet.setDuration(3000);
        animatorSet.playTogether(vAnimator, tAnimator);
        animatorSet.start();
    }

    private Bitmap drawableToBitmap(Drawable drawable) {
        Bitmap bitmap = null;

        if (drawable instanceof BitmapDrawable) {
            BitmapDrawable bitmapDrawable = (BitmapDrawable) drawable;
            if (bitmapDrawable.getBitmap() != null) {
                return bitmapDrawable.getBitmap();
            }
        }

        if (drawable.getIntrinsicWidth() <= 0 || drawable.getIntrinsicHeight() <= 0) {
            bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
        } else {
            bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        }

        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        return bitmap;
    }

    private String getDirectionsUrl(LatLng origin, LatLng dest, String directionMode) {
        // Origin of route
        String str_origin = "origin=" + origin.latitude + "," + origin.longitude;
        // Destination of route
        String str_dest = "destination=" + dest.latitude + "," + dest.longitude;
        // Mode
        String mode = "mode=" + directionMode;
        // Building the parameters to the web service
        String parameters = str_origin + "&" + str_dest + "&" + mode;
        // Output format
        String output = "json";
        // Building the url to the web service
        String url = "https://maps.googleapis.com/maps/api/directions/" + output + "?" + parameters + "&key=" + getString(R.string.google_maps_key);
        return url;
    }

    public static void zoomToPolyline(GoogleMap map, Polyline p) {
        if (p == null || p.getPoints().isEmpty())
            return;

        LatLngBounds.Builder builder = LatLngBounds.builder();

        for (LatLng latLng : p.getPoints()) {
            builder.include(latLng);
        }
        final LatLngBounds bounds = builder.build();
        try {
            map.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 250));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onTaskDone(Object... values) {
        if (currentPolyline != null) {
            currentPolyline.remove();
        }
        currentPolyline = mMap.addPolyline((PolylineOptions) values[0]);
        List<LatLng> points = currentPolyline.getPoints();
        MarkerOptions options = new MarkerOptions()
                .position(points.get(0))
                .title("Your Location")
                .icon(generateBitmapDescriptorFromRes(this, R.drawable.ic_marker_me));
        mMap.addMarker(options);

        MarkerOptions store = new MarkerOptions()
                .position(points.get(points.size() - 1))
                .title("Liquor Store")
                .icon(generateBitmapDescriptorFromRes(this, R.drawable.ic_marker_store));
        mMap.addMarker(store);
        zoomToPolyline(mMap, currentPolyline);
    }

    @Override
    protected void onResume() {
        super.onResume();
        IntentFilter filter = new IntentFilter("FILTER");
        this.registerReceiver(br, filter);
    }

    @Override
    protected void onPause() {
        unregisterReceiver(br);
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        stopLocationService();
        super.onDestroy();
    }

    private BitmapDescriptor generateBitmapDescriptorFromRes(Context context, int resId) {
        Drawable drawable = ContextCompat.getDrawable(context, resId);
        drawable.setBounds(
                0,
                0,
                drawable.getIntrinsicWidth(),
                drawable.getIntrinsicHeight());
        Bitmap bitmap = Bitmap.createBitmap(
                drawable.getIntrinsicWidth(),
                drawable.getIntrinsicHeight(),
                Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.draw(canvas);
        return BitmapDescriptorFactory.fromBitmap(bitmap);
    }

    @Override
    public void onClick(View v) {
        if (v == btnBack) {
            finish();
        } else if (v == btnExpand) {
            if (animUtils.getState() == 0) {
                btnExpand.setImageResource(R.drawable.ic_arrow_down);
                animUtils.expand(cardView);
            } else {
                btnExpand.setImageResource(R.drawable.ic_arrow_up);
                animUtils.collapse(cardView);
            }
        }
    }

    /**
     * check location is running
     *
     * @return boolean
     */
    private boolean isLocationServiceRunning() {
        ActivityManager activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        if (activityManager != null) {
            for (ActivityManager.RunningServiceInfo serviceInfo : activityManager.getRunningServices(Integer.MAX_VALUE)) {
                if (LocationService.class.getName().equals(serviceInfo.service.getClassName())) {
                    if (serviceInfo.foreground) {
                        return true;
                    }
                }
            }
            return false;
        }
        return false;
    }

    /**
     * Start location service for get the latlng
     */
    private void startLocationService() {
        if (!isLocationServiceRunning()) {
            Intent intent = new Intent(getApplicationContext(), LocationService.class);
            intent.setAction(GoogleServiceConstants.ACTION_START_LOCATION_SERVICE);
            startService(intent);
        }
    }

    /**
     * Stop location service for get the latlng
     */
    private void stopLocationService() {
        if (isLocationServiceRunning()) {
            Intent intent = new Intent(getApplicationContext(), LocationService.class);
            intent.setAction(GoogleServiceConstants.ACTION_STOP_LOCATION_SERVICE);
            stopService(intent);
        }
    }
}
