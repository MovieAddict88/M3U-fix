package com.cinecraze.free;

import android.content.Context;
import android.util.AttributeSet;

import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout;

import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;

public class CustomPlayerView extends PlayerView {

    private GestureDetector gestureDetector;
    private ScaleGestureDetector scaleGestureDetector;
    private int currentResizeMode = 0;
    private static final int[] RESIZE_MODES = {
            AspectRatioFrameLayout.RESIZE_MODE_FIT,
            AspectRatioFrameLayout.RESIZE_MODE_FILL,
            AspectRatioFrameLayout.RESIZE_MODE_ZOOM
    };

    public CustomPlayerView(Context context) {
        super(context);
        init();
    }

    public CustomPlayerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public CustomPlayerView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        gestureDetector = new GestureDetector(getContext(), new PlayerGestureListener());
        scaleGestureDetector = new ScaleGestureDetector(getContext(), new ScaleListener());
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        gestureDetector.onTouchEvent(ev);
        scaleGestureDetector.onTouchEvent(ev);
        return super.onTouchEvent(ev);
    }

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            if (detector.getScaleFactor() > 1) {
                // Zoom in - switch to fill mode
                setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_FILL);
                if (resizeModeListener != null) {
                    resizeModeListener.onResizeModeChanged(AspectRatioFrameLayout.RESIZE_MODE_FILL);
                }
            } else if (detector.getScaleFactor() < 1) {
                // Zoom out - switch to fit mode
                setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_FIT);
                if (resizeModeListener != null) {
                    resizeModeListener.onResizeModeChanged(AspectRatioFrameLayout.RESIZE_MODE_FIT);
                }
            }
            return true;
        }
    }

    private class PlayerGestureListener extends GestureDetector.SimpleOnGestureListener {

        private boolean isHorizontalScroll;
        private boolean isVerticalScroll;
        private static final int SEEK_SENSITIVITY = 1000; // milliseconds per pixel

        @Override
        public boolean onDown(MotionEvent e) {
            isHorizontalScroll = false;
            isVerticalScroll = false;
            return super.onDown(e);
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            if (!isVerticalScroll && Math.abs(distanceX) > Math.abs(distanceY)) {
                isHorizontalScroll = true;
            } else if (!isHorizontalScroll) {
                isVerticalScroll = true;
            }

            if (isHorizontalScroll) {
                // Horizontal scroll for seeking
                float seekDelta = (e2.getX() - e1.getX()) * SEEK_SENSITIVITY / getWidth();
                if (playerScrollListener != null) {
                    playerScrollListener.onHorizontalScroll(seekDelta);
                }
            } else if (isVerticalScroll) {
                // Vertical scroll for volume/brightness
                float volumeDelta = (e1.getY() - e2.getY()) / getHeight();
                boolean isRightSide = e1.getX() > getWidth() / 2;
                if (playerScrollListener != null) {
                    playerScrollListener.onVerticalScroll(volumeDelta, isRightSide);
                }
            }
            return true;
        }

        @Override
        public boolean onDoubleTap(MotionEvent e) {
            if (e.getX() > getWidth() / 2) {
                // Double tap on the right side - fast forward
                if (playerDoubleTapListener != null) {
                    playerDoubleTapListener.onDoubleTapForward();
                }
            } else {
                // Double tap on the left side - rewind
                if (playerDoubleTapListener != null) {
                    playerDoubleTapListener.onDoubleTapRewind();
                }
            }
            return true;
        }

        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {
            // Toggle controls visibility
            if (playerTapListener != null) {
                playerTapListener.onSingleTap();
            }
            return true;
        }
    }

    // Resize mode cycling
    public void cycleResizeMode() {
        currentResizeMode = (currentResizeMode + 1) % RESIZE_MODES.length;
        setResizeMode(RESIZE_MODES[currentResizeMode]);
        if (resizeModeListener != null) {
            resizeModeListener.onResizeModeChanged(RESIZE_MODES[currentResizeMode]);
        }
    }

    public int getCurrentResizeMode() {
        return RESIZE_MODES[currentResizeMode];
    }

    private PlayerDoubleTapListener playerDoubleTapListener;
    private PlayerScrollListener playerScrollListener;
    private PlayerTapListener playerTapListener;
    private ResizeModeListener resizeModeListener;

    public void setPlayerDoubleTapListener(PlayerDoubleTapListener playerDoubleTapListener) {
        this.playerDoubleTapListener = playerDoubleTapListener;
    }

    public void setPlayerScrollListener(PlayerScrollListener playerScrollListener) {
        this.playerScrollListener = playerScrollListener;
    }

    public void setPlayerTapListener(PlayerTapListener playerTapListener) {
        this.playerTapListener = playerTapListener;
    }

    public void setResizeModeListener(ResizeModeListener resizeModeListener) {
        this.resizeModeListener = resizeModeListener;
    }

    public interface PlayerDoubleTapListener {
        void onDoubleTapForward();
        void onDoubleTapRewind();
    }

    public interface PlayerScrollListener {
        void onHorizontalScroll(float seekDelta);
        void onVerticalScroll(float volumeDelta, boolean isRightSide);
    }

    public interface PlayerTapListener {
        void onSingleTap();
    }

    public interface ResizeModeListener {
        void onResizeModeChanged(int resizeMode);
    }
}
