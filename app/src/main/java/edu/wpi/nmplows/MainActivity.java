package edu.wpi.nmplows;

import android.Manifest;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.location.*;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.*;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;
import java.util.Objects;

public class MainActivity extends AppCompatActivity implements ActivityCompat.OnRequestPermissionsResultCallback, LocationListener {

    private static final String TAG = "MainActivity";

    private static final double MPS_TO_MPH = 2.23694;

    private LocationManager locationManager;
    private SensorManager sensorManager;
    private Sensor orientationSensor;
    private ProgressBar progressBar;
    private EditText plowId;
    private Switch running;
    private TextView time;
    private TextView latitude;
    private TextView longitude;
    private TextView speed;
    private TextView bearing;

    private final Object jsonLock = new Object();
    private final JSONObject json = new JSONObject();
    private final Runnable networkThread = new Runnable() {
        @Override
        public void run() {
            Log.i(TAG, "Running network thread");

            while (true) {
                Log.i(TAG, "Waiting for JSON data");

                try {
                    String data;
                    synchronized (jsonLock) {
                        jsonLock.wait();
                        data = json.toString();
                    }

                    HttpURLConnection connection = (HttpURLConnection) new URL("https://nmplows.firebaseio.com/plows/nm" + plowId.getText() + ".json").openConnection();
                    connection.setRequestMethod("PUT");
                    connection.setRequestProperty("Content-Type", "application/json");
                    connection.getOutputStream().write(data.getBytes());
                    Log.d(TAG, connection.getResponseCode() + " " + connection.getResponseMessage());
                    connection.disconnect();
                } catch (IOException | InterruptedException e) {
                    Log.e(TAG, "Error sending JSON data", e);
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        orientationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION);
        plowId = (EditText) findViewById(R.id.plowId);
        running = (Switch) findViewById(R.id.running);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        time = (TextView) findViewById(R.id.time);
        latitude = (TextView) findViewById(R.id.latitude);
        longitude = (TextView) findViewById(R.id.longitude);
        speed = (TextView) findViewById(R.id.speed);
        bearing = (TextView) findViewById(R.id.bearing);

        new Thread(networkThread).start();

        running.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    startTracking();
                } else {
                    stopTracking();
                }
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        startTracking();
    }

    private void startTracking() {
        progressBar.setVisibility(View.VISIBLE);
        plowId.setEnabled(false);

        Log.d(TAG, "Starting tracking");

        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "Requesting location permissions");
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 0);
            return;
        }

        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 0.0f, this);
    }

    private void stopTracking() {
        progressBar.setVisibility(View.GONE);
        plowId.setEnabled(true);

        Log.d(TAG, "Stopping tracking");

        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationManager.removeUpdates(this);
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        Log.d(TAG, "Got location update");

        progressBar.setVisibility(View.GONE);

        try {
            synchronized (jsonLock) {
                json.put("time", location.getTime() / 1000);
                time.setText(new Date(location.getTime()).toString());

                json.put("latitude", location.getLatitude());
                latitude.setText(String.format("%f°", location.getLatitude()));

                json.put("longitude", location.getLongitude());
                longitude.setText(String.format("%f°", location.getLongitude()));

                if (location.hasSpeed()) {
                    json.put("speed", location.getSpeed() * MPS_TO_MPH);
                    speed.setText(String.format("%f mph", location.getSpeed() * MPS_TO_MPH));
                }

                if (location.hasBearing()) {
                    String label = bearingLabel(location.getBearing());
                    json.put("bearing", label);
                    bearing.setText(label);
                }

                jsonLock.notify();
            }
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        switch (status) {
            case LocationProvider.OUT_OF_SERVICE:
                Log.d(TAG, "Location status changed: " + provider + " out of service");
                break;
            case LocationProvider.AVAILABLE:
                Log.d(TAG, "Location status changed: " + provider + " available");
                break;
            case LocationProvider.TEMPORARILY_UNAVAILABLE:
                Log.d(TAG, "Location status changed: " + provider + " temporarily unavailable");
                break;
        }
    }

    @Override
    public void onProviderEnabled(String provider) {
        Log.d(TAG, "Location provider enabled: " + provider);
    }

    @Override
    public void onProviderDisabled(String provider) {
        Log.d(TAG, "Location provider disabled: " + provider);
    }

    private String bearingLabel(float bearing) {
        if (bearing < 22.5f) {
            return "N";
        } else if (bearing < 67.5f) {
            return "NE";
        } else if (bearing < 112.5f) {
            return "E";
        } else if (bearing < 157.5f) {
            return "SE";
        } else if (bearing < 202.5f) {
            return "S";
        } else if (bearing < 247.5f) {
            return "SW";
        } else if (bearing < 292.5f) {
            return "W";
        } else if (bearing < 337.5f) {
            return "NW";
        } else {
            return "N";
        }

    }
}

