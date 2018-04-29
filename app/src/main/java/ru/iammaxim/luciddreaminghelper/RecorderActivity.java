package ru.iammaxim.luciddreaminghelper;

import android.Manifest;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import java.io.IOException;

public class RecorderActivity extends AppCompatActivity implements SensorEventListener, View.OnClickListener {
    private static final int PERMISSION_WRITE_EXTERNAL_STORAGE = 2871;

    private SensorManager sensorManager;
    private mSensorManager mSensorManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recorder);
        // don't turn off screen while recording;
        // otherwise, phone will go into sleep and prevent proper sensor recording
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSION_WRITE_EXTERNAL_STORAGE);
        } else {
            init();
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        try {
            if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER)
                mSensorManager.onAccelUpdated(event.values[0], event.values[1], event.values[2]);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_WRITE_EXTERNAL_STORAGE: {
                if (!(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    Toast.makeText(this, "Couldn't get write permission", Toast.LENGTH_LONG).show();
                    finish();
                } else init();
            }
        }
    }

    private void init() {
        try {
            mSensorManager = new mSensorManager();
            mSensorManager.initRecording();
        } catch (IOException e) {
            e.printStackTrace();
        }
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        registerSensorListeners();
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.stop:
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("Stop");
                builder.setMessage("Are you sure you want to stop recording?");
                builder.setCancelable(true);
                builder.setNegativeButton("No", null);
                builder.setPositiveButton("Yes", (dialog, which) -> {
                    unregisterSensorListeners();
                    mSensorManager.stop();
                    finish();
                });
                builder.show();
                break;
        }
    }

    private void registerSensorListeners() {
        sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_NORMAL);
    }

    private void unregisterSensorListeners() {
        sensorManager.unregisterListener(this);
    }
}
