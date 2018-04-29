package ru.iammaxim.luciddreaminghelper;

import android.os.Environment;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;

/**
 * Created by maxim on 1/27/18.
 */

@SuppressWarnings("WeakerAccess")
public class mSensorManager {
    public static final int HISTORY_LENGTH = 128;
    public static final int REALTIME_SMOOTH_SAMPLES = 16;
    public static final String
            output_dir = Environment.getExternalStorageDirectory() + "/LucidDreamingHelper/",
            filename_prefix = "session_",
            filename_postfix = ".bin";
    private static final int CURRENT_VERSION = 2;
    public static SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yy_HH:mm:ss");
    private static DataOutputStream dos;
    private long startTime;

    public static final byte GX = 1, GY = 2, GZ = 3, AX = 4, AY = 5, AZ = 6;
    public static final byte GYRO = 1, ACCEL = 2;

    private double[]
            hax = new double[HISTORY_LENGTH],
            hay = new double[HISTORY_LENGTH],
            haz = new double[HISTORY_LENGTH];

    public long[] intervals = new long[HISTORY_LENGTH];
    public double[] derivative = new double[HISTORY_LENGTH];
    public double[] smoothed_derivative = new double[HISTORY_LENGTH];

    public mSensorManager() {
    }

    public void initRecording() throws IOException {
        File dir = new File(output_dir);
        if (!dir.exists())
            dir.mkdirs();
        File outFile = new File(output_dir + filename_prefix + sdf.format(System.currentTimeMillis()) + filename_postfix);
        if (!outFile.exists())
            outFile.createNewFile();
        FileOutputStream fos = new FileOutputStream(outFile);
        dos = new DataOutputStream(fos);
        startTime = System.currentTimeMillis();
        dos.writeInt(CURRENT_VERSION);
    }

    public static final long minPeakPeriod = 2000;
    public static final double noiseThreshold = 0.08;
    public double prevDer = 0;
    public long lastPeakTime = 0;
    public double lastPeak = -1000;
    public double localMax;
    public double localMin;
    public double avgAmpl;
    public long avgInterval;
    public double avgFreq;
    public boolean isNoise = false;

    public void processUpdate(long time) {
//        double[] signals = new double[HISTORY_LENGTH];
//        Arrays.fill(signals, 0);
//        double[] filteredY = new double[HISTORY_LENGTH];
//        double[] avgFilter = new double[HISTORY_LENGTH];
//        Arrays.fill(avgFilter, 0);
//        double[] stdFilter = new double[HISTORY_LENGTH];
//        Arrays.fill(avgFilter, 0);
//        avgFilter[HISTORY_LENGTH - 1] = mean(y, 0, lag);
//        stdFilter[HISTORY_LENGTH - 1] = std(y, 0, lag, avgFilter[HISTORY_LENGTH - 1]);
//
//        for (int i = lag; i < HISTORY_LENGTH - 1; i++) {
//            if (Math.abs(y[i] - avgFilter[i - 1]) > threshold * stdFilter[i - 1]) {
//                if (y[i] > avgFilter[i - 1])
//                    signals[i] = 1;
//                else
//                    signals[i] = -1;
//
//                filteredY[i] = influence * y[i] + (1 - influence) * filteredY[i - 1];
//                avgFilter[i] = mean(filteredY, i - lag, i);
//                stdFilter[i] = std(filteredY, i - lag, i, avgFilter[i]);
//            } else {
//                signals[i] = 0;
//                filteredY[i] = y[i];
//                avgFilter[i] = mean(filteredY, i - lag, lag);
//                stdFilter[i] = std(filteredY, i - lag, lag, avgFilter[i]);
//            }
//        }
//        return signals;
        double[] y = hay;

        if (y[y.length - 1] > lastPeak)
            lastPeak = y[y.length - 1];

        // add derivative
        derivative[HISTORY_LENGTH - 1] = y[HISTORY_LENGTH - 1] - y[HISTORY_LENGTH - 2];

        int smooth_factor = 16;
        // smooth derivative
        {
            double out = 0;
            for (int i = HISTORY_LENGTH - smooth_factor; i < HISTORY_LENGTH; i++)
                out += derivative[i];
            out /= smooth_factor;
            appendToHistory(smoothed_derivative, out);
        }

        int ampl_factor = 64;
        localMin = 100000;
        localMax = -100000;
        for (int i = HISTORY_LENGTH - ampl_factor; i < HISTORY_LENGTH; i++) {
            if (y[i] < localMin)
                localMin = y[i];
            if (y[i] > localMax)
                localMax = y[i];
        }
        avgAmpl = localMax - localMin;
        isNoise = avgAmpl < noiseThreshold;

        if (isNoise)
            return;

        double der = smoothed_derivative[HISTORY_LENGTH - 1];

        double isPeak = prevDer >= 0 && der < 0 ? lastPeak : 0;
        long interval = time - lastPeakTime;
        if (interval < minPeakPeriod)
            isPeak = 0;

        prevDer = der;
        if (isPeak != 0) {
            lastPeakTime = time;
            lastPeak = -1000;
            appendToHistory(intervals, interval);

            int interval_factor = 16;
            avgInterval = 0;
            for (int i = HISTORY_LENGTH - interval_factor; i < HISTORY_LENGTH; i++)
                avgInterval += intervals[i];
            avgInterval /= interval_factor;
            avgFreq = 60000 / avgInterval;
        }
    }

    public void add(double x, double y, double z) {
        appendToHistory(hax, x);
        appendToHistory(hay, y);
        appendToHistory(haz, z);
    }

    public void onAccelUpdated(double x, double y, double z) throws IOException {
        appendToHistory(hax, x);
        appendToHistory(hay, y);
        appendToHistory(haz, z);
        long time = System.currentTimeMillis() - startTime;

        dos.writeByte(ACCEL);
        dos.writeLong(time);
        dos.writeDouble(x);
        dos.writeDouble(y);
        dos.writeDouble(z);
    }

    private static void appendToHistory(double[] h, double value) {
        System.arraycopy(h, 1, h, 0, HISTORY_LENGTH - 1);
        h[h.length - 1] = value;
    }

    private static void appendToHistory(long[] h, long value) {
        System.arraycopy(h, 1, h, 0, HISTORY_LENGTH - 1);
        h[h.length - 1] = value;
    }

    public void stop() {
        try {
            dos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
