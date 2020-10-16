package com.example.android.basicgesturedetect;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import java.util.ArrayList;

public class WalletLayoutSolver extends GestureDetector.SimpleOnGestureListener
{
    public static class Animator
    {
        private static final int FRAME_TIME = 1; // in milisecs
        private long mTimeOrigin;
        private float mDuration;
        public boolean mIsAwake;

        public Animator(float duration)
        {
            mDuration = duration;
        }

        public void awake()
        {
            mTimeOrigin = System.currentTimeMillis();
            onAwake();
            if(!mIsAwake)
            {
                mIsAwake = true;
                update();
            }
        }

        public void destroy()
        {
            mIsAwake = false;
            onDestroy(true);
        }

        public void update()
        {
            if(!mIsAwake)
            {
                return;
            }
            float time = (System.currentTimeMillis() - mTimeOrigin)/mDuration;
            time = CMath.clamp(0.0f,1.0f, time);
            onUpdate(time);
            if(time >= 1.0f)
            {
                mIsAwake = false;
                onDestroy(false);
                return;
            }
            // TODO: this is bad for performance, do not create object every frame
            final Handler handler = new Handler(Looper.getMainLooper());
            handler.postDelayed(new Runnable() {
                public void run() {
                    update();
                }
            }, FRAME_TIME);
        }

        public void onAwake(){}

        public void onDestroy(boolean premature){}

        public void onUpdate(float time){}
    }

    public static class CMath
    {
        public static float easeOutBack(float from, float to, float alpha)
        {
            final float c1 = 1.70158f;
            final float c2 = c1 + 1.0f;
            float r = (float) (1.0f + c2 * Math.pow(alpha - 1.0f, 3) + c1 * Math.pow(alpha - 1.0f, 2));
            float d = to - from;
            return from + d * r;
        }
        public static float easeInOutBack(float from, float to, float alpha)
        {
            final float c1 = 1.70158f;
            final float c2 = c1 * 1.525f;
            float alpha2 = alpha*2.0f;
            float r = (float) (alpha < 0.5f
                    ? (Math.pow(alpha2, 2) * ((c2 + 1.0f) * alpha2 - c2)) / 2.0f
                    : (Math.pow(alpha2 - 2.0f, 2) * ((c2 + 1) * (alpha2 - 2.0f) + c2) + 2.0f) / 2.0f);
            float d = to - from;
            return from + d * r;
        }

        public static float lerp(float from, float to, float alpha)
        {
            return from + (to - from)*alpha;
        }
        public static float lerpInvert(float from, float to, float alpha)
        {
            return to + (from - to)*(1.0f-alpha);
        }

        public static float clamp(float min, float max, float alpha)
        {
            return Math.min(max, Math.max(min, alpha));
        }
        public static int clamp(int min, int max, int alpha)
        {
            return Math.min(max, Math.max(min, alpha));
        }
    }

    private enum State
    {
        Intro,
        List,
        ListToSingle,
        Single,
        SingleToList,
        Edit
    }
    private static final long INTRO_TO_LIST_TIME = 1800; // in milisecs
    private static final long LIST_TO_SINGLE_TIME = 2500; // in milisecs
    private static final long SINGLE_TO_LIST_TIME = 2500; // in milisecs
    private static final long CAMERA_BOUNCE_TIME = 1000; // in milisecs
    private static final long POSITION_ADJUST_TIME = 1000; // in milisecs
    private static final float STACK_RATIO = 0.2f;
    private static final int SINGLE_OUT_OFFSET = 300;
    private static final int CUSTOM_LONG_PRESS_DURATION = 250;
    private static final float CUSTOM_LONG_PRESS_DISTANCE_THRESHOLD = 20f;
    private static final boolean USE_ABSOLUTE_LAYOUT = true;

    private boolean mIsWaitingForLongPress;
    private float mCameraY;
    private float mCameraYTarget;
    private State mState;
    private int mFocusedItem;
    private ArrayList<Float> mItemPositions;
    private ArrayList<Float> mItemPositionTargets;
    private int mEditingItem;
    private int mEditingItemTarget;
    private float mEditingCursor;
    private float mEditingCursorDy;
    private float mStackedHeight;
    private float mStackedHeightTarget;
    private float mItemHeight;

