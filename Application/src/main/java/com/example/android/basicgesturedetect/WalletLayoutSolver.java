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

    public static class MathC
    {
        public static final float lerp(float from, float to, float alpha)
        {
            return from + (to - from)*alpha;
        }
        public static final float clamp(float min, float max, float alpha)
        {
            return Math.min(max, Math.max(min, alpha));
        }
        public static final int clamp(int min, int max, int alpha)
        {
            return Math.min(max, Math.max(min, alpha));
        }
    }
    private enum State
    {
        List,
        ListToSingle,
        Single,
        SingleToList,
        ListToEdit,
        Edit,
        EditToList
    }
    private static final int FRAME_TIME = 1; // in milisecs
    private static final long LIST_TO_SINGLE_TIME = 1000; // in milisecs
    private static final long SINGLE_TO_LIST_TIME = 1000; // in milisecs
    private static final long EDIT_TO_LIST_TIME = 1000; // in milisecs
    private static final float STACK_RATIO = 0.25f;
    public static final String TAG = "WalletLayoutSolver";

    private long mTimeOrigin;
    private float mCameraY;
    private float mCameraYTarget;
    private State mState;
    private int mFocusedItem;
    private int mEditingItem;
    private int mEditingItemTarget;
    private float mEditingCursor;
    private float mEditingCursorTarget;
    private float mEditingCursorDy;

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
            view.setAlpha(0.0f);
        }
        stack.setClickable(true);
        stack.setFocusable(true);
        stack.post( new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < mStackItems.size(); i++)
                {
                    View view = mStackItems.get(i);
                    view.setElevation(i);
                    view.setAlpha(1.0f);
                }
                updateStack();

            }
        });
        final GestureDetector gd = new GestureDetector(activity, this);
        stack.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                gd.onTouchEvent(motionEvent);
                onTouchEvent(motionEvent);
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
        float y = e.getY() + mCameraY;
        if(y < 0 || y > mItemStackedHeight*(mStackItems.size() - 1) + mItemHeight)
        {
            return;
        }
        int index = (int) (y / mItemStackedHeight);
        index = MathC.clamp(0, mStackItems.size() - 1, index);
        mEditingCursor = index * mItemStackedHeight;
        mEditingCursorDy = y - mEditingCursor;
        Log.i(TAG,"list edit touch y "+y+" cursor y "+mEditingCursor);
        editOn(index);
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
    private void recalculateItemElevation()
    {
        for(int i = 0;i<mStackItems.size();i++)
        {
            View item = mStackItems.get(i);
            item.setElevation(i);
        }
    }

    public void onTouchEvent(MotionEvent e)
    {
        if(mState == State.Edit)
        {
            if (e.getAction() == MotionEvent.ACTION_UP || e.getAction() == MotionEvent.ACTION_CANCEL)
            {
                editOff();
                updateStack();
            }
            else if(e.getAction() == MotionEvent.ACTION_MOVE)
            {
                float y = e.getY() + mCameraY;
                mEditingCursor = y - mEditingCursorDy;
                mEditingItemTarget = (int)Math.floor(mEditingCursor / mItemStackedHeight);
                mEditingItemTarget = MathC.clamp(-1, mStackItems.size(), mEditingItemTarget);
                Log.i(TAG,"list edit touch y "+y+" cursor y "+mEditingCursor + " target "+mEditingItemTarget);
                updateStack();
            }
        }
    }
    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY)
    {
        if(mState == State.List)
        {
            mCameraY += distanceY;
            updateStack();
        }
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
            mCameraY = MathC.clamp(0, mItemStackedHeight* mStackItems.size(), mCameraY);
        }
        Log.i("GestureListener", "mCameraY "+mCameraY);
        for(int i = 0; i< mStackItems.size(); i++)
        {
            float origin = i * mItemHeight;
            float target = Math.max(i * mItemStackedHeight, mCameraY);
            switch(mState)
            {
                case ListToSingle:
                case SingleToList:
                case Single:
                    target += i > mFocusedItem?mUnfocusedOffset:0;
                    break;
                case Edit:
                    target += i > mEditingItemTarget?mItemStackedHeight:0;
                    target -= i > mEditingItem?mItemStackedHeight:0;
                    target = i == mEditingItem?mEditingCursor:target;
                    break;
                case EditToList:
                    target = i == mEditingItemTarget?mEditingCursor:target;
                    break;
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
        if(mState == State.List)
        {
            addFakeCard();
        }
        return false;
    }

    @Override
    public boolean onDoubleTapEvent(MotionEvent e) {
        // Since double-tap is actually several events which are considered one aggregate
        // gesture, there's a separate callback for an individual event within the doubletap
        // occurring.  This occurs for down, up, and move.
        return false;
    }

    private void singleOff()
    {
        mCameraYTarget = mFocusedItem * mItemStackedHeight - (mStack.getHeight() - mItemHeight)/2.0f;
        mCameraYTarget = MathC.clamp(0, mItemStackedHeight* mStackItems.size(), mCameraYTarget);
        mUnfocusedOffsetTarget = 0;
        mState = State.SingleToList;
        mTimeOrigin = System.currentTimeMillis();
        mDetail.setAlpha(1.0f);
        singleToList();
    }

    public void singleToList()
    {
        float time = (System.currentTimeMillis() - mTimeOrigin)/(float) LIST_TO_SINGLE_TIME;
        mCameraY = MathC.lerp(mCameraY, mCameraYTarget, time);
        mUnfocusedOffset = MathC.lerp(mUnfocusedOffset, mUnfocusedOffsetTarget, time);
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
                singleToList();
            }
        }, FRAME_TIME);
    }

    private void singleOn(int index)
    {
        mFocusedItem = index;
        mUnfocusedOffsetTarget = 1500;
        mUnfocusedOffset = 0;
        mCameraYTarget = mFocusedItem * mItemStackedHeight;
        mCameraYTarget = MathC.clamp(0, mItemStackedHeight* mStackItems.size(), mCameraYTarget);
        mState = State.ListToSingle;
        mTimeOrigin = System.currentTimeMillis();
        mDetail.setVisibility(View.VISIBLE);
        mDetail.setAlpha(0.0f);
        listToSingle();
    }


    public void listToSingle()
    {
        float time = (System.currentTimeMillis() - mTimeOrigin)/(float) SINGLE_TO_LIST_TIME;
        mCameraY = MathC.lerp(mCameraY, mCameraYTarget, time);
        mUnfocusedOffset = MathC.lerp(mUnfocusedOffset, mUnfocusedOffsetTarget, time);
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
                listToSingle();
            }
        }, FRAME_TIME);
    }

    private void editOn(int index)
    {
        mEditingItem = index;
        mEditingItemTarget = index;
        mState = State.Edit;
        View item = mStack.getChildAt(mEditingItem);
        item.setElevation(9999);
        item.setAlpha(0.5f);
        Log.i(TAG,"edit on");
    }

    private void editOff()
    {
        boolean move_up = mEditingItemTarget - mEditingItem < 0;
        if(move_up)
        {
            mEditingItemTarget++;
        }
        mEditingItemTarget = MathC.clamp(0, mStackItems.size() - 1, mEditingItemTarget);
        View item = mStack.getChildAt(mEditingItem);
        mStack.removeViewAt(mEditingItem);
        mStack.addView(item,mEditingItemTarget);
        mStackItems.remove(mEditingItem);
        mStackItems.add(mEditingItemTarget, item);
        //item.setAlpha(1.0f);
        recalculateItemElevation();
        mState = State.EditToList;
        mEditingCursorTarget = mEditingItemTarget* mItemStackedHeight;
        mTimeOrigin = System.currentTimeMillis();
        editToList();
        Log.i(TAG,"edit off");
    }

    private void editToList()
    {
        float time = (System.currentTimeMillis() - mTimeOrigin)/(float) EDIT_TO_LIST_TIME;
        mEditingCursor = MathC.lerp(mEditingCursor, mEditingCursorTarget, time);
        float alpha =  mStackItems.get(mEditingItemTarget).getAlpha();
        alpha = MathC.lerp(alpha, 1.0f, time);
        mStackItems.get(mEditingItemTarget).setAlpha(alpha);
        updateStack();
        if(time > 1.0f)
        {
            mState = State.List;
            return;
        }
        final Handler handler = new Handler(Looper.getMainLooper());
        handler.postDelayed(new Runnable()
        {
            @Override
            public void run()
            {
                editToList();
            }
        }, FRAME_TIME);
    }


    @Override
    public boolean onSingleTapConfirmed(MotionEvent e) {
        // A confirmed single-tap event has occurred.  Only called when the detector has
        // determined that the first tap stands alone, and is not part of a double tap.
        // Since double-tap is actually several events which are considered one aggregate
        // gesture, there's a separate callback for an individual event within the doubletap
        // occurring.  This occurs for down, up, and move.
        if(mState == State.List)
        {
            float y = e.getY() + mCameraY;
            if(y < 0 || y > mItemStackedHeight*(mStackItems.size() - 1) + mItemHeight)
            {
                return false;
            }
            int i = (int) (y / mItemStackedHeight);
            singleOn(i);
        }
        else if(mState == State.Single)
        {
            singleOff();
        }
        return false;
    }
    // END_INCLUDE(init_gestureListener)


}
