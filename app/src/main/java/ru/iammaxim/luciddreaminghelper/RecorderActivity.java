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
import android.widget.CheckBox;
import android.widget.Toast;

import java.io.IOException;

import ru.iammaxim.graphlib.LineGraph;

public class RecorderActivity extends AppCompatActivity implements SensorEventListener, View.OnClickListener {
    private static final int PERMISSION_WRITE_EXTERNAL_STORAGE = 2871;

    private SensorManager sensorManager;
    private mSensorManager mSensorManager;
    private LineGraph
            gax, gay, gaz,
            ggx, ggy, ggz;
    private int counterA = 0, counterG = 0;
    private boolean graphsEnabled = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recorder);

        gax = findViewById(R.id.view1);
        gay = findViewById(R.id.view2);
        gaz = findViewById(R.id.view3);
        ggx = findViewById(R.id.view4);
        ggy = findViewById(R.id.view5);
        ggz = findViewById(R.id.view6);

        CheckBox enable_graphs = findViewById(R.id.enable_graphs);
        enable_graphs.setChecked(graphsEnabled);
        enable_graphs.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                graphsEnabled = true;
            } else {
                graphsEnabled = false;
                clearGraphs();
            }
        });

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
            else if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE)
                mSensorManager.onGyroUpdated(event.values[0], event.values[1], event.values[2]);
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
            mSensorManager = new mSensorManager() {
                @Override
                public void onGyroUpdated(double x, double y, double z) {
                    try {
                        super.onGyroUpdated(x, y, z);
                        if (graphsEnabled) {
                            ggx.add(counterG, (float) x);
                            ggy.add(counterG, (float) y);
                            ggz.add(counterG, (float) z);
                            counterG++;
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onAccelUpdated(double x, double y, double z) {
                    try {
                        super.onAccelUpdated(x, y, z);
                        if (graphsEnabled) {
                            gax.add(counterA, (float) x);
                            gay.add(counterA, (float) y);
                            gaz.add(counterA, (float) z);
                            counterA++;
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            };
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
            case R.id.clear:
                clearGraphs();
                break;
            case R.id.stop:
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("Stop");
                builder.setMessage("Are you sure you want to stop recording?");
                builder.setCancelable(true);
                builder.setNegativeButton("No", null);
                builder.setPositiveButton("Yes", (dialog, which) -> {
                    unregisterSensorListeners();
                    mSensorManager.stop();
                });
                builder.show();
                break;
        }
    }

    private void clearGraphs() {
        gax.clear();
        gay.clear();
        gaz.clear();
        ggx.clear();
        ggy.clear();
        ggz.clear();
    }

    private void registerSensorListeners() {
        sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE), SensorManager.SENSOR_DELAY_NORMAL);
    }

    private void unregisterSensorListeners() {
        sensorManager.unregisterListener(this);
    }
}