    private ViewGroup mStack;
    private Activity mParentActivity;
    private int mItemTemplate;
    private GestureDetector mGestureDetector;
    private MotionEvent mCurrentDownEvent;

    private Animator introToList;
    private Animator singleToList;
    private Animator listToSingle;
    private Animator cameraBounce;
    private Animator positionAdjust;

    public WalletLayoutSolver()
    {
        mCameraY = 0.0f;
        mState = State.Intro;
        mFocusedItem = -1;
        mIsWaitingForLongPress = false;
        introToList = new Animator(INTRO_TO_LIST_TIME){
            @Override
            public void onAwake()
            {
                mState = State.Intro;
                recalculateItemHeight();
                mStackedHeightTarget = mStackedHeight;
                mStackedHeight = mItemHeight;
                mCameraYTarget = 0;
            }
            @Override
            public void onDestroy(boolean premature)
            {
                mStackedHeight = calculateStackHeight();
                recalculateItemPosition();
                reanimateItemPosition();
                mState = State.List;
            }
            @Override
            public void onUpdate(float time)
            {
                mStackedHeight = CMath.lerp(mStackedHeight, mStackedHeightTarget, time);
                if(mStack.getChildCount() > 0)
                {
                    float alpha = mStack.getChildAt(0).getAlpha();
                    alpha = CMath.lerp(alpha, 1.0f, time);
                    for (int i = 0; i < mStack.getChildCount(); i++) {
                        View view = mStack.getChildAt(i);
                        view.setAlpha(alpha);
                    }
                }
                recalculateItemPosition();
            }
        };
        singleToList = new Animator(LIST_TO_SINGLE_TIME){
            @Override
            public void onAwake()
            {
                mCameraYTarget = calculateCameraClamped();
                mStackedHeight = mItemHeight*STACK_RATIO;
                reanimateItemPosition();
                mState = State.SingleToList;
                onLeaveCard(mFocusedItem, mStack.getChildAt(mFocusedItem));
            }
            @Override
            public void onDestroy(boolean premature)
            {
                mFocusedItem = -1;
                mState = State.List;
                updateStack();
            }
            @Override
            public void onUpdate(float time)
            {
                mCameraY = CMath.lerp(mCameraY, mCameraYTarget, time);
                for(int i = 0;i<mStack.getChildCount();i++) {
                    float y = mItemPositions.get(i);
                    float target = mItemPositionTargets.get(i);
                    y = CMath.lerp(y, target, time);
                    mItemPositions.set(i, y);
                }
                updateStack();
            }
        };
        listToSingle = new Animator(SINGLE_TO_LIST_TIME){
            @Override
            public void onAwake()
            {
                for(int i = 0;i<mStack.getChildCount();i++)
                {
                    float y = mCameraY;
                    if(i < mFocusedItem)
                    {
                        y -= mStack.getHeight()*2.0f;
                        y -= (mFocusedItem - i)*mStackedHeight;
                    }
                    if(i > mFocusedItem)
                    {
                        y += mStack.getHeight()* 2.0f;
                        y += (i - mFocusedItem)*mStackedHeight;
                    }
                    mItemPositionTargets.set(i, y);
                }
                mState = State.ListToSingle;
                //mCameraYTarget = CMath.clamp(0, mStackedHeight * mStack.getChildCount(), mCameraYTarget);
                onEnterCard(mFocusedItem, mStack.getChildAt(mFocusedItem));
            }
            @Override
            public void onDestroy(boolean premature)
            {
                mState = State.Single;
                updateStack();
            }
            @Override
            public void onUpdate(float time)
            {
                //mCameraY = CMath.lerp(mCameraY, mCameraYTarget, time);
                for(int i = 0;i<mStack.getChildCount();i++) {
                    float y = mItemPositions.get(i);
                    float target = mItemPositionTargets.get(i);
                    y = CMath.lerp(y, target, time);
                    mItemPositions.set(i, y);
                }
                updateStack();
            }
        };
        cameraBounce = new Animator(CAMERA_BOUNCE_TIME){
            @Override
            public void onAwake()
            {
                if(mState != State.Single) {
                    mCameraYTarget = calculateCameraClamped();
                }
                if(Math.abs(mCameraYTarget - mCameraY) < 0.01f)
                {
                    destroy();
                }
            }
            @Override
            public void onUpdate(float time)
            {
                mCameraY = CMath.lerp(mCameraY, mCameraYTarget, time);
                calculateStackStretch();
                if(mState == State.List) {
                    recalculateItemPosition();
                }
                else
                {
                    updateStack();
                }
            }
        };
        positionAdjust = new Animator(POSITION_ADJUST_TIME){
            @Override
            public void onUpdate(float time)
            {
                for(int i = 0;i<mStack.getChildCount();i++)
                {
                    float y = mItemPositions.get(i);
                    float target = mItemPositionTargets.get(i);
                    if(mState == State.Edit)
                    {
                        if(i == mEditingItemTarget)
                        {
                            mItemPositions.set(i, mEditingCursor);
                        }
                        else if(i == mEditingItem)
                        {
                            float target_mid = mItemPositionTargets.get(i) + mStackedHeight*2.0f;
                            if(time < 0.5f) {
                                y = CMath.lerpInvert(y, target_mid, time*2.0f);
                            }
                            else {
                                y = CMath.lerp(y, target, (time-0.5f)*2.0f);
                            }
                        }
                    }
                    else
                    {
                        y = CMath.lerp(y, target, time);
                    }
                    mItemPositions.set(i,y);
                }
                updateStack();
            }
        };
        mItemPositions = new ArrayList<>();
        mItemPositionTargets = new ArrayList<>();
    }

