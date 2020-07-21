package com.scorfield.barfinder.beans;

import com.google.android.gms.maps.model.LatLng;

public class BarBean {

    private String shop, address;
    private float rating;
    private boolean open;
    private LatLng myLatLng;
    private LatLng storeLatLng;
    private String placeId;
    private double distance;
    private String distanceString;

    public BarBean(String shop, String address, float rating, boolean open, LatLng myLatLng, LatLng storeLatLng, String placeId) {
        this.shop = shop;
        this.address = address;
        this.rating = rating;
        this.open = open;
        this.myLatLng = myLatLng;
        this.storeLatLng = storeLatLng;
        this.placeId = placeId;
    }

    public void setDistanceString(String distanceString) {
        this.distanceString = distanceString;
    }

    public String getDistanceString() {
        return distanceString;
    }

    public void setDistance(double distance) {
        this.distance = distance;
    }

    public double getDistance() {
        return distance;
    }

    public String getPlaceId() {
        return placeId;
    }

    public LatLng getMyLatLng() {
        return myLatLng;
    }


    public LatLng getStoreLatLng() {
        return storeLatLng;
    }


    public String getShop() {
        return shop;
    }


    public String getAddress() {
        return address;
    }


    public float getRating() {
        return rating;
    }


    public boolean isOpen() {
        return open;
    }

}
