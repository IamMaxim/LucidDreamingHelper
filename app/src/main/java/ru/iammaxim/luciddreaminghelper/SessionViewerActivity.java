package ru.iammaxim.luciddreaminghelper;

import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.Toast;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;

import ru.iammaxim.graphlib.Graph;
import ru.iammaxim.graphlib.LineGraph;

import static ru.iammaxim.luciddreaminghelper.mSensorManager.ACCEL;
import static ru.iammaxim.luciddreaminghelper.mSensorManager.AX;
import static ru.iammaxim.luciddreaminghelper.mSensorManager.AY;
import static ru.iammaxim.luciddreaminghelper.mSensorManager.AZ;
import static ru.iammaxim.luciddreaminghelper.mSensorManager.GX;
import static ru.iammaxim.luciddreaminghelper.mSensorManager.GY;
import static ru.iammaxim.luciddreaminghelper.mSensorManager.GYRO;
import static ru.iammaxim.luciddreaminghelper.mSensorManager.GZ;
import static ru.iammaxim.luciddreaminghelper.mSensorManager.HISTORY_LENGTH;
import static ru.iammaxim.luciddreaminghelper.mSensorManager.processUpdate;

public class SessionViewerActivity extends AppCompatActivity implements View.OnClickListener, CompoundButton.OnCheckedChangeListener, SeekBar.OnSeekBarChangeListener {
    private LineGraph
            gax, gay, gaz,
            ggx, ggy, ggz,
            gsay, gsay2;
    private int
            counterAX = 0,
            counterAY = 0,
            counterAZ = 0,
            counterGX = 0,
            counterGY = 0,
            counterGZ = 0;
    private View settings_panel;
    private View open_button;
    private View accel_part, gyro_part;
    SeekBar sb1, sb2;
    ProgressBar pb;

    private void initViews() {
        pb = findViewById(R.id.pb);
        settings_panel = findViewById(R.id.settings_panel);
        open_button = findViewById(R.id.open);

        gax = findViewById(R.id.view1);
        gay = findViewById(R.id.view2);
        gaz = findViewById(R.id.view3);
        ggx = findViewById(R.id.view4);
        ggy = findViewById(R.id.view5);
        ggz = findViewById(R.id.view6);
        gsay = findViewById(R.id.view7);
        gsay2 = findViewById(R.id.view8);

        CheckBox cb1 = findViewById(R.id.cb1);
        CheckBox cb2 = findViewById(R.id.cb2);
        CheckBox cb3 = findViewById(R.id.cb3);
        CheckBox cb4 = findViewById(R.id.cb4);
        CheckBox cb5 = findViewById(R.id.cb5);
        CheckBox cb6 = findViewById(R.id.cb6);
        CheckBox cb7 = findViewById(R.id.cb7);
        CheckBox cb8 = findViewById(R.id.cb8);

        cb1.setOnCheckedChangeListener(this);
        cb2.setOnCheckedChangeListener(this);
        cb3.setOnCheckedChangeListener(this);
        cb4.setOnCheckedChangeListener(this);
        cb5.setOnCheckedChangeListener(this);
        cb6.setOnCheckedChangeListener(this);
        cb7.setOnCheckedChangeListener(this);
        cb8.setOnCheckedChangeListener(this);

        sb1 = findViewById(R.id.sb1);
        sb2 = findViewById(R.id.sb2);
        sb1.setOnSeekBarChangeListener(this);
        sb2.setOnSeekBarChangeListener(this);

        accel_part = findViewById(R.id.accel_part);
        gyro_part = findViewById(R.id.gyro_part);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_session_viewer);
        initViews();

        if (getIntent().getExtras() == null) {
            new AlertDialog.Builder(this).setMessage("Looks like there's no file passed to viewer. Nothing to show.").setTitle("Whooops!").setCancelable(true).setOnCancelListener(dialog -> finish()).show();
            return;
        }

        String path = getIntent().getExtras().getString("file");

        if (path == null) {
            new AlertDialog.Builder(this).setMessage("Looks like there's no file passed to viewer. Nothing to show.").setTitle("Whooops!").setCancelable(true).setOnCancelListener(dialog -> finish()).show();
            return;
        }

