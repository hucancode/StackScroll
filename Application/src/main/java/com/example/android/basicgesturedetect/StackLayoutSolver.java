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

import android.annotation.TargetApi;
import android.app.Activity;
import android.os.Build;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.example.android.common.logger.Log;

import java.util.ArrayList;
import java.util.Random;

public class StackLayoutSolver extends GestureDetector.SimpleOnGestureListener {

    private float mCameraY;
    private float mCascadeHeight;
    private float mItemHeight;
    LinearLayout mStack;
    View mFirstChild;
    Activity mParentActivity;
    private ArrayList<View> mScrollItems;
    public static final String TAG = "StackScrollSolver";

    public void setup(LinearLayout stack, Activity activity)
    {
        mParentActivity = activity;
        mStack = stack;
        mStack.addOnLayoutChangeListener( new View.OnLayoutChangeListener()
        {
            public void onLayoutChange( View v,
                                        int left,    int top,    int right,    int bottom,
                                        int leftWas, int topWas, int rightWas, int bottomWas )
            {
                recalculateItemHeight();
            }
        });
        mCameraY = 0.0f;
        mScrollItems = new ArrayList<>();
        for (int i = 0; i < stack.getChildCount(); i++)
        {
            View view = stack.getChildAt(i);
            mScrollItems.add(view);
            view.setElevation(i);
        }
        mFirstChild = mScrollItems.get(0);
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
    }

    // BEGIN_INCLUDE(init_gestureListener)
    @Override
    public boolean onSingleTapUp(MotionEvent e) {
        // Up motion completing a single tap occurred.
        Log.i(TAG, "Single Tap Up" + getTouchType(e));
        return false;
    }

    @Override
    public void onLongPress(MotionEvent e) {
        // Touch has been long enough to indicate a long press.
        // Does not indicate motion is complete yet (no up event necessarily)
        Log.i(TAG, "Long Press" + getTouchType(e));
        ImageView view = new ImageView(mParentActivity.getApplicationContext());
        int[] ids = {R.drawable.credit_card_1,R.drawable.credit_card_2,R.drawable.credit_card_3,R.drawable.credit_card_4};
        int id = ids[(new Random()).nextInt(ids.length)];
        view.setImageResource(id);
        ViewGroup.MarginLayoutParams marginParams = new ViewGroup.MarginLayoutParams(mScrollItems.get(0).getLayoutParams());
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(marginParams);
        view.setLayoutParams(layoutParams);
        mScrollItems.add(view);
        view.setElevation(mScrollItems.size()-1);
        mStack.addView(view);
        onScroll(null,null,0,0);
    }

    private void recalculateItemHeight()
    {
        if(mScrollItems.size() > 0)
        {
            View img = mScrollItems.get(0);
            mItemHeight = img.getBottom() - img.getTop();
            mCascadeHeight = (mStack.getHeight() - mItemHeight)/2.0f;
        }
        onScroll(null,null,0,0);
    }

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        if(mItemHeight <= 0)
        {
            return false;
        }
        mCameraY += distanceY;
        mCameraY = Math.min(mCameraY,mItemHeight*mScrollItems.size()-(mStack.getHeight() - mItemHeight)/2.0f);
        mCameraY = Math.max(mCameraY, mItemHeight/2);
        float pivotIndex = Math.max(0,mCameraY/mItemHeight)+1;
        Log.i("GestureListener", "mCameraY "+mCameraY+" pivotIndex "+pivotIndex);
        for(int i=0;i<mScrollItems.size();i++)
        {
            float origin = i * mItemHeight;
            float target = mCameraY - ((pivotIndex - i)/pivotIndex)* mCascadeHeight;
            float margin = Math.max(0, target - origin) - mCameraY + mCascadeHeight;
            ViewGroup.MarginLayoutParams marginParams = new ViewGroup.MarginLayoutParams(mScrollItems.get(i).getLayoutParams());
            marginParams.setMargins(0, (int)margin, 0, (int)-margin);
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(marginParams);
            mScrollItems.get(i).setLayoutParams(layoutParams);
        }
        return false;
    }

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
    float velocityY) {
        // Fling event occurred.  Notification of this one happens after an "up" event.
        Log.i(TAG, "Fling" + getTouchType(e1));
        return false;
    }

    @Override
    public void onShowPress(MotionEvent e) {
        // User performed a down event, and hasn't moved yet.
        Log.i(TAG, "Show Press" + getTouchType(e));
    }

    @Override
    public boolean onDown(MotionEvent e) {
        // "Down" event - User touched the screen.
        Log.i(TAG, "Down" + getTouchType(e));
        return false;
    }

    @Override
    public boolean onDoubleTap(MotionEvent e) {
        // User tapped the screen twice.
        Log.i(TAG, "Double tap" + getTouchType(e));
        return false;
    }

    @Override
    public boolean onDoubleTapEvent(MotionEvent e) {
        // Since double-tap is actually several events which are considered one aggregate
        // gesture, there's a separate callback for an individual event within the doubletap
        // occurring.  This occurs for down, up, and move.
        Log.i(TAG, "Event within double tap" + getTouchType(e));
        return false;
    }

    @Override
    public boolean onSingleTapConfirmed(MotionEvent e) {
        // A confirmed single-tap event has occurred.  Only called when the detector has
        // determined that the first tap stands alone, and is not part of a double tap.
        Log.i(TAG, "Single tap confirmed" + getTouchType(e));
        return false;
    }
    // END_INCLUDE(init_gestureListener)


    /**
     * Returns a human-readable string describing the type of touch that triggered a MotionEvent.
     */

    private static String getTouchType(MotionEvent e){

        String touchTypeDescription = " ";
        int touchType = e.getToolType(0);

        switch (touchType) {
            case MotionEvent.TOOL_TYPE_FINGER:
                touchTypeDescription += "(finger)";
                break;
            case MotionEvent.TOOL_TYPE_STYLUS:
                touchTypeDescription += "(stylus, ";
                //Get some additional information about the stylus touch
                float stylusPressure = e.getPressure();
                touchTypeDescription += "pressure: " + stylusPressure;

                if(Build.VERSION.SDK_INT >= 21) {
                    touchTypeDescription += ", buttons pressed: " + getButtonsPressed(e);
                }

                touchTypeDescription += ")";
                break;
            case MotionEvent.TOOL_TYPE_ERASER:
                touchTypeDescription += "(eraser)";
                break;
            case MotionEvent.TOOL_TYPE_MOUSE:
                touchTypeDescription += "(mouse)";
                break;
            default:
                touchTypeDescription += "(unknown tool)";
                break;
        }

        return touchTypeDescription;
    }

    /**
     * Returns a human-readable string listing all the stylus buttons that were pressed when the
     * input MotionEvent occurred.
     */
    @TargetApi(21)
    private static String getButtonsPressed(MotionEvent e){
        String buttons = "";

        if(e.isButtonPressed(MotionEvent.BUTTON_PRIMARY)){
            buttons += " primary";
        }

        if(e.isButtonPressed(MotionEvent.BUTTON_SECONDARY)){
            buttons += " secondary";
        }

        if(e.isButtonPressed(MotionEvent.BUTTON_TERTIARY)){
            buttons += " tertiary";
        }

        if(e.isButtonPressed(MotionEvent.BUTTON_BACK)){
            buttons += " back";
        }

        if(e.isButtonPressed(MotionEvent.BUTTON_FORWARD)){
            buttons += " forward";
        }

        if (buttons.equals("")){
            buttons = "none";
        }

        return buttons;
    }

}
