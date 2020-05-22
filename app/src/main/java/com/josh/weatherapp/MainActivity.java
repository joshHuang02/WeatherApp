package com.josh.weatherapp;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Looper;
import android.provider.Settings;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.androdocs.httprequest.HttpRequest;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    // For location services, NOT WORKING
    int PERMISSION_ID = 44;
    FusedLocationProviderClient mFusedLocationClient;

    // my openweather api key
    String API = "3b83dae12419f728565201e7c002b352";

    String searchField;
    double lat;
    double lon;

    // UI elements
    TextView temperatureTxt;
    TextView displayCityNameTxt;
    Button rawDataBtn;
    Button currentLocation;

    // Controls app behavior based on actions by user
    boolean gotWeather = false;
    boolean useCurrentLocation = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Attempts to read the saved location from last search and look up weather
//        FileInputStream fin = null;
//        String cityName = "";
//        try {
//            fin = openFileInput("cityName");
//            int c;
//            while ((c = fin.read()) != -1) {
//                cityName = cityName + Character.toString((char) c);
//            }
//            fin.close();
//            searchField = cityName;
//            new weatherTask().execute();
//        } catch (FileNotFoundException e) {
//            e.printStackTrace();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }

        // Below code allows user to hit enter key to search instead of clicking "SEARCH"

        final EditText searchEditText = (EditText) findViewById(R.id.searchEditText);
        searchEditText.setOnKeyListener(new View.OnKeyListener() {
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (event.getAction() == KeyEvent.ACTION_DOWN) {
                    switch (keyCode) {
                        case KeyEvent.KEYCODE_ENTER:
                            String userInput = searchEditText.getText().toString();
                            getWeather(searchEditText);

                            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                            imm.hideSoftInputFromWindow(searchEditText.getWindowToken(), 0);

                            return true;
                        default:
                            break;
                    }
                }
                return false;
            }
        });

        // Attempt at getting device location, get weather is called in getLastLocation
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        getLastLocation();
    }

    // method of actions to perform when clicking search button
    public void getWeather(View v) {
        useCurrentLocation = false;
        EditText searchEditText = (EditText) findViewById(R.id.searchEditText);
        searchField = searchEditText.getText().toString();

        // Drops the onscreen keyboard
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(searchEditText.getWindowToken(), 0);

        // Executes getting weather
        new weatherTask().execute();
    }

    // copied from online tutorial on how to access web api, async because it has to wait for a return from API
    class weatherTask extends AsyncTask<String, Void, String> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        // Getting weather data from openweather api using my API key
        protected String doInBackground(String... args) {
            String response;
            if (useCurrentLocation) {
                response = HttpRequest.excuteGet("https://api.openweathermap.org/data/2.5/weather?lat=" + lat + "&lon=" + lon + "&units=metric&appid=" + API);
            } else {
                // Try searching openweather api with a city name endpoint, if error try using zip code endpoint
                try {
                    response = HttpRequest.excuteGet("https://api.openweathermap.org/data/2.5/weather?q=" + searchField + ",us&units=metric&appid=" + API);
                } catch (Exception e) {
                    response = HttpRequest.excuteGet("https://api.openweathermap.org/data/2.5/weather?zip=" + searchField + ",us&units=metric&appid=" + API);
                }
            }
            return response;
        }

        // After receiving a response from api
        @Override
        protected void onPostExecute(String result) {
            displayWeather(result);
        }
    }

    // actions to display weather and save data
    public void displayWeather(String result) {
        temperatureTxt = (TextView) findViewById(R.id.tempTxt);
        displayCityNameTxt = (TextView) findViewById(R.id.cityNameTxt);
        rawDataBtn = (Button) findViewById(R.id.rawDataBtn);
        rawDataBtn.setVisibility(View.VISIBLE);
        gotWeather = true;

        // Write the raw weather data to storage for viewing in a different activity
        FileOutputStream rawWeatherData;
        try {
            rawWeatherData = openFileOutput("rawWeatherData", Context.MODE_PRIVATE);
            rawWeatherData.write(result.getBytes());
            rawWeatherData.close();

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Seeing if there are error messages returned
        try {
            JSONObject jsonObj = new JSONObject(result);

            String code = jsonObj.getString("cod");
            String message = jsonObj.getString("message");
            displayCityNameTxt.setText(message);
            temperatureTxt.setText("0°C");

        } catch (JSONException e) {
            // When there is no error the above try block will go to this catch block
            // Processing JSON object when there is data returned
            try {
                JSONObject jsonObj = new JSONObject(result);

                JSONObject main = jsonObj.getJSONObject("main");
                JSONObject sys = jsonObj.getJSONObject("sys");
                JSONObject wind = jsonObj.getJSONObject("wind");
                JSONObject weather = jsonObj.getJSONArray("weather").getJSONObject(0);

                Long updatedAt = jsonObj.getLong("dt");
                String updatedAtText = "Updated at: " + new SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.ENGLISH).format(new Date(updatedAt * 1000));
                String temperature = main.getString("temp") + "°C";
                String tempMin = "Min Temp: " + main.getString("temp_min") + "°C";
                String tempMax = "Max Temp: " + main.getString("temp_max") + "°C";
                String pressure = main.getString("pressure");
                String humidity = main.getString("humidity");

                Long sunrise = sys.getLong("sunrise");
                Long sunset = sys.getLong("sunset");
                String windSpeed = wind.getString("speed");
                String weatherDescription = weather.getString("description");

                String address = jsonObj.getString("name"); // + ", " + sys.getString("country");

                temperatureTxt.setText(temperature);
                displayCityNameTxt.setText(address);

                FileOutputStream cityName;
                try {
                    cityName = openFileOutput("cityName", Context.MODE_PRIVATE);
                    cityName.write(address.getBytes());
                    cityName.close();
                } catch (FileNotFoundException err) {
                    e.printStackTrace();
                } catch (IOException err) {
                    e.printStackTrace();
                }

            } catch (JSONException err) {
            System.out.println(e);
            }
        }

    }

    // Goes to different activity to see raw returned data from openweather api
    public void seeRawData(View v) {
        if (gotWeather) {
            Intent intent = new Intent(this, RawDataView.class);
            startActivity(intent);
        }
    }


    // Below is all the code to make get location work, get weather is called once location is got
    @SuppressLint("MissingPermission")
    private void getLastLocation() {
        if (checkPermissions()) {
            if (isLocationEnabled()) {
                mFusedLocationClient.getLastLocation().addOnCompleteListener(
                        new OnCompleteListener<Location>() {

                            @Override
                            public void onComplete(@NonNull Task<Location> task) {
                                Location location = task.getResult();
                                if (location == null) {
                                    requestNewLocationData();
                                } else {
                                    String tempLat = location.getLatitude() + "";
                                    String tempLon = location.getLongitude() + "";
                                    lat = Double.parseDouble(tempLat);
                                    lon = Double.parseDouble(tempLon);
                                    new weatherTask().execute();
                                }
                            }
                        }
                );
            } else {
                Toast.makeText(this, "Turn on location", Toast.LENGTH_LONG).show();
                Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivity(intent);
            }
        } else {
            requestPermissions();
        }
    }


    @SuppressLint("MissingPermission")
    private void requestNewLocationData() {

        LocationRequest mLocationRequest = new LocationRequest();
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setInterval(0);
        mLocationRequest.setFastestInterval(0);
        mLocationRequest.setNumUpdates(1);

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        mFusedLocationClient.requestLocationUpdates(
                mLocationRequest, mLocationCallback,
                Looper.myLooper()
        );

    }

    private LocationCallback mLocationCallback = new LocationCallback() {
        @Override
        public void onLocationResult(LocationResult locationResult) {
            Location mLastLocation = locationResult.getLastLocation();
            String tempLat = mLastLocation.getLatitude() + "";
            String tempLon = mLastLocation.getLongitude() + "";
            lat = Double.parseDouble(tempLat);
            lon = Double.parseDouble(tempLon);
            new weatherTask().execute();
        }
    };

    private boolean checkPermissions() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            return true;
        }
        return false;
    }

    private void requestPermissions() {
        ActivityCompat.requestPermissions(
                this,
                new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION},
                PERMISSION_ID
        );
    }

    private boolean isLocationEnabled() {
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(
                LocationManager.NETWORK_PROVIDER
        );
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_ID) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getLastLocation();
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (checkPermissions()) {
            getLastLocation();
        }

    }
}
