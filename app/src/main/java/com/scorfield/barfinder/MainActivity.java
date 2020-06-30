package com.scorfield.barfinder;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.material.navigation.NavigationView;
import com.scorfield.barfinder.beans.BarAdapter;
import com.scorfield.barfinder.beans.BarBean;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements SwipeRefreshLayout.OnRefreshListener, NavigationView.OnNavigationItemSelectedListener {

    private static final String BAR_SHARED_PREFS = "BarCache";
    private static final String BAR_SEARCH_RANGE = "SearchRange";

    SharedPreferences sharedPreferences;

    SwipeRefreshLayout mSwipeRefreshLayout;
    RecyclerView recyclerView;

    private DrawerLayout drawer;

    BroadcastReceiver br;
    LatLng myLatLng;

    ArrayList<BarBean> barBeans;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sharedPreferences = getSharedPreferences(BAR_SHARED_PREFS, MODE_PRIVATE);

        Toolbar myToolbar = findViewById(R.id.toolbar);
        setSupportActionBar(myToolbar);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setHomeAsUpIndicator(R.drawable.ic_menu);

        drawer = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        recyclerView = findViewById(R.id.recyclerView);
        mSwipeRefreshLayout = findViewById(R.id.swipe_container);
        mSwipeRefreshLayout.setOnRefreshListener(this);
        mSwipeRefreshLayout.post(new Runnable() {

            @Override
            public void run() {
                if (mSwipeRefreshLayout != null) {
                    mSwipeRefreshLayout.setRefreshing(true);
                }
                fetchLocationStores();
            }
        });

        startLocationService();
        barBeans = new ArrayList<>();
        br = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                double lat = intent.getExtras().getDouble("lat");
                double lng = intent.getExtras().getDouble("lng");
                myLatLng = new LatLng(lat, lng);
            }
        };

        new Thread() {
            @Override
            public void run() {
                try {
                    boolean flag = true;
                    while (flag) {
                        if (myLatLng != null) {
                            Thread.sleep(500);
                            fetchLocationStores();
                            flag = false;
                        }
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }.start();
    }

    private void fetchLocationStores() {
        if (myLatLng != null) {
            String location = "location=" + myLatLng.latitude + "," + myLatLng.longitude;
            String radius = "radius=" + sharedPreferences.getInt(BAR_SEARCH_RANGE, 1000);
            String keyword = "keyword=liquor";
            String parameters = location + "&" + radius + "&" + keyword;
            String output = "json";
            String url = "https://maps.googleapis.com/maps/api/place/nearbysearch/" + output + "?" + parameters + "&key=" + getString(R.string.google_maps_key);
            Log.e("URL", url);
            JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, url.replace(" ", "%20"), null, new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(JSONObject response) {
                    try {
                        barBeans.clear();
                        JSONArray results = (JSONArray) response.get("results");
                        for (int i = 0; i < results.length(); i++) {
                            JSONObject bar = (JSONObject) results.get(i);
                            String shop = bar.getString("name");
                            String address = bar.getString("vicinity");
                            float rating = (float) bar.getDouble("rating");
                            boolean open;
                            if (bar.opt("opening_hours") != null) {
                                JSONObject open_hours = (JSONObject) bar.get("opening_hours");
                                open = open_hours.optBoolean("open_now");
                            } else {
                                open = false;
                            }
                            JSONObject location = (JSONObject) ((JSONObject) bar.get("geometry")).get("location");
                            LatLng latLng = new LatLng(location.getDouble("lat"), location.getDouble("lng"));
                            double distance = distance(myLatLng.latitude, myLatLng.longitude, latLng.latitude, latLng.longitude);
                            String placeId = bar.getString("place_id");
                            BarBean barBean = new BarBean(shop, address, rating, open, myLatLng, latLng, placeId, distance);
                            barBeans.add(barBean);
                        }

                        BarAdapter barAdapter = new BarAdapter(barBeans);
                        recyclerView.setHasFixedSize(true);
                        recyclerView.setLayoutManager(new LinearLayoutManager(MainActivity.this));
                        recyclerView.setAdapter(barAdapter);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    mSwipeRefreshLayout.setRefreshing(false);
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    error.printStackTrace();
                    mSwipeRefreshLayout.setRefreshing(false);
                }
            });

            jsonObjectRequest.setRetryPolicy(new DefaultRetryPolicy(
                    10000,
                    DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                    DefaultRetryPolicy.DEFAULT_BACKOFF_MULT)
            );

            RequestQueue requestQueue = Volley.newRequestQueue(MainActivity.this);
            requestQueue.add(jsonObjectRequest);
        }
    }

    private double distance(double lat1, double lon1, double lat2, double lon2) {
        double theta = lon1 - lon2;
        double dist = Math.sin(deg2rad(lat1))
                * Math.sin(deg2rad(lat2))
                + Math.cos(deg2rad(lat1))
                * Math.cos(deg2rad(lat2))
                * Math.cos(deg2rad(theta));
        dist = Math.acos(dist);
        dist = rad2deg(dist);
        dist = dist * 60 * 1.1515;
        return (dist);
    }

    private double deg2rad(double deg) {
        return (deg * Math.PI / 180.0);
    }

    private double rad2deg(double rad) {
        return (rad * 180.0 / Math.PI);
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


    @Override
    protected void onResume() {
        super.onResume();
        fetchLocationStores();
        IntentFilter filter = new IntentFilter("FILTER");
        this.registerReceiver(br, filter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(br);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopLocationService();
    }

    @Override
    public void onRefresh() {
        fetchLocationStores();
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.nav_settings) {
            startActivity(new Intent(MainActivity.this, SettingsActivity.class));
        } else if (id == R.id.nav_about) {
            startActivity(new Intent(MainActivity.this, AboutActivity.class));
        }
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            drawer.openDrawer(GravityCompat.START);
        }
        return super.onOptionsItemSelected(item);
    }
}
