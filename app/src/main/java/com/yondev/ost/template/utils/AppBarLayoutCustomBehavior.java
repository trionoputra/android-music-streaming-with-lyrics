package com.yondev.ost.template.utils;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CoordinatorLayout;
import android.util.AttributeSet;
import android.view.MotionEvent;

/**
 * Created by Bakwan on 25/10/2017.
 */

public class AppBarLayoutCustomBehavior extends AppBarLayout.Behavior {
    private boolean setIntercept = false;
    private boolean lockAppBar = false;

    DragCallback mDragCallback = new DragCallback() {
        @Override
        public boolean canDrag(@NonNull AppBarLayout appBarLayout) {
            return !lockAppBar;
        }
    };

    @Override
    public boolean onInterceptTouchEvent(CoordinatorLayout parent, AppBarLayout child, MotionEvent ev) {
        super.onInterceptTouchEvent(parent, child, ev);
        return setIntercept;
    }

    public void setInterceptTouchEvent(boolean set) {
        setIntercept = set;
    }

    public AppBarLayoutCustomBehavior() {
        super();
        setDragCallback(mDragCallback);
    }

    public AppBarLayoutCustomBehavior(Context ctx, AttributeSet attributeSet) {
        super(ctx, attributeSet);
        setDragCallback(mDragCallback);
    }

    public void lockAppBar() {
        lockAppBar = true;
    }

    public void unlockAppBar() {
        lockAppBar = false;
    }
}