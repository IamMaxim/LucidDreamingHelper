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
    private LineGraph ga, gsay;
    private View settings_panel;
    private View open_button;
    SeekBar sb1, sb2;
    ProgressBar pb;
    mSensorManager sm = new mSensorManager();

    private void initViews() {
        pb = findViewById(R.id.pb);
        settings_panel = findViewById(R.id.settings_panel);
        open_button = findViewById(R.id.open);

        ga = findViewById(R.id.view1);
        gsay = findViewById(R.id.view7);

        ga.setGraphColor(0, 0xffff0000);
        ga.setGraphColor(1, 0xff00ff00);
        ga.setGraphColor(2, 0xff0000ff);

        gsay.setGraphColor(1, 0xffff0000);

        CheckBox cb1 = findViewById(R.id.cb1);
        CheckBox cb2 = findViewById(R.id.cb2);
        CheckBox cb3 = findViewById(R.id.cb3);

        cb1.setOnCheckedChangeListener(this);
        cb2.setOnCheckedChangeListener(this);
        cb3.setOnCheckedChangeListener(this);

        sb1 = findViewById(R.id.sb1);
        sb2 = findViewById(R.id.sb2);
        sb1.setOnSeekBarChangeListener(this);
        sb2.setOnSeekBarChangeListener(this);
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

                    gsay.add(0, time, (float) sm.avgAmpl);
                    gsay.add(1, time, (float) sm.avgFreq);
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

        ga.addAll(0, dax);
        dax.clear();
        ga.addAll(1, day);
        day.clear();
        ga.addAll(2, daz);
        daz.clear();
        setXzoom(100f / ga.size());
    }

    private void disableAutoYzoom() {
        ga.setAutoYzoom(false);
        gsay.setAutoYzoom(false);
    }

    private void setMinY(float y) {
        ga.setMinY(y, false);
        gsay.setMinY(y, false);
    }

    private void setMaxY(float y) {
        ga.setMaxY(y, false);
        gsay.setMaxY(y, false);
    }

    private void setXzoom(float zoom) {
        ga.setXzoom(zoom);
        gsay.setXzoom(zoom);
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        switch (buttonView.getId()) {
            // TODO: fix this
//            case R.id.cb1:
//                ga.setVisibility(isChecked ? View.VISIBLE : View.GONE);
//                break;
//            case R.id.cb2:
//                gay.setVisibility(isChecked ? View.VISIBLE : View.GONE);
//                break;
//            case R.id.cb3:
//                gaz.setVisibility(isChecked ? View.VISIBLE : View.GONE);
//                break;
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