    public WalletLayoutSolver setParentActivity(Activity activity)
    {
        mParentActivity = activity;
        mGestureDetector = new GestureDetector(mParentActivity, this);
        return this;
    }

    public WalletLayoutSolver setStackLayout(ViewGroup layout)
    {
        mStack = layout;
        mStack.setClickable(true);
        mStack.setFocusable(true);
        return this;
    }

    public WalletLayoutSolver setItemTemplate(int id)
    {
        mItemTemplate = id;
        return this;
    }

    public void onBind(int index, View view)
    {
    }

    public void onEnterDetailConfirmed(int index)
    {
    }

    public WalletLayoutSolver populate(int count)
    {
        if(mStack == null)
        {
            return this;
        }
        int delta = count - mStack.getChildCount();
        for(int i = 0;i < delta;i++)
        {
            try
            {
                mParentActivity.getLayoutInflater().inflate(mItemTemplate, mStack);
                mItemPositions.add(0.0f);
                mItemPositionTargets.add(0.0f);
            }
            catch (Exception e)
            {
                return this;
            }
        }
        for(int i = delta;i < 0;i++)
        {
            mStack.removeViewAt(mStack.getChildCount() - 1);
            mItemPositions.remove(mItemPositions.size() - 1);
            mItemPositionTargets.remove(mItemPositionTargets.size() - 1);
        }
        for(int i = 0;i<mStack.getChildCount();i++)
        {
            View view = mStack.getChildAt(i);
            view.setAlpha(0.0f);
            view.setElevation(i);
            onBind(i, view);
        }
        updateStack();
        mStack.postDelayed(new Runnable()
        {
            @Override
            public void run()
            {
                for(int i = 0;i<mStack.getChildCount();i++)
                {
                    View view = mStack.getChildAt(i);
                    view.setAlpha(1.0f);
                }
                recalculateItemHeight();
                introToList.awake();
                recalculateItemHeight();
                mStack.setOnTouchListener(new View.OnTouchListener() {
                    @Override
                    public boolean onTouch(View v, MotionEvent event) {
                        mGestureDetector.onTouchEvent(event);
                        onTouchEvent(event);
                        return false;
                    }
                });
                updateStack();
            }
        }, 200);
        return this;
    }

    @Override
    public boolean onSingleTapUp(MotionEvent e) {
        // Up motion completing a single tap occurred.
        return false;
    }

    @Override
    public void onLongPress(MotionEvent e) {
        // Touch has been long enough to indicate a long press.
        // Does not indicate motion is complete yet (no up event necessarily)

    }

