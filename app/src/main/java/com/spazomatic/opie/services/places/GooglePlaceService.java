package com.spazomatic.opie.services.places;

import android.location.Location;

import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * Created by samuelsegal on 12/30/15.
 */
public interface GooglePlaceService {
    //@GET("/maps/api/place/nearbysearch/json")
    //Call<Place> searchPlaces(
    //        @QueryMap Map<String, String> options);
    List<PlacePojo> searchPlaces(String keyWord, Location location, String radius, boolean chooseOnlyOpen) throws ExecutionException, InterruptedException;
}
