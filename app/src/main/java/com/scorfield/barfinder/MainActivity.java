package com.scorfield.barfinder;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.FrameLayout;

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
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.material.navigation.NavigationView;
import com.scorfield.barfinder.beans.BarAdapter;
import com.scorfield.barfinder.beans.BarBean;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements SwipeRefreshLayout.OnRefreshListener, NavigationView.OnNavigationItemSelectedListener{
    private static final String TAG = "MainActivity";

    private static final String BAR_SHARED_PREFS = "BarCache";
    private static final String BAR_SEARCH_RANGE = "SearchRange";

    SharedPreferences sharedPreferences;

    SwipeRefreshLayout mSwipeRefreshLayout;
    RecyclerView recyclerView;

    DrawerLayout drawer;

    BroadcastReceiver br;
    LatLng myLatLng;

    ArrayList<BarBean> barBeans;

    FrameLayout adContainerView;
    AdView mAdView;

    BarAdapter barAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        sharedPreferences = getSharedPreferences(BAR_SHARED_PREFS, MODE_PRIVATE);

        Toolbar myToolbar = findViewById(R.id.toolbar);
        setSupportActionBar(myToolbar);

        ActionBar actionBar = getSupportActionBar();
        assert actionBar != null;
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

        MobileAds.initialize(this, new OnInitializationCompleteListener() {
            @Override
            public void onInitializationComplete(InitializationStatus initializationStatus) {
                Log.e(TAG, "Ad Init Completed");
            }
        });

        adContainerView = findViewById(R.id.ad_view_container);

        // Since we're loading the banner based on the adContainerView size, we need to wait until this
        // view is laid out before we can get the width.
        adContainerView.post(new Runnable() {
            @Override
            public void run() {
                loadBanner();
            }
        });
    }

    private void fetchLocationStores() {
        if (myLatLng != null) {
            String location = "location=" + myLatLng.latitude + "," + myLatLng.longitude;
            String radius = "radius=" + sharedPreferences.getInt(BAR_SEARCH_RANGE, 1000);
            String keyword = "keyword=liquor";
            String parameters = location + "&" + radius + "&" + keyword;
            String output = "json";
            String url = "https://maps.googleapis.com/maps/api/place/nearbysearch/" + output + "?" + parameters + "&key=" + getString(R.string.google_maps_key);
            RequestQueue mainRequestQueue = Volley.newRequestQueue(MainActivity.this);
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
                            String placeId = bar.getString("place_id");
                            setDistance(myLatLng, latLng, i);
                            BarBean barBean = new BarBean(shop, address, rating, open, myLatLng, latLng, placeId);
                            barBeans.add(barBean);
                        }

                        barAdapter = new BarAdapter(barBeans);
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
            mainRequestQueue.add(jsonObjectRequest);
        }
    }

    private void setDistance(LatLng latLng1, LatLng latLng2, final int index) {
        String origins = latLng1.latitude + "," + latLng1.longitude;
        String destination = latLng2.latitude + "," + latLng2.longitude;
        String params = "?origins=" + origins + "&destinations=" + destination + "&key=" + getString(R.string.google_maps_key);
        String url = "https://maps.googleapis.com/maps/api/distancematrix/json" + params;
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, url.replace(" ", "%20"), null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try {
                    String distance = response.getJSONArray("rows").getJSONObject(0).getJSONArray("elements").getJSONObject(0).getJSONObject("distance").getString("text");
                    BarBean barBean = barBeans.get(index);
                    barBean.setDistance(Double.parseDouble(distance.split(" ")[0]));
                    barBean.setDistanceString(distance);
                    barBeans.set(index, barBean);
                    barAdapter.notifyItemChanged(index);
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

        RequestQueue requestQueue = Volley.newRequestQueue(this);
        requestQueue.add(jsonObjectRequest);
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

        if (mAdView != null) {
            mAdView.resume();
        }
    }

    @Override
    protected void onPause() {
        unregisterReceiver(br);

        if (mAdView != null) {
            mAdView.pause();
        }
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        stopLocationService();
        if (mAdView != null) {
            mAdView.destroy();
        }
        super.onDestroy();
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
        int size = barBeans.size();
        BarBean temp = null;
        switch (item.getItemId()) {
            case android.R.id.home:
                drawer.openDrawer(GravityCompat.START);
                break;
            case R.id.menuAsc:
                for (int i = 0; i < size; i++) {
                    for (int j = 1; j < (size - i); j++) {
                        if (barBeans.get(j - 1).getDistance() > barBeans.get(j).getDistance()) {
                            temp = barBeans.get(j - 1);
                            barBeans.set(j - 1, barBeans.get(j));
                            barBeans.set(j, temp);
                        }
                    }
                }
                barAdapter.notifyDataSetChanged();
                break;
            case R.id.menuDesc:
                for (int i = 0; i < size; i++) {
                    for (int j = 1; j < (size - i); j++) {
                        if (barBeans.get(j - 1).getDistance() < barBeans.get(j).getDistance()) {
                            temp = barBeans.get(j - 1);
                            barBeans.set(j - 1, barBeans.get(j));
                            barBeans.set(j, temp);
                        }
                    }
                }
                barAdapter.notifyDataSetChanged();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.sort_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    private void loadBanner() {
        // Create an ad request. Check your logcat output for the hashed device ID to
        // get test ads on a physical device. e.g.
        // "Use AdRequest.Builder.addTestDevice("ABCDEF012345") to get test ads on this device."
        mAdView = new AdView(this);
        mAdView.setAdUnitId(getString(R.string.ad_unit_id));
        adContainerView.removeAllViews();
        adContainerView.addView(mAdView);

        AdSize adSize = getAdSize();
        mAdView.setAdSize(adSize);

        AdRequest adRequest = new AdRequest.Builder().build();

        // Start loading the ad in the background.
        mAdView.loadAd(adRequest);
    }

    private AdSize getAdSize() {
        // Determine the screen width (less decorations) to use for the ad width.
        Display display = getWindowManager().getDefaultDisplay();
        DisplayMetrics outMetrics = new DisplayMetrics();
        display.getMetrics(outMetrics);

        float density = outMetrics.density;

        float adWidthPixels = adContainerView.getWidth();

        // If the ad hasn't been laid out, default to the full screen width.
        if (adWidthPixels == 0) {
            adWidthPixels = outMetrics.widthPixels;
        }

        int adWidth = (int) (adWidthPixels / density);

        return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(this, adWidth);
    }
}
