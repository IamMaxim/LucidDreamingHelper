package ru.iammaxim.graphlib;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.Transformation;

import java.util.ArrayList;

/**
 * Created by maxim on 11/25/17.
 */

public abstract class Graph extends View {
    public void setXzoom(float xzoom) {
        this.zoom = xzoom;
    }

    public void setMinY(float minY, boolean animate) {
        this.minY = minY;
        if (!animate)
            currMinY = minY;
        dirty = true;
        postInvalidate();
    }

    public void setMaxY(float maxY, boolean animate) {
        this.maxY = maxY;
        if (!animate)
            currMaxY = maxY;
        dirty = true;
        postInvalidate();
    }

    public void setOffset(float offset) {
        this.offset = offset;
        dirty = true;
        postInvalidate();
    }

    public int size() {
        int size = 0;
        for (ArrayList<Point> list : datasets)
            size = Math.max(size, list.size());
        return size;
    }

    public void disableAxes() {
        enableAxes = false;
    }

    public static class Point {
        public float x, y;

        public Point(float x, float y) {
            this.x = x;
            this.y = y;
        }
    }


    protected final ArrayList<ArrayList<Point>> datasets = new ArrayList<>();

    private ScaleGestureDetector mScaleDetector;
    public boolean enableAxes = true;
    AnimationSet animSet;
    protected boolean dirty = false;
    protected Animation animXMax, animXMin, animYMax, animYMin;
    protected boolean animEnabled = true;
    protected float minX = Float.MAX_VALUE / 3;
    protected float maxX = -Float.MAX_VALUE / 3;
    protected float minY = Float.MAX_VALUE / 3;
    protected float maxY = -Float.MAX_VALUE / 3;
    protected float currMinX = 0;
    protected float currMaxX = 0;
    protected float currMinY = 0;
    protected float currMaxY = 0;
    protected Paint axesPaint, axesTextPaint, subAxesPaint;
    protected ArrayList<Paint> graphPaints = new ArrayList<>();
    protected float axesDistance = 10;
    protected float zoom = 1;
    protected float offset = 1;
    protected boolean autoYzoom = true;

    public void setGraphColor(int datasetIndex, int color) {
        if (datasetIndex < 0)
            throw new IndexOutOfBoundsException();

        while (datasetIndex >= graphPaints.size())
            addPaint();
        graphPaints.get(datasetIndex).setColor(color);
    }

    public void addPaint() {
        Paint p = new Paint();
        p.setColor(0xff3333ff);
        p.setStrokeWidth(px(2));
        p.setStrokeCap(Paint.Cap.ROUND);
        p.setStrokeJoin(Paint.Join.ROUND);
        p.setAntiAlias(true);
        graphPaints.add(p);
    }

    public void setAutoYzoom(boolean enable) {
        if (enable) {
            autoYzoom = true;
            synchronized (datasets) {
                for (ArrayList<Point> list : datasets)
                    for (Point point : list)
                        processMinAndMax(point.x, point.y);
            }
        } else {
            autoYzoom = false;
        }
    }

    public Graph(Context context) {
        this(context, null);
    }

    public Graph(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);

        // init paints
        axesPaint = new Paint();
        axesPaint.setColor(0x33000000);
        axesPaint.setStrokeWidth(3);

        subAxesPaint = new Paint();
        subAxesPaint.setColor(0x10000000);
        subAxesPaint.setStrokeWidth(3);

        axesTextPaint = new Paint();
        axesTextPaint.setColor(0xff000000);
        axesTextPaint.setTextSize(px(12));
        axesTextPaint.setAntiAlias(true);
        axesTextPaint.setStrokeWidth(10);

        addPaint();

