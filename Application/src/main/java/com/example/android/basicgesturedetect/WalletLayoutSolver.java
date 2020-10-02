/*
 * Copyright 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.basicgesturedetect;

import android.app.Activity;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.example.android.common.logger.Log;

import java.util.ArrayList;
import java.util.Random;

public class WalletLayoutSolver extends GestureDetector.SimpleOnGestureListener {

    private enum State
    {
        List,
        ListToSingle,
        Single,
        SingleToList
    }
    private static final int FRAME_TIME = 1;
    private static final long SLIDE_IN_TIME = 1000; // in milisecs
    private static final long SLIDE_OUT_TIME = 1000; // in milisecs
    private static final float STACK_RATIO = 0.25f;
    public static final String TAG = "WalletLayoutSolver";

    private long mTimeOrigin;
    private float mCameraY;
    private float mCameraYTarget;
    private State mState;
    private int mFocusedItem;

    private float mItemStackedHeight;
    private float mItemHeight;
    private float mUnfocusedOffset;
    private float mUnfocusedOffsetTarget;
    private LinearLayout mStack;
    private LinearLayout mDetail;
    private Activity mParentActivity;
    private ArrayList<View> mStackItems;

    public void setup(LinearLayout stack, LinearLayout detail, Activity activity)
    {
        mParentActivity = activity;
        mStack = stack;
        mDetail = detail;
        mCameraY = 0.0f;
        mStackItems = new ArrayList<>();
        for (int i = 0; i < stack.getChildCount(); i++)
        {
            View view = stack.getChildAt(i);
            mStackItems.add(view);
            view.setElevation(i);
        }
        stack.setClickable(true);
        stack.setFocusable(true);
        final GestureDetector gd = new GestureDetector(activity, this);
        stack.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                gd.onTouchEvent(motionEvent);
                return false;
            }
        });
        mState = State.List;
        mFocusedItem = -1;
    }

    // BEGIN_INCLUDE(init_gestureListener)
    @Override
    public boolean onSingleTapUp(MotionEvent e) {
        // Up motion completing a single tap occurred.
        return false;
    }

    @Override
    public void onLongPress(MotionEvent e) {
        // Touch has been long enough to indicate a long press.
        // Does not indicate motion is complete yet (no up event necessarily)
        addFakeCard();
    }

    private void addFakeCard()
    {
        ImageView view = new ImageView(mParentActivity.getApplicationContext());
        int[] ids = {R.drawable.credit_card_1,R.drawable.credit_card_2,R.drawable.credit_card_3,R.drawable.credit_card_4};
        int id = ids[(new Random()).nextInt(ids.length)];
        view.setImageResource(id);
        ViewGroup.MarginLayoutParams marginParams = new ViewGroup.MarginLayoutParams(mStackItems.get(0).getLayoutParams());
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(marginParams);
        view.setLayoutParams(params);
        mStackItems.add(view);
        view.setElevation(mStackItems.size()-1);
        mStack.addView(view);
        updateStack();
    }

    private void recalculateItemHeight()
    {
        if(mStackItems.size() > 0)
        {
            View img = mStackItems.get(0);
            mItemHeight = img.getBottom() - img.getTop();
            mItemStackedHeight = mItemHeight * STACK_RATIO;
        }
    }

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        mCameraY += distanceY;
        updateStack();
        return false;
    }

    private void updateStack()
    {
        recalculateItemHeight();
        if(mItemHeight <= 0)
        {
            return;
        }
        if(mState == State.List)
        {
            mCameraY = Math.min(mCameraY,mItemStackedHeight* mStackItems.size());
            mCameraY = Math.max(mCameraY, 0);
        }
        Log.i("GestureListener", "mCameraY "+mCameraY);
        for(int i = 0; i< mStackItems.size(); i++)
        {
            float origin = i * mItemHeight;
            float target = Math.max(i * mItemStackedHeight, mCameraY);
            if(mState != State.List && i > mFocusedItem)
            {
                target += mUnfocusedOffset;
            }
            float margin = target - origin - mCameraY;
            ViewGroup.MarginLayoutParams marginParams = new ViewGroup.MarginLayoutParams(mStackItems.get(i).getLayoutParams());
            marginParams.setMargins(0, (int)margin, 0, (int)-margin);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(marginParams);
            mStackItems.get(i).setLayoutParams(params);
        }
    }

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
                           float velocityY) {
        // Fling event occurred.  Notification of this one happens after an "up" event.
        return false;
    }

    @Override
    public void onShowPress(MotionEvent e) {
        // User performed a down event, and hasn't moved yet.
    }

    @Override
    public boolean onDown(MotionEvent e) {
        // "Down" event - User touched the screen.
        return false;
    }

    @Override
    public boolean onDoubleTap(MotionEvent e) {
        // User tapped the screen twice.
        return false;
    }

    @Override
    public boolean onDoubleTapEvent(MotionEvent e) {
        // Since double-tap is actually several events which are considered one aggregate
        // gesture, there's a separate callback for an individual event within the doubletap
        // occurring.  This occurs for down, up, and move.
        if(e.getAction() == MotionEvent.ACTION_UP)
        {
            if(mState == State.List)
            {
                float y = e.getY() + mCameraY;
                int i = (int) (y / mItemStackedHeight);
                singleOn(i);
            }
            else if(mState == State.Single)
            {
                singleOff();
            }
        }
        return false;
    }

    private void singleOff()
    {
        mCameraYTarget = mFocusedItem * mItemStackedHeight - (mStack.getHeight() - mItemHeight)/2.0f;
        mCameraYTarget = Math.min(mCameraYTarget,mItemStackedHeight* mStackItems.size());
        mCameraYTarget = Math.max(mCameraYTarget, 0);
        mUnfocusedOffsetTarget = 0;
        mState = State.SingleToList;
        mTimeOrigin = System.currentTimeMillis();
        mDetail.setAlpha(1.0f);
        slideToList();
    }

    public void slideToList()
    {
        float time = (System.currentTimeMillis() - mTimeOrigin)/(float) SLIDE_IN_TIME;
        mCameraY = mCameraY + (mCameraYTarget - mCameraY)*time;
        mUnfocusedOffset = mUnfocusedOffset + (mUnfocusedOffsetTarget - mUnfocusedOffset)*time;
        mDetail.setAlpha(Math.max(0.5f-time,0.0f));
        updateStack();
        if(time > 1.0f)
        {
            mFocusedItem = -1;
            mState = State.List;
            mDetail.setVisibility(View.GONE);
            return;
        }
        final Handler handler = new Handler(Looper.getMainLooper());
        handler.postDelayed(new Runnable()
        {
            @Override
            public void run()
            {
                slideToList();
            }
        }, FRAME_TIME);
    }

    private void singleOn(int index)
    {
        mFocusedItem = index;
        mUnfocusedOffsetTarget = 1500;
        mUnfocusedOffset = 0;
        mCameraYTarget = mFocusedItem * mItemStackedHeight;
        mCameraYTarget = Math.min(mCameraYTarget,mItemStackedHeight* mStackItems.size());
        mCameraYTarget = Math.max(mCameraYTarget, 0);
        mState = State.ListToSingle;
        mTimeOrigin = System.currentTimeMillis();
        mDetail.setVisibility(View.VISIBLE);
        mDetail.setAlpha(0.0f);
        slideToSingle();
    }

    public void slideToSingle()
    {
        float time = (System.currentTimeMillis() - mTimeOrigin)/(float) SLIDE_OUT_TIME;
        mCameraY = mCameraY + (mCameraYTarget - mCameraY)*time;
        mUnfocusedOffset = mUnfocusedOffset + (mUnfocusedOffsetTarget - mUnfocusedOffset)*time;
        mDetail.setAlpha(time);
        updateStack();
        if(time > 1.0f)
        {
            mState = State.Single;
            return;
        }
        // TODO: do not create object every frame
        final Handler handler = new Handler(Looper.getMainLooper());
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                slideToSingle();
            }
        }, FRAME_TIME);
    }

    @Override
    public boolean onSingleTapConfirmed(MotionEvent e) {
        // A confirmed single-tap event has occurred.  Only called when the detector has
        // determined that the first tap stands alone, and is not part of a double tap.
        return false;
    }
    // END_INCLUDE(init_gestureListener)


}
