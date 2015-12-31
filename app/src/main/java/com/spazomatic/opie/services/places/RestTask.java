package com.spazomatic.opie.services.places;

import android.location.Location;
import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by samuelsegal on 12/30/15.
 */
public class RestTask extends AsyncTask<Object, Void, List<PlacePojo>> {

    private final static String LOG_TAG = RestTask.class.getName();

    @Override
    protected List<PlacePojo> doInBackground(Object... params) {
        Location loc = (Location)params[1];
        double lat = loc.getLatitude();
        double lon = loc.getLongitude();
        String location = String.valueOf(lat) + "," + String.valueOf(lon);// "-33.8670522,151.1957362";
        String radius = (String)params[2];
        String query = (String)params[0];
        String key = "AIzaSyCIeLg_4u-kHTkgUhd9qjFXOnawR5rp-r0";
        Boolean chooseOnlyOpen = (Boolean)params[3];

        String url = String.format(
                "https://maps.googleapis.com/maps/api/place/nearbysearch/json?"+
                        "location=%s&radius=%s&keyword=%s&key=%s%s",
                        location,radius,query,key,chooseOnlyOpen ? "&opennow" : "");
        try {
            URL obj = new URL(url);
            HttpURLConnection con = (HttpURLConnection) obj.openConnection();
            Log.e(LOG_TAG,"url: " + url);
            // optional default is GET
            con.setRequestMethod("GET");

            //add request header
            //con.setRequestProperty("User-Agent", USER_AGENT);

            int responseCode = con.getResponseCode();
            System.out.println("\nSending 'GET' request to URL : " + url);
            Log.d(LOG_TAG,"Response Code : " + responseCode);

            BufferedReader in = new BufferedReader(
                    new InputStreamReader(con.getInputStream()));
            String inputLine;
            StringBuffer response = new StringBuffer();

            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();

            Log.e(LOG_TAG, response.toString());

            JSONObject reader = new JSONObject(response.toString());
            JSONArray results = reader.optJSONArray("results");
            List<PlacePojo> placesList = new ArrayList<>();
            for(int i = 0; i < results.length(); i++){
                JSONObject place = results.getJSONObject(i);
                String name = place.optString("name");
                JSONObject geometry = place.getJSONObject("geometry");
                JSONObject placeLoc = geometry.getJSONObject("location");
                String placeLat = placeLoc.optString("lat");
                String placeLon = placeLoc.optString("lng");
                String latitude = place.optString("geometry.location.lat");
                Log.e(LOG_TAG,"Name of place: " + name);
                Log.e(LOG_TAG,"Lat/Lon: " + placeLat + "/" + placeLon);
               // LatLng latLng = new LatLng(Double.valueOf(placeLat), Double.valueOf(placeLon));
                Location l = new Location("google");
                l.setLatitude(Double.valueOf(placeLat));
                l.setLongitude( Double.valueOf(placeLon));
                PlacePojo pp = new PlacePojo(l,name);
                placesList.add(pp);
            }

            return placesList;
        } catch (Exception e) {
            Log.e(LOG_TAG,"ERROR in REST Task: " + e.getMessage(),e);
        }
        return null;
    }
}
