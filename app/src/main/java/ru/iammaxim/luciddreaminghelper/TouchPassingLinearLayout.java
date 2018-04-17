package ru.iammaxim.luciddreaminghelper;

import android.content.Context;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.LinearLayout;

/**
 * Created by maxim on 1/28/18.
 */

public class TouchPassingLinearLayout extends LinearLayout {
    public TouchPassingLinearLayout(Context context) {
        super(context);
    }

    public TouchPassingLinearLayout(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        for (int i = 0; i < getChildCount(); i++)
            getChildAt(i).onTouchEvent(event);
        return true;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        return true;
    }
}
