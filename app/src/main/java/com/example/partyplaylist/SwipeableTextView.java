package com.example.partyplaylist;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;

import androidx.appcompat.widget.AppCompatTextView;

public class SwipeableTextView extends AppCompatTextView {

    public interface OnSwipeListener {
        void onSwipeLeft();
        void onSwipeRight();
    }

    private OnSwipeListener swipeListener;

    public SwipeableTextView(Context context) {
        super(context);
        init();
    }

    public SwipeableTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public SwipeableTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        Log.d("SwipeableTextView", "init() called");
        GestureDetector gestureDetector = new GestureDetector(getContext(), new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                Log.d("SwipeableTextView", "onFling() detected");
                float deltaX = e2.getX() - e1.getX();
                if (Math.abs(deltaX) > Math.abs(e2.getY() - e1.getY())) {
                    if (deltaX > 0) {
                        Log.d("SwipeableTextView", "Swipe Right Detected");
                        if (swipeListener != null) swipeListener.onSwipeRight();
                    } else {
                        Log.d("SwipeableTextView", "Swipe Left Detected");
                        if (swipeListener != null) swipeListener.onSwipeLeft();
                    }
                    return true;
                }
                return false;
            }

            @Override
            public boolean onDown(MotionEvent e) {
                Log.d("SwipeableTextView", "onDown() detected");
                return true; // Returning true allows further gesture events to be detected
            }

            @Override
            public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
                Log.d("SwipeableTextView", "onScroll() detected");
                return true;
            }

        });

        setOnTouchListener((v, event) -> {
            Log.d("SwipeableTextView", "onTouch() event: " + event.getAction());
            return gestureDetector.onTouchEvent(event);
        });
    }

    public void setOnSwipeListener(OnSwipeListener listener) {
        this.swipeListener = listener;
    }
}