        // init gestures
        mScaleDetector = new ScaleGestureDetector(context, new GraphScaleListener());
    }

    protected abstract void draw(Canvas canvas, float left, float right);

    protected float px(float dp) {
        DisplayMetrics dm = getContext().getResources().getDisplayMetrics();
        return dm.density * dp;
    }

    public void clear() {
        datasets.clear();
        minX = Float.MAX_VALUE;
        maxX = -Float.MAX_VALUE;
        if (autoYzoom) {
            minY = Float.MAX_VALUE;
            maxY = -Float.MAX_VALUE;
        }
        postInvalidate();
    }

    /*public void setPoints(ArrayList<Point> datasets) {
        synchronized (datasets) {
            this.datasets = datasets;

            for (Point point : datasets) {
                processMinAndMax(point.x, point.y);
            }
        }
        dirty = true;
        postInvalidate();
    }*/

    protected void updateXMaxAnim() {
        if (animEnabled) {
            final float start = currMaxX;
            final float target = maxX;

            animXMax = new Animation() {
                @Override
                protected void applyTransformation(float interpolatedTime, Transformation t) {
                    currMaxX = start + (target - start) * interpolatedTime;
                    postInvalidate();
                }
            };
            animXMax.setDuration(200);
        }
    }

    protected void updateXMinAnim() {
        if (animEnabled) {
            final float start = currMinX;
            final float target = minX;

            animXMin = new Animation() {
                @Override
                protected void applyTransformation(float interpolatedTime, Transformation t) {
                    currMinX = start + (target - start) * interpolatedTime;
                    postInvalidate();
                }
            };
            animXMin.setDuration(200);
        }
    }

    protected void updateYMinAnim() {
        if (animEnabled) {
            final float start = currMinY;
            final float target = minY;

            animYMin = new Animation() {
                @Override
                protected void applyTransformation(float interpolatedTime, Transformation t) {
                    currMinY = start + (target - start) * interpolatedTime;
                    postInvalidate();
                }
            };
            animYMin.setDuration(200);
        }
    }

    protected void updateYMaxAnim() {
        if (animEnabled) {
            final float start = currMaxY;
            final float target = maxY;

            animYMax = new Animation() {
                @Override
                protected void applyTransformation(float interpolatedTime, Transformation t) {
                    currMaxY = start + (target - start) * interpolatedTime;
                    postInvalidate();
                }
            };
            animYMax.setDuration(200);
        }
    }

    private void processMinAndMax(float x, float y) {
        if (x > maxX)
            maxX = x;
        if (x < minX)
            minX = x;

        if (autoYzoom) {
            if (y > maxY)
                maxY = y;
            if (y < minY)
                minY = y;
        }
    }

    public void add(int datasetIndex, float x, float y) {
        if (datasetIndex < 0)
            throw new IndexOutOfBoundsException("No dataset with such index");

        while (datasetIndex >= datasets.size()) {
            datasets.add(new ArrayList<>());
            addPaint();
        }

        synchronized (datasets) {
            if (datasets.get(datasetIndex).size() > 0) {
                Point lastP = datasets.get(datasetIndex).get(datasets.get(datasetIndex).size() - 1);
                if (x < lastP.x)
                    throw new IllegalArgumentException("x < datasets[-1].x");
            }

            datasets.get(datasetIndex).add(new Point(x, y));
        }
        dirty = true;
        processMinAndMax(x, y);
        postInvalidate();
    }

    public void addAll(int datasetIndex, ArrayList<Point> list) {
        if (datasetIndex < 0)
            throw new IndexOutOfBoundsException("No dataset with such index");

        while (datasetIndex >= datasets.size()) {
            datasets.add(new ArrayList<>());
            addPaint();
        }

        synchronized (datasets) {
            datasets.get(datasetIndex).addAll(list);
        }

        for (Point p : list) {
            float k = p.x;
            float v = p.y;
            processMinAndMax(k, v);
        }

        dirty = true;
        postInvalidate();
    }

    public void addInBeginning(int datasetIndex, float x, float y) {
        if (datasetIndex < 0)
            throw new IndexOutOfBoundsException("No dataset with such index");

        while (datasetIndex >= datasets.size()) {
            datasets.add(new ArrayList<>());
            addPaint();
        }

        synchronized (datasets) {
            if (datasets.get(datasetIndex).size() > 0) {
                Point p = datasets.get(datasetIndex).get(0);
                if (x > p.x)
                    throw new IllegalArgumentException("x > datasets[0].x");
            }
            datasets.get(datasetIndex).add(0, new Point(x, y));
        }

        dirty = true;
        processMinAndMax(x, y);
        postInvalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float left = (currMaxX - currMinX) * zoom * (offset - 1),
                right = (currMaxX - currMinX) * zoom * offset;
//        float left = currMinX,
//                right = currMaxX;

        // start anims if needed
        if (animEnabled && dirty) {
            updateXMaxAnim();
            updateXMinAnim();
            updateYMinAnim();
            updateYMaxAnim();

            if (animSet != null)
                animSet.cancel();

            animSet = new AnimationSet(true);
            animSet.addAnimation(animXMax);
            animSet.addAnimation(animXMin);
            animSet.addAnimation(animYMax);
            animSet.addAnimation(animYMin);

            startAnimation(animSet);

            dirty = false;
        }

        draw(canvas, left, right);
    }

    protected float lastPointerPos;

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if (ev.getPointerCount() == 1) {
            if (ev.getAction() == MotionEvent.ACTION_DOWN)
                lastPointerPos = ev.getX();
            else if (ev.getAction() == MotionEvent.ACTION_MOVE) {
                float pos = ev.getX();
                float delta = (pos - lastPointerPos) / getWidth();
                offset -= delta;
                if (offset < 0)
                    offset = 0;
                lastPointerPos = pos;
                invalidate();
            }
        } else
            mScaleDetector.onTouchEvent(ev);
        return true;
    }

    private class GraphScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            zoom /= detector.getScaleFactor();
            offset *= detector.getScaleFactor();
            invalidate();
            return true;
        }
    }
}
