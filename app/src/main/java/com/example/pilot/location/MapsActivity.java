package com.example.pilot.location;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.apache.ApacheHttpTransport;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.List;

public class MapsActivity extends FragmentActivity {

    private static final int REQUEST_PLACE_PICKER = 1;
    private GoogleMap mMap; // Might be null if Google Play services APK is not available.
    //private static final HttpTransport HTTP_TRANSPORT = new NetHttpTransport();
    // Google API Key
    private GoogleApiClient client=null;
    //private Geofence g;
    private static final String PLACES_SEARCH_URL =  "https://maps.googleapis.com/maps/api/place/search/json?";
    private static final boolean PRINT_AS_STRING = false;
    private static final HttpTransport transport = new ApacheHttpTransport();

    String[] mPlaceType=null;
    String[] mPlaceTypeName=null;

    double mLatitude=0;
    double mLongitude=0;

    LocationManager locationManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        final String type=getIntent().getExtras().getString("Place");
        // Array of place types
        mPlaceType = getResources().getStringArray(R.array.place_type);

        // Array of place type names
        mPlaceTypeName = getResources().getStringArray(R.array.place_type_name);

        // Creating an array adapter with an array of Place types
        // to populate the spinner
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, mPlaceTypeName);

        // Getting Google Play availability status
        int status = GooglePlayServicesUtil.isGooglePlayServicesAvailable(getBaseContext());

        if(status!= ConnectionResult.SUCCESS){ // Google Play Services are not available

            int requestCode = 10;
            Dialog dialog = GooglePlayServicesUtil.getErrorDialog(status, this, requestCode);
            dialog.show();

        }
        else
        setUpMapIfNeeded(type);
    }

    @Override
    protected void onResume() {
        super.onResume();
        //setUpMapIfNeeded(type);
    }


    /**
     * Sets up the map if it is possible to do so (i.e., the Google Play services APK is correctly
     * installed) and the map has not already been instantiated.. This will ensure that we only ever
     * call {@link #setUpMap()} once when {@link #mMap} is not null.
     * <p/>
     * If it isn't installed {@link SupportMapFragment} (and
     * {@link com.google.android.gms.maps.MapView MapView}) will show a prompt for the user to
     * install/update the Google Play services APK on their device.
     * <p/>
     * A user can return to this FragmentActivity after following the prompt and correctly
     * installing/updating/enabling the Google Play services. Since the FragmentActivity may not
     * have been completely destroyed during this process (it is likely that it would only be
     * stopped or paused), {@link #onCreate(Bundle)} may not be called again so we should call this
     * method in {@link #onResume()} to guarantee that it will be called.
     */
    private void setUpMapIfNeeded(String type) {
        // Do a null check to confirm that we have not already instantiated the map.
        if (mMap == null) {
            // Try to obtain the map from the SupportMapFragment.
            mMap = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map))
                    .getMap();
            // Check if we were successful in obtaining the map.
            if (mMap != null) {
                setUpMap(type);
            }

        }
    }

    /** A method to download json data from url */
    private String downloadUrl(String strUrl) throws IOException{
        String data = "";
        InputStream iStream = null;
        HttpURLConnection urlConnection = null;
        try{
            URL url = new URL(strUrl);

            // Creating an http connection to communicate with url
            urlConnection = (HttpURLConnection) url.openConnection();

            // Connecting to url
            urlConnection.connect();

            // Reading data from url
            iStream = urlConnection.getInputStream();

            BufferedReader br = new BufferedReader(new InputStreamReader(iStream));

            StringBuffer sb  = new StringBuffer();

            String line = "";
            while( ( line = br.readLine())  != null){
                sb.append(line);
            }

            data = sb.toString();

            br.close();

        }catch(Exception e){
            Log.d("Exception while downloading url", e.toString());
        }finally{
            iStream.close();
            urlConnection.disconnect();
        }

        return data;
    }

    /** A class, to download Google Places */
    private class PlacesTask extends AsyncTask<String, Integer, String> {

        String data = null;

        // Invoked by execute() method of this object
        @Override
        protected String doInBackground(String... url) {
            try{
                data = downloadUrl(url[0]);
            }catch(Exception e){
                Log.d("Background Task",e.toString());
            }

            return data;
        }
        // Executed after the complete execution of doInBackground() method
        @Override
        protected void onPostExecute(String result){
            ParserTask parserTask = new ParserTask();

            // Start parsing the Google places in JSON format
            // Invokes the "doInBackground()" method of the class ParseTask
            parserTask.execute(result);
        }

    }

    /** A class to parse the Google Places in JSON format */
    private class ParserTask extends AsyncTask<String, Integer, List<HashMap<String,String>>> {

        JSONObject jObject;

        // Invoked by execute() method of this object
        @Override
        protected List<HashMap<String, String>> doInBackground(String... jsonData) {

            List<HashMap<String, String>> places = null;
            PlaceJSONParser placeJsonParser = new PlaceJSONParser();

            try {
                jObject = new JSONObject(jsonData[0]);

                /** Getting the parsed data as a List construct */
                places = placeJsonParser.parse(jObject);

            } catch (Exception e) {
                Log.d("Exception", e.toString());
            }
            return places;
        }

        // Executed after the complete execution of doInBackground() method
        @Override
        protected void onPostExecute(List<HashMap<String, String>> list) {
            double lati = 0,lng=0;
            for (int i = 0; i < list.size(); i++) {

                // Creating a marker
                MarkerOptions markerOptions = new MarkerOptions();

                // Getting a place from the places list
                HashMap<String, String> hmPlace = list.get(i);

                // Getting latitude of the place
                lati = Double.parseDouble(hmPlace.get("lat"));

                // Getting longitude of the place
                lng = Double.parseDouble(hmPlace.get("lng"));

                // Getting name
                String name = hmPlace.get("place_name");

                // Getting vicinity
                String vicinity = hmPlace.get("vicinity");

                LatLng latLng = new LatLng(lati, lng);

                // Setting the position for the marker
                markerOptions.position(latLng);

                // Setting the title for the marker.
                //This will be displayed on taping the marker
                markerOptions.title(name + " : " + vicinity);

                // Placing a marker on the touched position
                mMap.addMarker(markerOptions);

            }
            String p = LocationManager.NETWORK_PROVIDER;
            
            Location lp = locationManager.getLastKnownLocation(p);
            double lat=lp.getLatitude();
            double ln=lp.getLongitude();
            float[] resultArray = new float[99];
            Location.distanceBetween(lat,ln,lati,lng,resultArray);
            Toast.makeText(getApplicationContext(),String.valueOf(resultArray[0]),Toast.LENGTH_LONG).show();
        }
    }
    /**
     * This is where we can add markers or lines, add listeners or move the camera. In this case, we
     * just add a marker near Africa.
     * <p/>
     * This should only be called once and when we are sure that {@link #mMap} is not null.
     */
    private void setUpMap(String type) {
        //mMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
        String svcName = Context.LOCATION_SERVICE;
        locationManager = (LocationManager)getSystemService(svcName);
        String provider = LocationManager.GPS_PROVIDER;
        //LocationProvider loc=locationManager.getProvider(provider);
        Location l = locationManager.getLastKnownLocation(provider);
        //double lat=location.getLatitude();
        //double lng=location.getLongitude();
        if(l!=null)
        mMap.addMarker(new MarkerOptions().position(new LatLng(l.getLatitude(),l.getLongitude())).title("You r here"));
        else {
            //MapController
            String p = LocationManager.NETWORK_PROVIDER;
            
            Location lp = locationManager.getLastKnownLocation(p);
            double lat=lp.getLatitude();
            double ln=lp.getLongitude();
            LatLng latLng=new LatLng(lat,ln);
            mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
            mMap.animateCamera(CameraUpdateFactory.zoomTo((float)14.5));
            if(lp!=null) {
                mMap.getUiSettings().setMyLocationButtonEnabled(true);
                mMap.addMarker(new MarkerOptions().position(new LatLng(lat, ln)).title("You r here"));
                Circle circle = mMap.addCircle(new CircleOptions().center(new LatLng(lat, ln)).radius(1400).strokeColor(Color.RED).strokeWidth(3).fillColor(Color.argb(20, 0, 0, 255)));
                //String type = "bank";
                StringBuilder sb = new StringBuilder("https://maps.googleapis.com/maps/api/place/search/json?");
                sb.append("location=" + lat + "," + ln);
                sb.append("&radius=1400");
                sb.append("&types=" + type);
                sb.append("&sensor=false");
                sb.append("&key=BROWSER_KEY");

                // Creating a new non-ui thread task to download json data
                PlacesTask placesTask = new PlacesTask();

                // Invokes the "doInBackground()" method of the class PlaceTask
                placesTask.execute(sb.toString());


            }
            else
            mMap.addMarker(new MarkerOptions().position(new LatLng(0, 0)).title("You r here"));
            /*try {
                PlacePicker.IntentBuilder intentBuilder =
                        new PlacePicker.IntentBuilder();
                Intent intent = intentBuilder.build(this);
                // Start the intent by requesting a result,
                // identified by a request code.
                startActivityForResult(intent, REQUEST_PLACE_PICKER);

            }
            catch (Exception e)
            {
                Toast.makeText(getApplicationContext(), e.toString(), Toast.LENGTH_LONG).show();
            }
            ArrayList list = null;
            try {
                    list.add(new Geofence.Builder()
                            // Set the request ID of the geofence. This is a string to identify this
                            // geofence.
                            .setRequestId(key)

                            .setCircularRegion(
                                    lat,
                                    ln,
                                    1500
                            )
                            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER |
                                    Geofence.GEOFENCE_TRANSITION_EXIT)
                            .build());
                }
                catch(Exception e)
                {
                    Toast.makeText(getApplicationContext(),"Invalid option",Toast.LENGTH_LONG).show();
                }*/
            /*int PLACE_PICKER_REQUEST = 1;
            PlacePicker.IntentBuilder builder = new PlacePicker.IntentBuilder();

            Context context = getApplicationContext();
            startActivityForResult(builder.build(context), PLACE_PICKER_REQUEST);*/
        }
        //mMap.addCircle(new CircleOptions().center(new LatLng(0,0)).radius(1000).strokeColor(Color.RED).fillColor(Color.BLUE));
    }

    /*@Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_PLACE_PICKER
                && resultCode == Activity.RESULT_OK) {

            // The user has selected a place. Extract the name and address.
            final Place place = PlacePicker.getPlace(data, this);

            final CharSequence name = place.getName();
            final CharSequence address = place.getAddress();
            String attributions = PlacePicker.getAttributions(data);
            if (attributions == null) {
                attributions = "";
            }
            Toast.makeText(getApplicationContext(),name,Toast.LENGTH_LONG).show();
            //mViewName.setText(name);
            //mViewAddress.setText(address);
            //mViewAttributions.setText(Html.fromHtml(attributions));

        } else
        super.onActivityResult(requestCode, resultCode, data);
    }*/
public void request(String type)
{
    String p = LocationManager.NETWORK_PROVIDER;
    //LocationProvider location=locationManager.getProvider(p);
    //String key="AIzaSyD4vurfi_HdVFimQvRORmxhJbFippvIiI0";
    Location lp = locationManager.getLastKnownLocation(p);
    double lat=lp.getLatitude();
    double ln=lp.getLongitude();
    //String type = "bank";

    StringBuilder sb = new StringBuilder("https://maps.googleapis.com/maps/api/place/search/json?");
    sb.append("location=" + lat + "," + ln);
    sb.append("&radius=1400");
    sb.append("&types=" + type);
    sb.append("&sensor=false");
    sb.append("&key=AIzaSyCyNaxddrTBptsOops987OkstRpbgDoXrQ");

    // Creating a new non-ui thread task to download json data
    PlacesTask placesTask = new PlacesTask();

    // Invokes the "doInBackground()" method of the class PlaceTask
    placesTask.execute(sb.toString());
}

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        //return super.onCreateOptionsMenu(menu);
        return(true);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id=item.getItemId();
        if(id==R.id.atm)
            request("atm");
        else if (id==R.id.bank)
            request("bank");
        return super.onOptionsItemSelected(item);
    }
}
