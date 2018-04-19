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
import static ru.iammaxim.luciddreaminghelper.mSensorManager.GYRO;

public class SessionViewerActivity extends AppCompatActivity implements View.OnClickListener, CompoundButton.OnCheckedChangeListener, SeekBar.OnSeekBarChangeListener {
    private LineGraph
            gax, gay, gaz,
            gsay, gsay2;
    private View settings_panel;
    private View open_button;
    private View accel_part;
    SeekBar sb1, sb2;
    ProgressBar pb;
    mSensorManager sm = new mSensorManager();

    private void initViews() {
        pb = findViewById(R.id.pb);
        settings_panel = findViewById(R.id.settings_panel);
        open_button = findViewById(R.id.open);

        gax = findViewById(R.id.view1);
        gay = findViewById(R.id.view2);
        gaz = findViewById(R.id.view3);
        gsay = findViewById(R.id.view7);
        gsay2 = findViewById(R.id.view8);

        gax.setGraphColor(0xffff0000);
        gay.setGraphColor(0xff00ff00);
        gaz.setGraphColor(0xff0000ff);

        gsay2.setGraphColor(0xffff0000);

        CheckBox cb1 = findViewById(R.id.cb1);
        CheckBox cb2 = findViewById(R.id.cb2);
        CheckBox cb3 = findViewById(R.id.cb3);
        CheckBox cb7 = findViewById(R.id.cb7);

        cb1.setOnCheckedChangeListener(this);
        cb2.setOnCheckedChangeListener(this);
        cb3.setOnCheckedChangeListener(this);
        cb7.setOnCheckedChangeListener(this);

        sb1 = findViewById(R.id.sb1);
        sb2 = findViewById(R.id.sb2);
        sb1.setOnSeekBarChangeListener(this);
        sb2.setOnSeekBarChangeListener(this);

        accel_part = findViewById(R.id.accel_part);
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
                daz = new ArrayList<>();

        while (dis.available() > 0) {
            byte type = dis.readByte();
            long time = dis.readLong();

            double x = dis.readDouble();
            double y = dis.readDouble();
            double z = dis.readDouble();

            switch (type) {
                case ACCEL:
                    dax.add(new Graph.Point(time, (float) x));
                    day.add(new Graph.Point(time, (float) y));
                    daz.add(new Graph.Point(time, (float) z));

                    sm.add(x, y, z);
                    sm.processUpdate(time);

                    gsay.add(time, (float) sm.localAmpl);
                    gsay2.add(time, sm.isNoise ? 1 : 0);
                    break;
                case GYRO:
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
        setXzoom(100f / gax.size());
    }

    private void disableAutoYzoom() {
        gax.setAutoYzoom(false);
        gay.setAutoYzoom(false);
        gaz.setAutoYzoom(false);

        gsay.setAutoYzoom(false);
    }

    private void setMinY(float y) {
        gax.setMinY(y, false);
        gay.setMinY(y, false);
        gaz.setMinY(y, false);

        gsay.setMinY(y, false);
    }

    private void setMaxY(float y) {
        gax.setMaxY(y, false);
        gay.setMaxY(y, false);
        gaz.setMaxY(y, false);

        gsay.setMaxY(y, false);
    }

    private void setXzoom(float zoom) {
        gax.setXzoom(zoom);
        gay.setXzoom(zoom);
        gaz.setXzoom(zoom);

        gsay.setXzoom(zoom);
        gsay2.setXzoom(zoom);
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
            case R.id.cb7:
                accel_part.setVisibility(isChecked ? View.VISIBLE : View.GONE);
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
