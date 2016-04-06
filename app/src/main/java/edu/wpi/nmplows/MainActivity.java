package edu.wpi.nmplows;

import android.Manifest;
import android.content.Intent;
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
import com.firebase.client.Firebase;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
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
    private static final String BASE_URL = "https://nmplows.firebaseio.com";

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
    private Firebase firebase;

    private final Plow plow = new Plow();

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

        Firebase.setAndroidContext(this);

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

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        Log.i(TAG, "Activity result: " + requestCode + " " + resultCode);

        if (requestCode == IntentIntegrator.REQUEST_CODE) {
            IntentResult scanningResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, intent);
            Log.i(TAG, "Scanning result: " + scanningResult.toString());
        }
    }

    private void startTracking() {
        Log.d(TAG, "Starting tracking");

        progressBar.setVisibility(View.VISIBLE);
        plowId.setEnabled(false);

        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "Requesting location permissions");
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 0);
            return;
        }

        firebase = new Firebase(BASE_URL + "/plows/nm" + plowId.getText());
        firebase.authAnonymously(null);

        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 10000, 100.0f, this);
    }

    private void stopTracking() {
        Log.d(TAG, "Stopping tracking");

        progressBar.setVisibility(View.GONE);
        plowId.setEnabled(true);

        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationManager.removeUpdates(this);
        }

        Log.i(TAG, "removeValue");
        firebase.removeValue();
    }

    @Override
    public void onLocationChanged(Location location) {
        Log.d(TAG, "Got location update");

        plow.id = Integer.valueOf(plowId.getText().toString());
        plow.time = location.getTime();
        plow.latitude = location.getLatitude();
        plow.longitude = location.getLongitude();
        if (location.hasSpeed()) plow.speed = location.getSpeed() * MPS_TO_MPH;
        if (location.hasBearing()) plow.bearing = bearingLabel(location.getBearing());

        firebase.setValue(plow);

        progressBar.setVisibility(View.GONE);
        time.setText(new Date(plow.time).toString());
        latitude.setText(String.format("%f°", plow.latitude));
        longitude.setText(String.format("%f°", plow.longitude));
        speed.setText(String.format("%f mph", plow.speed));
        bearing.setText(plow.bearing);
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