    public void onCustomLongPress(MotionEvent e)
    {
        if(mState != State.List && mState != State.Single)
        {
            return;
        }
        float y = e.getY() + mCameraY;
        if(y < 0 || y > mStackedHeight *(mStack.getChildCount() - 1) + mItemHeight)
        {
            return;
        }
        if(mState == State.Single)
        {
            onLeaveCard(mFocusedItem, mStack.getChildAt(mFocusedItem));
        }
        int index = mState == State.Single?mFocusedItem:((int) (y / mStackedHeight));
        index = CMath.clamp(0, mStack.getChildCount() - 1, index);
        mEditingCursor = index * mStackedHeight;
        mEditingCursorDy = y - mEditingCursor;
        reanimateItemPosition();
        editOn(index);
    }


    private void recalculateItemHeight()
    {
        if(mStack.getChildCount() > 0)
        {
            View img = mStack.getChildAt(0);
            mItemHeight = img.getBottom() - img.getTop();
            float h = mItemHeight * STACK_RATIO;
            if(mState != State.Intro)
            {
                mStackedHeight = h;
            }
            else
            {
                mStackedHeightTarget = h;
            }
        }
    }
    private void recalculateItemElevation()
    {
        for(int i = 0;i<mStack.getChildCount();i++)
        {
            View item = mStack.getChildAt(i);
            item.setElevation(i);
        }
    }

    public void onTouchEvent(MotionEvent e)
    {
        if (e.getAction() == MotionEvent.ACTION_DOWN)
        {
            mIsWaitingForLongPress = true;
            mCurrentDownEvent = MotionEvent.obtain(e);
            final Handler handler = new Handler(Looper.getMainLooper());
            handler.postDelayed(new Runnable() {
                @Override
                public void run()
                {
                    if (mIsWaitingForLongPress) {
                        onCustomLongPress(mCurrentDownEvent);
                    }
                }
            }, CUSTOM_LONG_PRESS_DURATION);
            cameraBounce.destroy();
            if(mState == State.SingleToList) {
                singleToList.destroy();
            }
            if(mState == State.ListToSingle) {
                listToSingle.destroy();
            }
        }
        else if (e.getAction() == MotionEvent.ACTION_UP)
        {
            mIsWaitingForLongPress = false;
            if(mState == State.List)
            {
                cameraBounce.awake();
            }
            if(mState == State.Single)
            {
                float item_y = mItemPositions.get(mFocusedItem);
                float delta = item_y - mCameraY;
                if(delta > SINGLE_OUT_OFFSET)
                {
                    singleToList.awake();
                }
                else
                {
                    mCameraYTarget = mItemPositions.get(mFocusedItem);
                    cameraBounce.awake();
                }
            }
        }
        else if (e.getAction() == MotionEvent.ACTION_MOVE)
        {
            if(mIsWaitingForLongPress)
            {
                float dx = mCurrentDownEvent.getX() - e.getX();
                float dy = mCurrentDownEvent.getY() - e.getY();
                float d = dx * dx + dy * dy;
                if (d > CUSTOM_LONG_PRESS_DISTANCE_THRESHOLD)
                {
                    mIsWaitingForLongPress = false;
                }
            }
        }
        if(mState != State.Edit)
        {
            return;
        }
        if (e.getAction() == MotionEvent.ACTION_UP || e.getAction() == MotionEvent.ACTION_CANCEL)
        {
            editOff();
            updateStack();
        }
        else if(e.getAction() == MotionEvent.ACTION_MOVE)
        {
            float y = e.getY() + mCameraY;
            recalculateEditIndex(y);
        }
    }

    private void recalculateEditIndex(float cursor)
    {
        mEditingCursor = cursor - mEditingCursorDy;
        mItemPositions.set(mEditingItemTarget, mEditingCursor);
        float sensor_cursor = mEditingCursor + mStackedHeight/2;
        int index = (int)Math.floor(sensor_cursor / mStackedHeight);
        index = CMath.clamp(-1,mStack.getChildCount() - 1, index);
        if(mEditingItemTarget != index)
        {
            mEditingItem = mEditingItemTarget;
            View item = mStack.getChildAt(mEditingItemTarget);
            mStack.removeViewAt(mEditingItemTarget);
            mStack.addView(item,index);
            item.setElevation(index);
            float position = mItemPositions.get(mEditingItemTarget);
            mItemPositions.remove(mEditingItemTarget);
            mItemPositions.add(index, position);
            mEditingItemTarget = index;
            positionAdjust.awake();
            for(int i = 0; i<mStack.getChildCount();i++)
            {
                if(i >= Math.min(mEditingItemTarget, mEditingItem) &&
                        i <= Math.max(mEditingItemTarget, mEditingItem))
                {
                    continue;
                }
                mItemPositions.set(i, mItemPositionTargets.get(i));
            }
        }
        updateStack();
    }
    private void recalculateItemPosition()
    {
        for(int i = 0; i< mStack.getChildCount(); i++)
        {
            float pos = Math.max(i * mStackedHeight, calculateCameraClamped());
            mItemPositions.set(i, pos);
        }
        updateStack();
    }

