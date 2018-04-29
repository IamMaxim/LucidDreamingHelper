package ru.iammaxim.graphlib;

import android.content.Context;
import android.graphics.Canvas;
import android.support.annotation.Nullable;
import android.util.AttributeSet;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.TimeZone;

/**
 * Created by maxim on 11/22/17.
 */

public class LineGraph extends Graph {
    SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");

    {
        sdf.setTimeZone(TimeZone.getTimeZone("GMT-0:00"));
    }

    public LineGraph(Context context) {
        super(context);
    }

    public LineGraph(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void draw(Canvas canvas, float left, float right) {
        int height = getHeight();
        int width = getWidth();

        if (enableAxes) {
            float cx = (0 - left) / (right - left) * width;
            float cy = (0 - currMinY) / (currMaxY - currMinY) * height;
            // use (height - y) because coordinate system in canvas starts on top
            canvas.drawLine(0, height - cy, width, height - cy, axesPaint);
            canvas.drawLine(cx, 0, cx, height, axesPaint);
        }

        int index = 0;
        for (ArrayList<Point> list : datasets) {
            if (list.size() < 2)
                return;

            // find borders with binary search
            int leftBorder = 0, rightBorder = list.size() - 1;
            int lower = 0;
            int upper = list.size() - 1;
            // left border
            while (lower <= upper) {
                int mid = (upper + lower) / 2;
                float val = list.get(mid).x;
                if (val < left) {
                    lower = mid + 1;
                    if (mid + 1 >= list.size()) {
                        leftBorder = mid;
                        break;
                    }
                    if (left < list.get(mid + 1).x) {
                        leftBorder = mid;
                        break;
                    }
                } else {
                    upper = mid - 1;
                }
            }
            lower = 0;
            upper = list.size() - 1;
            // right border
            while (lower <= upper) {
                int mid = (upper + lower) / 2;
                float val = list.get(mid).x;
                if (right < val) {
                    upper = mid - 1;
                    if (mid - 1 < 0) {
                        rightBorder = mid;
                        break;
                    }
                    if (list.get(mid - 1).x < right) {
                        rightBorder = mid;
                        break;
                    }
                } else {
                    lower = mid + 1;
                }
            }

            Float prevK = null, prevV = null;

            for (int i = leftBorder; i <= rightBorder; i++) {
                Graph.Point p = list.get(i);
                float k = p.x;
                float v = p.y;

                if (prevK == null) {
                    prevK = k;
                    prevV = v;
                    continue;
                }

                float x1 = (prevK - left) / (right - left) * width;
                float y1 = (prevV - currMinY) / (currMaxY - currMinY) * height;

                float x2 = (k - left) / (right - left) * width;
                float y2 = (v - currMinY) / (currMaxY - currMinY) * height;

                // use (height - y) because coordinate system in canvas starts on top
                canvas.drawLine(x1, height - y1, x2, height - y2, graphPaints.get(index));

                prevK = k;
                prevV = v;

                if (index == 0 && i == leftBorder + 1) {
                    canvas.drawText(String.valueOf(p.y), 10, 30, axesTextPaint);
                }
            }


            // draw units
            if (index == 0 && enableAxes) {
                for (int i = leftBorder + 1; i <= rightBorder; i++) {
                    Point leftPoint = list.get(i - 1);
                    Point rightPoint = list.get(i);

                    int interval = 1000 * 60;
                    if (Math.ceil(leftPoint.x / interval) == Math.floor(rightPoint.x / interval)) {
                        long time = (long) Math.floor(rightPoint.x);
                        float x = (time - left) / (right - left) * width;
                        float y = (0 - currMinY) / (currMaxY - currMinY) * height;
                        canvas.drawPoint(x, height - y, axesTextPaint);
                        // draw vertical line
                        canvas.drawLine(x, 0, x, height, subAxesPaint);
                        canvas.drawText(sdf.format(time), x, height - y - 10, axesTextPaint);
                    }
                }

                int yInterval = 5;
                for (int i = (int) currMinY; i < currMaxY; i++) {
                    if (i % yInterval != 0)
                        continue;

                    float x = (0 - left) / (right - left) * width;
                    if (x < 0)
                        x = 0;
                    float y = (i - currMinY) / (currMaxY - currMinY) * height;
                    canvas.drawLine(0, height - y, width, height - y, subAxesPaint);
                    canvas.drawText(String.valueOf(i), x + 10, height - y, axesTextPaint);
                }
            }

            index++;
        }
    }
}
