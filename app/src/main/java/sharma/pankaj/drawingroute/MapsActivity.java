package sharma.pankaj.drawingroute;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.PopupMenu;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;


import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.widget.AutocompleteSupportFragment;
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;


import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import es.dmoral.toasty.Toasty;
import retrofit2.Call;
import sharma.pankaj.drawingroute.databinding.ActivityMapsBinding;
import sharma.pankaj.drawingroute.model.RouteModel;
import sharma.pankaj.drawingroute.utils.Constants;
import sharma.pankaj.drawingroute.utils.ModeIcon;
import sharma.pankaj.drawingroute.utils.PreferenceManager;
import sharma.pankaj.drawingroute.webservices.ResponseHandler;
import sharma.pankaj.drawingroute.webservices.ServiceInterface;
import sharma.pankaj.drawingroute.webservices.ServiceWrapper;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private ActivityMapsBinding binding;
    private GoogleMap mMap;
    private static final String TAG = "MapsActivity";

    private FusedLocationProviderClient fusedLocationProviderClient;
    private LocationRequest locationRequest;
    protected static final int REQUEST_CHECK_SETTINGS = 909;
    private  Marker locationMarker = null;
    private  Marker destinationMarker = null;
    private LatLng destination = null;
    private LatLng source =  null;
    private String[] mode = {"driving", "walking", "bicycling", "transit"};
    LatLng latLsng;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMapsBinding.inflate(getLayoutInflater()); //setContentView(R.layout.activity_maps);
        View view = binding.getRoot();
        setContentView(view);
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(MapsActivity.this);
        if (!Places.isInitialized()) {
            Places.initialize(getApplicationContext(), getString(R.string.google_maps_key), Locale.getDefault());
        }
        AutoCompletePlaceSearch();
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        assert mapFragment != null;
        mapFragment.getMapAsync(this);
        binding.mode.setImageDrawable(ResourcesCompat.getDrawable(getResources(),
                ModeIcon.icon[PreferenceManager.getInt(MapsActivity.this, Constants.MODE)],
                this.getTheme()));
        binding.mode.setOnClickListener(this::showRouteOption);
        binding.drawRoute.setOnClickListener(v->{
            if (source!=null && destination!=null ) {
                Log.e(TAG, "location coordinate: \n" + destination.longitude+"\t"+destination.longitude+"\n"+source.latitude+"\n"+source.longitude);
                drawRoute(source, destination);
            }else {
                Toasty.error(MapsActivity.this, "Please enter destination location", Toasty.LENGTH_LONG).show();
            }
        });
    }


    private void AutoCompletePlaceSearch() {
        AutocompleteSupportFragment autocompleteFragment = (AutocompleteSupportFragment)
                getSupportFragmentManager().findFragmentById(R.id.search_result);
        assert autocompleteFragment != null;
        autocompleteFragment.setPlaceFields(Arrays.asList(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG));
        autocompleteFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(@NonNull Place place) {
                if (place.getLatLng() != null) {
                    destination = place.getLatLng();
                    setDestinationMarker(place.getLatLng());
                }
            }


            @Override
            public void onError(@NonNull Status status) {
                // TODO: Handle the error.
                Log.i(TAG, "An error occurred: " + status);
            }
        });
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.getUiSettings().setMapToolbarEnabled(false);
        if (isLocationEnabled()){ getLocation();}

    }


    @Override
    protected void onResume() {
        super.onResume();
        if (isLocationEnabled()){ getLocation();}
    }

    private void getLocation() {
        locationRequest = new LocationRequest();
        locationRequest.setFastestInterval(4000);
        locationRequest.setInterval(6000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(locationRequest);
        builder.setAlwaysShow(true);
        Task<LocationSettingsResponse> task = LocationServices.getSettingsClient(this).checkLocationSettings(builder.build());
        task.addOnCompleteListener(new OnCompleteListener<LocationSettingsResponse>() {
            @Override
            public void onComplete(@NonNull Task<LocationSettingsResponse> task) {
                try {
                    LocationSettingsResponse response = task.getResult(ApiException.class);
                    // All location settings are satisfied. The client can initialize location
                    // requests here.
                    if (ContextCompat.checkSelfPermission(MapsActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

                        fusedLocationProviderClient.requestLocationUpdates(locationRequest, new LocationCallback() {
                            @Override
                            public void onLocationResult(LocationResult locationResult) {
                                for (Location location : locationResult.getLocations()) {
                                    source = new LatLng(location.getLatitude(), location.getLongitude());
                                    setMarker(source);
                                }

                            }
                        }, getMainLooper());

                    }
                } catch (ApiException exception) {
                    switch (exception.getStatusCode()) {
                        case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                            // Location settings are not satisfied. But could be fixed by showing the
                            // user a dialog.
                            try {
                                // Cast to a resolvable exception.
                                ResolvableApiException resolvable = (ResolvableApiException) exception;
                                // Show the dialog by calling startResolutionForResult(),
                                // and check the result in onActivityResult().
                                resolvable.startResolutionForResult(MapsActivity.this, 2001);
                                break;
                            } catch (IntentSender.SendIntentException e) {
                                // Ignore the error.
                            } catch (ClassCastException e) {
                                // Ignore, should be an impossible error.
                            }
                            break;
                        case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                            // Location settings are not satisfied. However, we have no way to fix the
                            // settings so we won't show the dialog.

                            break;
                    }
                }
            }
        });

    }

    private void drawRoute(LatLng origin, LatLng dest){
        String str_origin = "origin=" + origin.latitude + "," + origin.longitude;
        String str_dest = "destination=" + dest.latitude + "," + dest.longitude;
        String sensor = "sensor=false";
        String mMode = "mode="+mode[PreferenceManager.getInt(MapsActivity.this, Constants.MODE)];
        String parameters = str_origin + "&" + str_dest + "&" + sensor + "&" + mMode;
        String output = "json";
        String key = "key="+getResources().getString(R.string.google_maps_key);
        String subUrl = output + "?" + parameters + "&" + key;
        Log.e(TAG, "drawRoute: " + subUrl );
        ServiceWrapper serviceWrapper = new ServiceWrapper(MapsActivity.this);
        Call<RouteModel> call = serviceWrapper.getRetrofit().create(ServiceInterface.class)
                .getRoute(subUrl);
        serviceWrapper.HandleResponse(call, new ResponseHandler<RouteModel>(MapsActivity.this) {
            @Override
            public void onResponse(RouteModel response) {

                if (response.getStatus().equalsIgnoreCase("OK") && !response.getRoutes().isEmpty()){
                    PolylineOptions polylineOptions = new PolylineOptions();
                    ArrayList<LatLng> points  = new ArrayList<>();
                    for (int i =0; i< response.getRoutes().get(0).getLegs().get(0).getSteps().size(); i++){
                        List list = decodePoly(response.getRoutes().get(0).getLegs().get(0).getSteps().get(i).getPolyline().getPoints());

                        Log.e(TAG, "onResponse: " + "=================================" );
                        for(int j=0;j <list.size();j++){
                            points.add(new LatLng(((LatLng)list.get(j)).latitude, ((LatLng)list.get(j)).longitude));
                            Log.e(TAG, "onResponse:    " +((LatLng)list.get(j)).latitude +"\t "+((LatLng)list.get(j)).longitude);
                        }
                    }
                    polylineOptions.addAll(points);
                    mMap.clear();
                    mMap.addPolyline(polylineOptions);
                    setMarker(points.get(0));
                    setDestinationMarker(points.get(points.size()-1));
                }else {
                    Toasty.info(MapsActivity.this, ""+response.getStatus(), Toasty.LENGTH_LONG).show();
                }
            }
        });


   }
    /**
     * Method to decode polyline points
     * Courtesy : http://jeffreysambells.com/2010/05/27/decoding-polylines-from-google-maps-direction-api-with-java
     * */
    private List decodePoly(String encoded) {

        List poly = new ArrayList();
        int index = 0, len = encoded.length();
        int lat = 0, lng = 0;

        while (index < len) {
            int b, shift = 0, result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lat += dlat;

            shift = 0;
            result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lng += dlng;

            LatLng p = new LatLng((((double) lat / 1E5)),
                    (((double) lng / 1E5)));
            poly.add(p);
        }

        return poly;
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 2001) {
            if (resultCode == Activity.RESULT_OK) {
                assert data != null;
                String result = data.getStringExtra("result");
            }
            if (resultCode == Activity.RESULT_CANCELED) {
                //Write your code if there's no result
            }
        }
    }

    private void setDestinationMarker(LatLng latLng) {
        if (destinationMarker!=null){
            destinationMarker.remove();
        }
        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(latLng);
        markerOptions.title(latLng.latitude + " : " + latLng.longitude);
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 16));
        destinationMarker = mMap.addMarker(markerOptions);
        destinationMarker.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.ic_destination));
    }

    private void setMarker(LatLng location) {
        if (locationMarker!=null){
            locationMarker.remove();
        }
        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(location);
        markerOptions.title(location.latitude + " : " + location.longitude);
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(location, 16));
        locationMarker = mMap.addMarker(markerOptions);
        locationMarker.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.ic_location));
    }

    //route mode
    void showRouteOption(View view) {
        PopupMenu popup = new PopupMenu(MapsActivity.this, view);
        try {
            Field[] fields = popup.getClass().getDeclaredFields();
            for (Field field : fields) {
                if ("mPopup".equals(field.getName())) {
                    field.setAccessible(true);
                    Object menuPopupHelper = field.get(popup);
                    assert menuPopupHelper != null;
                    Class<?> classPopupHelper = Class.forName(menuPopupHelper.getClass().getName());
                    Method setForceIcons = classPopupHelper.getMethod("setForceShowIcon", boolean.class);
                    setForceIcons.invoke(menuPopupHelper, true);
                    break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        popup.getMenuInflater().inflate(R.menu.menu_mode, popup.getMenu());
        popup.setOnMenuItemClickListener(item -> {
            if (item.getTitle().toString().equalsIgnoreCase(getString(R.string.driving))) {
                getIcon(0);
            } else if (item.getTitle().toString().equalsIgnoreCase(getString(R.string.walking))) {
                getIcon(1);
            } else if (item.getTitle().toString().equalsIgnoreCase(getString(R.string.bicycling))) {
                getIcon(2);
            } else {
                getIcon(3);
            }
            return true;
        });
        popup.show();
    }

    //set route/mode icon
    private void getIcon(int id) {
        PreferenceManager.saveInt(MapsActivity.this, Constants.MODE, id);
        binding.mode.setImageDrawable(ResourcesCompat.getDrawable(getResources(),
                ModeIcon.icon[PreferenceManager.getInt(MapsActivity.this, Constants.MODE)],
                this.getTheme()));
    }

    // check permission
    private boolean isLocationEnabled() {
        final boolean[] isEnabled = {false};
        Dexter.withContext(this)
                .withPermissions(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                ).withListener(new MultiplePermissionsListener() {
            @Override
            public void onPermissionsChecked(MultiplePermissionsReport report) {
                if (report.areAllPermissionsGranted()) {
                    isEnabled[0] = true;
                } else {
                    isEnabled[0] = false;
                }
            }

            @Override
            public void onPermissionRationaleShouldBeShown(List<PermissionRequest> permissions, PermissionToken token) {/* ... */}
        }).check();

        return isEnabled[0];
    }
}