    private void reanimateItemPosition()
    {
        for(int i = 0; i< mStack.getChildCount(); i++)
        {
            float pos = Math.max(i * mStackedHeight, calculateCameraClamped());
            mItemPositionTargets.set(i, pos);
        }
        //positionAdjust.awake();
    }

    private float calculateStackHeight()
    {
        return mItemHeight * STACK_RATIO;
    }

    private float calculateCameraClamped()
    {
        return CMath.clamp(0.0f, calculateCameraMax(), mCameraY);
    }

    private void calculateStackStretch()
    {
        //mCameraY = calculateCameraClamped();
        float delta = mCameraY - calculateCameraClamped();
        float stretch_amount = 0.05f*delta;
        mStackedHeight = calculateStackHeight() - stretch_amount;
        mStackedHeight = CMath.clamp(1.0f, mItemHeight, mStackedHeight);
    }


    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY)
    {
        mCameraY += distanceY;
        calculateStackStretch();
        mCameraYTarget = mCameraY;
        if(mState == State.List) {
            recalculateItemPosition();
        }
        else
        {
            updateStack();
        }
        return false;
    }

    private float calculateCameraMax()
    {
        float h = mItemHeight * STACK_RATIO;
        return Math.max(0, h * mStack.getChildCount() - mStack.getHeight() - h + mItemHeight);
    }

    private void updateStack()
    {
        if(mItemHeight <= 0)
        {
            return;
        }
        float camera = mState == State.List?calculateCameraClamped():mCameraY;
        for(int i = 0; i< mStack.getChildCount(); i++)
        {
            View item = mStack.getChildAt(i);
            float target = mItemPositions.get(i);
            if(USE_ABSOLUTE_LAYOUT)
            {
                float margin = target - camera;
                ViewGroup.MarginLayoutParams marginParams = new ViewGroup.MarginLayoutParams(item.getLayoutParams());
                marginParams.setMargins(0, (int) margin, 0, (int) -margin);
                FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(marginParams);
                item.setLayoutParams(params);
            }
            else
            {
                float origin = i * mItemHeight;
                float margin = target - origin - camera;
                ViewGroup.MarginLayoutParams marginParams = new ViewGroup.MarginLayoutParams(mStack.getChildAt(i).getLayoutParams());
                marginParams.setMargins(0, (int) margin, 0, (int) -margin);
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(marginParams);
                mStack.getChildAt(i).setLayoutParams(params);
            }
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
        return false;
    }

    public void onLeaveCard(int index, View view)
    {
    }

    public void onEnterCard(int index, View view)
    {

    }

    private void editOn(int index)
    {
        mEditingItem = index;
        mEditingItemTarget = index;
        mState = State.Edit;
        View item = mStack.getChildAt(mEditingItem);
        item.setElevation(9999);
        item.setAlpha(0.8f);
    }

    private void editOff()
    {
        mEditingItem = mEditingItemTarget;
        View item = mStack.getChildAt(mEditingItemTarget);
        item.setAlpha(1.0f);
        mState = State.List;
        recalculateItemElevation();
        reanimateItemPosition();
        positionAdjust.awake();
        //editToList.awake();
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
            if(y < 0 || y > mStackedHeight *(mStack.getChildCount() - 1) + mItemHeight)
            {
                return false;
            }
            int i = (int) (y / mStackedHeight);
            i = CMath.clamp(0, mStack.getChildCount() - 1, i);
            mFocusedItem = i;
            listToSingle.awake();
        }
        else if(mState == State.Single)
        {
            onEnterDetailConfirmed(mFocusedItem);
        }
        return false;
    }
}