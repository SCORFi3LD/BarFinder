package com.scorfield.barfinder;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;

import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.scorfield.barfinder.mapsdirection.FetchURL;
import com.scorfield.barfinder.mapsdirection.TaskLoadedCallback;
import com.scorfield.barfinder.utils.AnimUtils;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, TaskLoadedCallback, View.OnClickListener {

    private GoogleMap mMap;
    private Polyline currentPolyline;
    LatLng myLatLng, storeLatLng;

    AnimUtils animUtils;
    CardView cardView;
    ImageButton btnExpand, btnBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        animUtils = new AnimUtils();

        cardView = findViewById(R.id.card_view);
        btnBack = findViewById(R.id.btn_map_back);
        btnExpand = findViewById(R.id.btn_expand);

        btnBack.setOnClickListener(this);
        btnExpand.setOnClickListener(this);

        Intent in = getIntent();
        myLatLng = (LatLng) in.getExtras().get("me");
        storeLatLng = (LatLng) in.getExtras().get("store");

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }


    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setMyLocationEnabled(true);
        mMap.getUiSettings().setMyLocationButtonEnabled(false);
        mMap.getUiSettings().setRotateGesturesEnabled(false);
        mMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(this, R.raw.maps_theme));

        MarkerOptions options = new MarkerOptions()
                .position(myLatLng)
                .title("Your Location")
                .icon(generateBitmapDescriptorFromRes(this, R.drawable.ic_marker_me));
        mMap.addMarker(options);

        MarkerOptions store = new MarkerOptions()
                .position(storeLatLng)
                .title("Liquor Store")
                .icon(generateBitmapDescriptorFromRes(this, R.drawable.ic_marker_store));
        mMap.addMarker(store);

        String url = getDirectionsUrl(myLatLng, storeLatLng, "driving");
        new FetchURL(MapsActivity.this).execute(url, "walking");
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
        zoomToPolyline(mMap, currentPolyline);
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
}
