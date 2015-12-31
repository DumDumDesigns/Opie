package com.spazomatic.opie;

import android.Manifest;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.location.places.ui.PlacePicker;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.spazomatic.opie.services.places.GooglePlaceServiceImpl;
import com.spazomatic.opie.services.places.PlacePojo;

import java.util.List;

public class MainActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, LocationListener {

    private static final String LOG_TAG = MainActivity.class.getName();
    private GoogleApiClient googleApiClient;
    // Request code to use when launching the resolution activity
    private static final int REQUEST_RESOLVE_ERROR = 1001;
    // Unique tag for the error dialog fragment
    private static final String DIALOG_ERROR = "dialog_error";
    // Bool to track whether the app is already resolving an error
    private boolean mResolvingError = false;
    private static final String STATE_RESOLVING_ERROR = "resolving_error";
    private static final int PLACE_PICKER_REQUEST = 1;
    private EditText searchCriteria;
    private CheckBox opennowBox;
    private Button searchBtn;
    private LocationRequest locationRequest;
    private GoogleMap map;
    private Location currentLocation;
    private boolean chooseOnlyOpen;
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(STATE_RESOLVING_ERROR, mResolvingError);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mResolvingError = savedInstanceState != null
                && savedInstanceState.getBoolean(STATE_RESOLVING_ERROR, false);

