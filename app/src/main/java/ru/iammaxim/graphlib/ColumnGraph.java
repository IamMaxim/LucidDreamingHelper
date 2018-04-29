package ru.iammaxim.graphlib;

import android.content.Context;
import android.graphics.Canvas;
import android.support.annotation.Nullable;
import android.util.AttributeSet;

import java.util.ArrayList;

/**
 * Created by maxim on 11/25/17.
 */

public class ColumnGraph extends Graph {
    public ColumnGraph(Context context) {
        super(context);
    }

    public ColumnGraph(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void draw(Canvas canvas, float left, float right) {
        int height = getHeight();
        int width = getWidth();

        synchronized (datasets) {
            int index = 0;
            for (ArrayList<Point> list : datasets) {
                for (Point p : list) {
                    float k = p.x;
                    float v = p.y;

                    float x1 = (k - currMinX) / (currMaxX + 1 - currMinX) * width;
                    float x2 = (k - currMinX + 1) / (currMaxX + 1 - currMinX) * width;
                    float y = (1 - (v - currMinY + 1) / (currMaxY - currMinY + 1)) * height;

                    canvas.drawRect(x1, y, x2, height, graphPaints.get(index));
                }
                index++;
            }
        }

        if (enableAxes) {
            axesDistance = (float) Math.ceil(Math.max(height / (currMaxY - currMinY), height / px(20)));

            for (int i = 1; i < Math.floor((currMaxY - currMinY) / axesDistance); i++) {
                float y = (1 - (i * axesDistance - currMinY + 1) / (currMaxY - currMinY + 1)) * height;
                canvas.drawText(String.valueOf((int) (axesDistance * i)), 5, y, axesTextPaint);
                canvas.drawLine(0, y, width, y, axesPaint);
            }
        }
    }

}
