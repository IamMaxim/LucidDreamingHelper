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

public class mSensorManager {
    public static final int HISTORY_LENGTH = 32;
    public static final String
            output_dir = Environment.getExternalStorageDirectory() + "/LucidDreamingHelper/",
            filename_prefix = "session_",
            filename_postfix = ".bin";
    private static final int CURRENT_VERSION = 2;
    public static SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yy_HH:mm:ss");
    private static File outFile;
    private static FileOutputStream fos;
    private static DataOutputStream dos;
    private long startTime;

    public static final byte GX = 1, GY = 2, GZ = 3, AX = 4, AY = 5, AZ = 6;
    public static final byte GYRO = 1, ACCEL = 2;

    private double[]
            hax = new double[HISTORY_LENGTH],
            hay = new double[HISTORY_LENGTH],
            haz = new double[HISTORY_LENGTH];
    private double[]
            hgx = new double[HISTORY_LENGTH],
            hgy = new double[HISTORY_LENGTH],
            hgz = new double[HISTORY_LENGTH];

    public mSensorManager() throws IOException {
        File dir = new File(output_dir);
        if (!dir.exists())
            dir.mkdirs();
        outFile = new File(output_dir + filename_prefix + sdf.format(System.currentTimeMillis()) + filename_postfix);
        if (!outFile.exists())
            outFile.createNewFile();
        fos = new FileOutputStream(outFile);
        dos = new DataOutputStream(fos);
        startTime = System.currentTimeMillis();
        dos.writeInt(CURRENT_VERSION);
    }

    /**
     * @param lag       amount of points to take in account
     * @param threshold deviation limit
     * @param influence [0..1], .....
     * @return
     */
    public static double processUpdate(double[] y, float threshold, float influence) {
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

        double[] derivative = new double[HISTORY_LENGTH];
        double[] smoothed_derivative = new double[HISTORY_LENGTH];

        // find derivative
        for (int i = 1; i < HISTORY_LENGTH; i++)
            derivative[i] = y[i] - y[i - 1];

        int smooth_factor = 5;
        // smooth derivative
        for (int i = smooth_factor; i < HISTORY_LENGTH; i++) {
            double out = 0;
            for (int j = i - smooth_factor; j < i; j++)
                out += derivative[j];
            out /= smooth_factor;
            smoothed_derivative[i] = out;
        }

        return smoothed_derivative[HISTORY_LENGTH - 1];
    }

    private static double mean(double[] arr, int start, int end) {
        double mean = 0;
        for (int i = start; i < end; i++) {
            mean += arr[i];
        }
        mean /= end - start;
        return mean;
    }

    private static double std(double[] arr, int start, int end, double mean) {
        double std = 0;
        for (int i = start; i < end; i++) {
            std += (arr[i] - mean) * (arr[i] - mean);
        }
        std /= end - start;
        std = Math.sqrt(std);
        return std;
    }

    public void onGyroUpdated(double x, double y, double z) throws IOException {
        appendToHistory(hgx, x);
        appendToHistory(hgy, y);
        appendToHistory(hgz, z);
        long time = System.currentTimeMillis() - startTime;

        dos.writeByte(GYRO);
        dos.writeLong(time);
        dos.writeDouble(x);
        dos.writeDouble(y);
        dos.writeDouble(z);
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

    private void appendToHistory(double[] h, double value) {
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