        googleApiClient = new GoogleApiClient
                .Builder(this)
                .addApi(Places.GEO_DATA_API)
                .addApi(Places.PLACE_DETECTION_API)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
        searchCriteria = (EditText) findViewById(R.id.editText);
        opennowBox = (CheckBox) findViewById(R.id.isOpen);
        opennowBox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(((CheckBox)v).isChecked()){
                    chooseOnlyOpen = true;
                }else{
                    chooseOnlyOpen = false;
                }
                findPlaces();
            }
        });
        searchBtn = (Button) findViewById(R.id.button);
        searchBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                searchCriteria.onEditorAction(EditorInfo.IME_ACTION_DONE);
                findPlaces();
            }
        });
        map = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map))
                .getMap();
        locationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(10 * 1000)        // 10 seconds, in milliseconds
                .setFastestInterval(1 * 1000); // 1 second, in milliseconds
    }

    private void findPlaces() {

        try {
            sendGet(searchCriteria.getText().toString());
        } catch (Exception e) {
            Log.e(LOG_TAG, "Error calling google rest service: " + e.getMessage(), e);
        }

    }

    // HTTP GET request
    private void sendGet(String searchWord) throws Exception {
        map.clear();
        GooglePlaceServiceImpl placesService = new GooglePlaceServiceImpl();
        List<PlacePojo> placesFound = placesService.searchPlaces(
                searchWord, currentLocation, "5000", chooseOnlyOpen);
        for (PlacePojo pp : placesFound) {
            MarkerOptions options = new MarkerOptions()
                    .position(new LatLng(pp.getLocation().getLatitude(), pp.getLocation().getLongitude()))
                    .title(pp.getName());
            map.addMarker(options);
            // map.moveCamera(CameraUpdateFactory.newLatLng(latLng));
        }


    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.e(LOG_TAG, "connecting");
        googleApiClient.connect();

    }

    @Override
    protected void onPause() {
        super.onPause();
        if (googleApiClient.isConnected()) {
            LocationServices.FusedLocationApi.removeLocationUpdates(googleApiClient, this);
            googleApiClient.disconnect();
        }
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        Log.e(LOG_TAG, "GooglePlayService connection failed");
        if (mResolvingError) {
            // Already attempting to resolve an error.
            return;
        } else if (result.hasResolution()) {
            try {
                mResolvingError = true;
                result.startResolutionForResult(this, REQUEST_RESOLVE_ERROR);
            } catch (IntentSender.SendIntentException e) {
                // There was an error with the resolution intent. Try again.
                googleApiClient.connect();
            }
        } else {
            // Show dialog using GoogleApiAvailability.getErrorDialog()
            showErrorDialog(result.getErrorCode());
            mResolvingError = true;
        }
    }

    // The rest of this code is all about building the error dialog

    /* Creates a dialog for an error message */
    private void showErrorDialog(int errorCode) {
        // Create a fragment for the error dialog
        ErrorDialogFragment dialogFragment = new ErrorDialogFragment();
        // Pass the error that should be displayed
        Bundle args = new Bundle();
        args.putInt(DIALOG_ERROR, errorCode);
        dialogFragment.setArguments(args);
        dialogFragment.show(getSupportFragmentManager(), "errordialog");
    }

    /* Called from ErrorDialogFragment when the dialog is dismissed. */
    public void onDialogDismissed() {
        mResolvingError = false;
    }

    @Override
    public void onLocationChanged(Location location) {
        handleNewLocation(location);
    }

    /* A fragment to display an error dialog */
    public static class ErrorDialogFragment extends DialogFragment {
        public ErrorDialogFragment() {
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            // Get the error code and retrieve the appropriate dialog
            int errorCode = this.getArguments().getInt(DIALOG_ERROR);
            return GoogleApiAvailability.getInstance().getErrorDialog(
                    this.getActivity(), errorCode, REQUEST_RESOLVE_ERROR);
        }

        @Override
        public void onDismiss(DialogInterface dialog) {
            ((MainActivity) getActivity()).onDialogDismissed();
        }
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Log.e(LOG_TAG, "Connected to google play service api");
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        currentLocation = LocationServices.FusedLocationApi.getLastLocation(googleApiClient);
        if (currentLocation == null) {
            LocationServices.FusedLocationApi.requestLocationUpdates(
                    googleApiClient, locationRequest, this);
        } else {
            handleNewLocation(currentLocation);
        }
    }

    private void handleNewLocation(Location location) {
        Log.d(LOG_TAG, location.toString());

        double currentLatitude = location.getLatitude();
        double currentLongitude = location.getLongitude();
        LatLng latLng = new LatLng(currentLatitude, currentLongitude);

        MarkerOptions options = new MarkerOptions()
                .position(latLng)
                .title("I am here!");
        map.addMarker(options);
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 10));
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        map.setMyLocationEnabled(true);
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.e(LOG_TAG,"Connection suspended to google play service api");
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.e(LOG_TAG,"RequestCode: " + requestCode + " ResultCode: " + resultCode);
        if (requestCode == REQUEST_RESOLVE_ERROR) {
            mResolvingError = false;

            if (resultCode == RESULT_OK) {
                // Make sure the app is not already connected or attempting to connect
                if (!googleApiClient.isConnecting() &&
                        !googleApiClient.isConnected()) {
                    googleApiClient.connect();
                }
            }
        }
        if (requestCode == PLACE_PICKER_REQUEST) {
            if (resultCode == RESULT_OK) {
                Place place = PlacePicker.getPlace(data, this);
                String toastMsg = String.format("Place: %s", place.getName());
                Toast.makeText(this, toastMsg, Toast.LENGTH_LONG).show();
            }else{
                Log.e(LOG_TAG,"ERROR With PlacePicker Intent");
            }
        }
    }
}
/*
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://maps.googleapis.com")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        GooglePlaceService service = retrofit.create(GooglePlaceService.class);
        Map<String,String> queryOptions = new HashMap<>();
        queryOptions.put("location", "-33.8670522,151.1957362");
        queryOptions.put("radius", "500");
        queryOptions.put("types","food");
        queryOptions.put("key", "AIzaSyDsOXVu6Y5M9MbdISovoVI6EfesIV6TF4Q");
        Call<Place> places = service.searchPlaces(queryOptions);
        Log.e(LOG_TAG,"Places: " + places);
        places.enqueue(new Callback<Place>() {
            @Override
            public void onResponse(retrofit.Response<Place> response, Retrofit retrofit) {
                Log.e(LOG_TAG, "response: " + response);
            }

            @Override
            public void onFailure(Throwable t) {
                Log.e(LOG_TAG, "FAILED UR A FAILURE!!!");
            }
        });
*/
       /* try {
            PlacePicker.IntentBuilder intentBuilder = new PlacePicker.IntentBuilder();
            //LatLngBounds llb = new LatLngBounds.Builder().include(new LatLng(32.0167, 81.1167) ).build();

            Intent intent = intentBuilder.build(this);
            // Start the Intent by requesting a result, identified by a request code.
            startActivityForResult(intent, PLACE_PICKER_REQUEST);

            // Hide the pick option in the UI to prevent users from starting the picker
            // multiple times.
            //showPickAction(true);

        } catch (GooglePlayServicesRepairableException e) {
            GooglePlayServicesUtil
                    .getErrorDialog(e.getConnectionStatusCode(), this, 0);
        } catch (GooglePlayServicesNotAvailableException e) {
            Toast.makeText(this, "Google Play Services is not available.",
                    Toast.LENGTH_LONG)
                    .show();
        }
        */