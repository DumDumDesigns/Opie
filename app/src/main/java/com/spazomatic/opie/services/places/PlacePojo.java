package com.spazomatic.opie.services.places;

import android.location.Location;

/**
 * Created by samuelsegal on 12/30/15.
 */
public class PlacePojo {

    private Location location;
    private String name;

    public PlacePojo(Location location, String name) {
        this.location = location;
        this.name = name;
    }

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
