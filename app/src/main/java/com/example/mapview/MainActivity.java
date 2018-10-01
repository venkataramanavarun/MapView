package com.example.mapview;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Address;
import android.location.Criteria;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;

import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlaceAutocomplete;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    int PLACE_AUTOCOMPLETE_REQUEST_CODE_TO = 2;
    MapView mMapView;
    Context mContext;
    ArrayList<LatLng> points;
    String selectedCateTye = "Emergency";

    boolean firstTimeBook = false;
    String selectedPrice = "";
    EditText from_lcation_textView, to_lcation_textView;
    ProgressBar progressBar;
    View includeCallSupport, includeBook;
    int PLACE_AUTOCOMPLETE_REQUEST_CODE_FROM = 1;
    private GoogleMap googleMap;
    private LatLng toPosition, fromPosition;
    private PolylineOptions tempLineOptions;
    private String orderedJobId;
    private String walletAmount = "";
    private String paymentMode;
    private String cashTobePaid = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mContext = this;

        mMapView = findViewById(R.id.mapView);

        from_lcation_textView = findViewById(R.id.from_lcation_textView);
        to_lcation_textView = findViewById(R.id.to_lcation_textView);
        Bundle bundle = getIntent().getParcelableExtra("bundle");


        mMapView.onCreate(savedInstanceState);
        mMapView.onResume(); // needed to get the map to display immediately


        from_lcation_textView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    Intent intent =
                            new PlaceAutocomplete.IntentBuilder(PlaceAutocomplete.MODE_FULLSCREEN)
                                    .build((Activity) mContext);
                    startActivityForResult(intent, PLACE_AUTOCOMPLETE_REQUEST_CODE_FROM);
                } catch (GooglePlayServicesRepairableException e) {
                    // TODO: Handle the error.
                } catch (GooglePlayServicesNotAvailableException e) {
                    // TODO: Handle the error.
                }

            }
        });

        to_lcation_textView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    Intent intent =
                            new PlaceAutocomplete.IntentBuilder(PlaceAutocomplete.MODE_FULLSCREEN)
                                    .build((Activity) mContext);
                    startActivityForResult(intent, PLACE_AUTOCOMPLETE_REQUEST_CODE_TO);
                } catch (GooglePlayServicesRepairableException e) {
                    // TODO: Handle the error.
                } catch (GooglePlayServicesNotAvailableException e) {
                    // TODO: Handle the error.
                }

            }
        });


        mMapView.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(GoogleMap mMap) {
                googleMap = mMap;


                try {
                    // For showing a move to my location button
                    if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        // TODO: Consider calling
                        //    ActivityCompat#requestPermissions
                        // here to request the missing permissions, and then overriding
                        //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                        //                                          int[] grantResults)
                        // to handle the case where the user grants the permission. See the documentation
                        // for ActivityCompat#requestPermissions for more details.


                        return;
                    }


                    LocationManager locationManager = (LocationManager) mContext.getSystemService(Context.LOCATION_SERVICE);
                    Criteria criteria = new Criteria();


                    Location location = locationManager.getLastKnownLocation(locationManager.getBestProvider(criteria, false));
                    if (location != null) {
                        String lat = String.valueOf(location.getLatitude());
                        String lng = String.valueOf(location.getLongitude());


                        fromPosition = new LatLng(Double.parseDouble(lat), Double.parseDouble(lng));

                        from_lcation_textView.setText(getAddressFromLatLng(fromPosition));


                    }


                    ArrayList<LatLng> list = new ArrayList<>();
                    list.add(fromPosition);
                    list.add(toPosition);


                   PolylineOptions options = new PolylineOptions().width(5).color(Color.BLUE).geodesic(true);
                    for (int z = 0; z < list.size(); z++) {
                        LatLng point = list.get(z);
                        options.add(point);
                    }
                    googleMap.addPolyline(options);


                    MarkerOptions marker2 = new MarkerOptions().position(fromPosition);
                    MarkerOptions marker1 = new MarkerOptions().position(toPosition);
                    marker1.icon(BitmapDescriptorFactory.fromResource(R.drawable.location_on_black));
                    marker2.icon(BitmapDescriptorFactory.fromResource(R.drawable.location_on_black));

                    MarkerOptions[] markers = {marker2, marker1};

                    googleMap.addMarker(marker2);
                    googleMap.addMarker(marker1);

                    String url = getUrl(fromPosition, toPosition);
                    Log.d("onMapClick", url.toString());
                    FetchUrl FetchUrl = new FetchUrl();
                    // Start downloading json data from Google Directions API
                    FetchUrl.execute(url);


                    LatLngBounds.Builder builder = new LatLngBounds.Builder();
                    for (MarkerOptions marker : markers) {
                        builder.include(marker.getPosition());
                    }
                    LatLngBounds bounds = builder.build();

                    int width = getResources().getDisplayMetrics().widthPixels;
                    int height = getResources().getDisplayMetrics().heightPixels;
                    int padding = (int) (width * 0.12);


                    // offset from edges of the map in pixels
                    CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(bounds, width, height, padding);
                    googleMap.moveCamera(cu);
                    googleMap.animateCamera(cu);
                } catch (Exception e) {
                }

            }
        });

    }

    private String getAddressFromLatLng(LatLng latLng) {
        Geocoder geocoder;
        List<Address> addresses;
        geocoder = new Geocoder(mContext, Locale.getDefault());
        String addressLoc = "";

        try {
            addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1); // Here 1 represent max location result to returned, by documents it recommended 1 to 5

            String address = addresses.get(0).getAddressLine(0); // If any additional address line present than only, check with max available address lines by getMaxAddressLineIndex()

            addressLoc = address;
        } catch (IOException e) {
            e.printStackTrace();
        }

        return addressLoc;

    }

    private String getUrl(LatLng origin, LatLng dest) {

        // Origin of route
        String str_origin = "origin=" + origin.latitude + "," + origin.longitude;

        // Destination of route
        String str_dest = "destination=" + dest.latitude + "," + dest.longitude;


        // Sensor enabled
        String sensor = "sensor=false";

        // Building the parameters to the web service
        String parameters = str_origin + "&" + str_dest + "&" + sensor;

        // Output format
        String output = "json";

        // Building the url to the web service
        String url = "https://maps.googleapis.com/maps/api/directions/" + output + "?" + parameters + "&key="
                + mContext.getResources().getString(R.string.google_api);


        return url;
    }

    private class FetchUrl extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... url) {

            // For storing data from web service
            String data = "";

            try {
                // Fetching the data from web service
                data = downloadUrl(url[0]);
                Log.d("Background Task data", data.toString());
            } catch (Exception e) {
                Log.d("Background Task", e.toString());
            }
            return data;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);

            ParserTask parserTask = new ParserTask();

            // Invokes the thread for parsing the JSON data
            parserTask.execute(result);

        }
    }

    /**
     * A class to parse the Google Places in JSON format
     */
    private class ParserTask extends AsyncTask<String, Integer, List<List<HashMap<String, String>>>> {


        // Parsing the data in non-ui thread
        @Override
        protected List<List<HashMap<String, String>>> doInBackground(String... jsonData) {

            JSONObject jObject;
            List<List<HashMap<String, String>>> routes = null;

            try {
                jObject = new JSONObject(jsonData[0]);
                Log.d("ParserTask", jsonData[0].toString());
                DataParser parser = new DataParser();
                Log.d("ParserTask", parser.toString());

                // Starts parsing data
                routes = parser.parse(jObject);
                Log.d("ParserTask", "Executing routes");
                Log.d("ParserTask", routes.toString());

            } catch (Exception e) {
                Log.d("ParserTask", e.toString());
                e.printStackTrace();
            }
            return routes;
        }

        // Executes in UI thread, after the parsing process
        @Override
        protected void onPostExecute(List<List<HashMap<String, String>>> result) {

            PolylineOptions lineOptions = null;


            // Traversing through all the routes
            if (result != null && result.size() > 0) {
                for (int i = 0; i < result.size(); i++) {
                    points = new ArrayList<>();
                    lineOptions = new PolylineOptions();

                    // Fetching i-th route
                    List<HashMap<String, String>> path = result.get(i);

                    // Fetching all the points in i-th route
                    for (int j = 0; j < path.size(); j++) {
                        HashMap<String, String> point = path.get(j);

                        double lat = Double.parseDouble(point.get("lat"));
                        double lng = Double.parseDouble(point.get("lng"));
                        LatLng position = new LatLng(lat, lng);

                        points.add(position);
                    }

                    // Adding all the points in the route to LineOptions
                    lineOptions.addAll(points);
                    lineOptions.width(12);
                    lineOptions.color(Color.BLUE);

                    Log.d("onPostExecute", "onPostExecute lineoptions decoded");

                }
            }
            // Drawing polyline in the Google Map for the i-th route
            if (lineOptions != null) {
                tempLineOptions = lineOptions;

                googleMap.addPolyline(lineOptions);
            } else {
                Log.d("onPostExecute", "without Polylines drawn");
            }
        }
    }

    private String downloadUrl(String strUrl) throws IOException {
        String data = "";
        InputStream iStream = null;
        HttpURLConnection urlConnection = null;
        try {
            URL url = new URL(strUrl);

            // Creating an http connection to communicate with url
            urlConnection = (HttpURLConnection) url.openConnection();

            // Connecting to url
            urlConnection.connect();

            // Reading data from url
            iStream = urlConnection.getInputStream();

            BufferedReader br = new BufferedReader(new InputStreamReader(iStream));

            StringBuffer sb = new StringBuffer();

            String line = "";
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }

            data = sb.toString();
            Log.d("downloadUrl", data.toString());
            br.close();

        } catch (Exception e) {
            Log.d("Exception", e.toString());
        } finally {
            iStream.close();
            urlConnection.disconnect();
        }
        return data;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PLACE_AUTOCOMPLETE_REQUEST_CODE_FROM) {
            if (resultCode == RESULT_OK) {
                Place place = PlaceAutocomplete.getPlace(mContext, data);
                from_lcation_textView.setText(place.getAddress());
                fromPosition = place.getLatLng();


                googleMap.clear();
                selectedPrice = "";


                MarkerOptions marker2 = new MarkerOptions().position(fromPosition);

                marker2.icon(BitmapDescriptorFactory.fromResource(R.drawable.maplo));
                googleMap.addMarker(marker2);


                if (toPosition != null) {
                    MarkerOptions marker1 = new MarkerOptions().position(toPosition);
                    marker1.icon(BitmapDescriptorFactory.fromResource(R.drawable.maplo));
                    googleMap.addMarker(marker1);

                    MarkerOptions[] markers = {marker2, marker1};
                    String url = getUrl(fromPosition, toPosition);
                    Log.d("onMapClick", url.toString());
                    FetchUrl FetchUrl = new FetchUrl();
                    // Start downloading json data from Google Directions API
                    FetchUrl.execute(url);


                    LatLngBounds.Builder builder = new LatLngBounds.Builder();
                    for (MarkerOptions marker : markers) {
                        builder.include(marker.getPosition());
                    }
                    LatLngBounds bounds = builder.build();

                    int padding = 50; // offset from edges of the map in pixels0
                    CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(bounds, padding);
                    googleMap.moveCamera(cu);
                    googleMap.animateCamera(cu);

                }


            } else if (resultCode == PlaceAutocomplete.RESULT_ERROR) {
                Status status = PlaceAutocomplete.getStatus(mContext, data);
                // TODO: Handle the error.


            } else if (resultCode == RESULT_CANCELED) {
                // The user canceled the operation.
            }
        }else if (requestCode == PLACE_AUTOCOMPLETE_REQUEST_CODE_TO) {
            if (resultCode == RESULT_OK) {
                Place place = PlaceAutocomplete.getPlace(mContext, data);
                to_lcation_textView.setText(place.getAddress());
                toPosition = place.getLatLng();


                googleMap.clear();
                selectedPrice = "";


                MarkerOptions marker2 = new MarkerOptions().position(toPosition);

                marker2.icon(BitmapDescriptorFactory.fromResource(R.drawable.maplo));
                googleMap.addMarker(marker2);


                if (fromPosition != null) {
                    MarkerOptions marker1 = new MarkerOptions().position(fromPosition);
                    marker1.icon(BitmapDescriptorFactory.fromResource(R.drawable.maplo));
                    googleMap.addMarker(marker1);

                    MarkerOptions[] markers = {marker2, marker1};
                    String url = getUrl(fromPosition, toPosition);
                    Log.d("onMapClick", url.toString());
                    FetchUrl FetchUrl = new FetchUrl();
                    // Start downloading json data from Google Directions API
                    FetchUrl.execute(url);


                    LatLngBounds.Builder builder = new LatLngBounds.Builder();
                    for (MarkerOptions marker : markers) {
                        builder.include(marker.getPosition());
                    }
                    LatLngBounds bounds = builder.build();

                    int padding = 50; // offset from edges of the map in pixels0
                    CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(bounds, padding);
                    googleMap.moveCamera(cu);
                    googleMap.animateCamera(cu);

                }


            } else if (resultCode == PlaceAutocomplete.RESULT_ERROR) {
                Status status = PlaceAutocomplete.getStatus(mContext, data);
                // TODO: Handle the error.


            } else if (resultCode == RESULT_CANCELED) {
                // The user canceled the operation.
            }
        }
    }
}
