package com.scorfield.barfinder;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
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

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, TaskLoadedCallback, View.OnClickListener {

    private GoogleMap mMap;
    private Polyline currentPolyline;
    LatLng myLatLng, storeLatLng;
    String placeId;

    AnimUtils animUtils;
    CardView cardView;
    ImageButton btnExpand, btnBack, btnCall, btnWeb;

    TextView storeName, address, closed, rating;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
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
                            }else{
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
