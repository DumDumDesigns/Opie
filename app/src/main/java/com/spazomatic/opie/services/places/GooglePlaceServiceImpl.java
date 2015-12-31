package com.spazomatic.opie.services.places;

import android.location.Location;

import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * Created by samuelsegal on 12/30/15.
 */
public class GooglePlaceServiceImpl implements GooglePlaceService {

    private static final String LOG_TAG = GooglePlaceServiceImpl.class.getName();

    @Override
    public List<PlacePojo> searchPlaces(String keyWord, Location location, String radius, boolean chooseOnlyOpen)
            throws ExecutionException, InterruptedException {

        RestTask restTask = new RestTask();
        restTask.execute(keyWord, location, radius, chooseOnlyOpen);
        List<PlacePojo> placesFound = restTask.get();

        return placesFound;
    }
}