        File f = new File(path);
        if (!f.exists()) {
            new AlertDialog.Builder(this).setMessage("Looks like you passed file that doesn't exist. Can't show you it.").setTitle("Whooops!").setCancelable(true).setOnCancelListener(dialog -> finish()).show();
            return;
        }

        new Thread(() -> {
            try {
                FileInputStream fis = new FileInputStream(f);
                DataInputStream dis = new DataInputStream(fis);
                int version = dis.readInt();

//                readV1(dis);

                switch (version) {
                    case 2:
                        readV2(dis);
                        break;
                    default:
                        dis.close();
                        runOnUiThread(() ->
                                Toast.makeText(this, "Couldn't detect version of file. Is it corrupted?", Toast.LENGTH_LONG).show()
                        );
                        finish();
                        return;
                }

                dis.close();
                disableAutoYzoom();
                setMinY(-sb1.getProgress() / 10);
                setMaxY(sb2.getProgress() / 10);
                runOnUiThread(() -> pb.setVisibility(View.GONE));
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void readV2(DataInputStream dis) throws IOException {
        ArrayList<Graph.Point>
                dax = new ArrayList<>(),
                day = new ArrayList<>(),
                daz = new ArrayList<>(),
                dgx = new ArrayList<>(),
                dgy = new ArrayList<>(),
                dgz = new ArrayList<>();

        double[] history_ay = new double[HISTORY_LENGTH];
        double prev_signal = 0;

        while (dis.available() > 0) {
            byte type = dis.readByte();
            long time = dis.readLong();

            double x = dis.readDouble();
            double y = dis.readDouble();
            double z = dis.readDouble();

//            System.out.println(time + " " + x + " " + y + " " + z);

            switch (type) {
                case ACCEL:
                    dax.add(new Graph.Point(time, (float) x));
                    day.add(new Graph.Point(time, (float) y));
                    daz.add(new Graph.Point(time, (float) z));

                    for (int i = 0; i < history_ay.length - 1; i++)
                        history_ay[i] = history_ay[i + 1];
                    history_ay[history_ay.length - 1] = y;

                    double signal = processUpdate(history_ay, 5, 0);

                    float out = prev_signal >= 0 && signal < 0 ? 1 : 0;
                    prev_signal = signal;

                    gsay.add(time, (float) signal);
                    gsay2.add(time, out);

                    break;
                case GYRO:
                    dgx.add(new Graph.Point(time, (float) x));
                    dgy.add(new Graph.Point(time, (float) y));
                    dgz.add(new Graph.Point(time, (float) z));
                    break;
                default:
                    throw new IllegalStateException("Invalid data type " + type);
            }
        }

//        double[] aydata = new double[day.size()];
//        for (int i = 0; i < day.size(); i++) {
//            aydata[i] = day.get(i).y;
//        }
//        double signal = processUpdate(aydata, 30, 5, 0);
//        for (int i = 0; i < signals.length; i++) {
//            gsay.add(i, (float) signals[i]);
//        }
//        gsay.add(signal);

        gax.addAll(dax);
        dax.clear();
        gay.addAll(day);
        day.clear();
        gaz.addAll(daz);
        daz.clear();
        ggx.addAll(dgx);
        dgx.clear();
        ggy.addAll(dgy);
        dgy.clear();
        ggz.addAll(dgz);
        dgz.clear();
        setXzoom(100f / gax.size());
    }

    private void readV1(DataInputStream dis) throws IOException {
        ArrayList<Graph.Point>
                dax = new ArrayList<>(),
                day = new ArrayList<>(),
                daz = new ArrayList<>(),
                dgx = new ArrayList<>(),
                dgy = new ArrayList<>(),
                dgz = new ArrayList<>();

        while (dis.available() > 0) {
            byte action = dis.readByte();
            double value = dis.readDouble();

            switch (action) {
                case AX:
                    dax.add(new Graph.Point(counterAX, (float) value));
                    counterAX++;
                    break;
                case AY:
                    day.add(new Graph.Point(counterAY, (float) value));
                    counterAY++;
                    break;
                case AZ:
                    daz.add(new Graph.Point(counterAZ, (float) value));
                    counterAZ++;
                    break;
                case GX:
                    dgx.add(new Graph.Point(counterGX, (float) value));
                    counterGX++;
                    break;
                case GY:
                    dgy.add(new Graph.Point(counterGY, (float) value));
                    counterGY++;
                    break;
                case GZ:
                    dgz.add(new Graph.Point(counterGZ, (float) value));
                    counterGZ++;
                    break;
            }
        }

        gax.addAll(dax);
        dax.clear();
        gay.addAll(day);
        day.clear();
        gaz.addAll(daz);
        daz.clear();
        ggx.addAll(dgx);
        dgx.clear();
        ggy.addAll(dgy);
        dgy.clear();
        ggz.addAll(dgz);
        dgz.clear();

        setXzoom(1000f / counterAX);
    }

    private void disableAutoYzoom() {
        gax.setAutoYzoom(false);
        gay.setAutoYzoom(false);
        gaz.setAutoYzoom(false);
        ggx.setAutoYzoom(false);
        ggy.setAutoYzoom(false);
        ggz.setAutoYzoom(false);

        gsay.setAutoYzoom(false);
    }

    private void setMinY(float y) {
        gax.setMinY(y, false);
        gay.setMinY(y, false);
        gaz.setMinY(y, false);
        ggx.setMinY(y, false);
        ggy.setMinY(y, false);
        ggz.setMinY(y, false);

        gsay.setMinY(y, false);
    }

    private void setMaxY(float y) {
        gax.setMaxY(y, false);
        gay.setMaxY(y, false);
        gaz.setMaxY(y, false);
        ggx.setMaxY(y, false);
        ggy.setMaxY(y, false);
        ggz.setMaxY(y, false);

        gsay.setMaxY(y, false);
    }

    private void setXzoom(float zoom) {
        gax.setXzoom(zoom);
        gay.setXzoom(zoom);
        gaz.setXzoom(zoom);
        ggx.setXzoom(zoom);
        ggy.setXzoom(zoom);
        ggz.setXzoom(zoom);

        gsay.setX(zoom);
        gsay2.setX(zoom);
    }

    private void resetGraphs() {
        setXzoom(1000f / counterAX);
        gax.setOffset(1);
        gay.setOffset(1);
        gaz.setOffset(1);
        ggx.setOffset(1);
        ggy.setOffset(1);
        ggz.setOffset(1);

        gsay.setOffset(1);
        gsay2.setOffset(1);
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        switch (buttonView.getId()) {
            case R.id.cb1:
                gax.setVisibility(isChecked ? View.VISIBLE : View.GONE);
                break;
            case R.id.cb2:
                gay.setVisibility(isChecked ? View.VISIBLE : View.GONE);
                break;
            case R.id.cb3:
                gaz.setVisibility(isChecked ? View.VISIBLE : View.GONE);
                break;
            case R.id.cb4:
                ggx.setVisibility(isChecked ? View.VISIBLE : View.GONE);
                break;
            case R.id.cb5:
                ggy.setVisibility(isChecked ? View.VISIBLE : View.GONE);
                break;
            case R.id.cb6:
                ggz.setVisibility(isChecked ? View.VISIBLE : View.GONE);
                break;
            case R.id.cb7:
                accel_part.setVisibility(isChecked ? View.VISIBLE : View.GONE);
                break;
            case R.id.cb8:
                gyro_part.setVisibility(isChecked ? View.VISIBLE : View.GONE);
                break;
        }
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        switch (seekBar.getId()) {
            case R.id.sb1:
                setMinY((float) (progress - 200) / 10);
                break;
            case R.id.sb2:
                setMaxY((float) (progress - 200) / 10);
                break;
        }
    }


    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.reset_graphs:
                resetGraphs();
                break;
            case R.id.reset_output:
                gsay.setAutoYzoom(true);
                break;
            case R.id.open:
                settings_panel.setVisibility(View.VISIBLE);
                open_button.setVisibility(View.GONE);
                break;
            case R.id.close:
                settings_panel.setVisibility(View.GONE);
                open_button.setVisibility(View.VISIBLE);
                break;
        }
    }
}
