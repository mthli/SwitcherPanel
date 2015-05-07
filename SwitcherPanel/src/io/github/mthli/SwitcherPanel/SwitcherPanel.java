package io.github.mthli.SwitcherPanel;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.annotation.NonNull;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.ViewDragHelper;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

public class SwitcherPanel extends ViewGroup {
    private View switcherView;
    private View contentView;

    private int coverHeight = 0;
    private int slideRange = 0;
    private float slideOffset = 1f;

    private static final float PARALLAX_OFFSET_DEFAULT = 64f;
    private float parallaxOffset = PARALLAX_OFFSET_DEFAULT;
    public float getParallaxOffset() {
        return parallaxOffset;
    }
    public void setParallaxOffset(float parallaxOffset) {
        this.parallaxOffset = parallaxOffset * ViewUnit.getDensity(getContext());
    }

    private static final int FLING_VELOCITY_DEFAULT = 256;
    private int flingVelocity = FLING_VELOCITY_DEFAULT;
    public int getFlingVelocity() {
        return flingVelocity;
    }
    public void setFlingVelocity(int flingVelocity) {
        this.flingVelocity = flingVelocity;
        if (dragHelper != null) {
            dragHelper.setMinVelocity(flingVelocity * ViewUnit.getDensity(getContext()));
        }
    }

    public enum Status {
        EXPANDED,
        COLLAPSED,
        FLING
    }
    private static final Status STATUS_DEFAULT = Status.EXPANDED;
    private Status status = STATUS_DEFAULT;
    public Status getStatus() {
        return status;
    }

    public interface StatusListener {
        void onExpanded();
        void onCollapsed();
        void onFling();
    }
    private StatusListener statusListener;
    public void setStatusListener(StatusListener statusListener) {
        this.statusListener = statusListener;
    }

    private ViewDragHelper dragHelper;
    private class DragHelperCallback extends ViewDragHelper.Callback {
        @Override
        public boolean tryCaptureView(View child, int pointerId) {
            return child == contentView;
        }

        @Override
        public void onViewCaptured(View capturedChild, int activePointerId) {
            setAllChildrenVisible();
        }

        @Override
        public int clampViewPositionVertical(View child, int top, int dy) {
            int hideTop = computeTopPosition(0f);
            int showTop = computeTopPosition(1f);
            return Math.min(Math.max(top, showTop), hideTop);
        }

        @Override
        public int getViewVerticalDragRange(View child) {
            return slideRange;
        }

        @Override
        public void onViewPositionChanged(View changedView, int left, int top, int dx, int dy) {
            fling(top);
            invalidate();
        }

        @Override
        public void onViewDragStateChanged(int state) {
            if (dragHelper.getViewDragState() == ViewDragHelper.STATE_IDLE) {
                slideOffset = computeSlideOffset(contentView.getTop());
                applyParallaxForCurrentSlideOffset();

                if (slideOffset == 1f && status != Status.EXPANDED) {
                    status = Status.EXPANDED;
                    dispatchOnExpanded();
                } else if (slideOffset == 0f && status != Status.COLLAPSED) {
                    status = Status.COLLAPSED;
                    dispatchOnCollapsed();
                }
            }
        }

        @Override
        public void onViewReleased(View view, float x, float y) {
            int target;
            float direction = -y;
            if (direction > 0) {
                target = computeTopPosition(1f);
            } else if (direction < 0) {
                target = computeTopPosition(0f);
            } else {
                target = computeTopPosition(0f);
            }

            dragHelper.settleCapturedViewAt(view.getLeft(), target);
            invalidate();
        }
    }

    public static class LayoutParams extends ViewGroup.MarginLayoutParams {
        private static final int[] ATTRS = new int[] {
                android.R.attr.layout_weight
        };

        public LayoutParams() {
            super(MATCH_PARENT, MATCH_PARENT);
        }

        public LayoutParams(int width, int height) {
            super(width, height);
        }

        public LayoutParams(android.view.ViewGroup.LayoutParams source) {
            super(source);
        }

        public LayoutParams(MarginLayoutParams source) {
            super(source);
        }

        public LayoutParams(LayoutParams source) {
            super(source);
        }

        public LayoutParams(Context c, AttributeSet attrs) {
            super(c, attrs);
            TypedArray typedArray = c.obtainStyledAttributes(attrs, ATTRS);
            typedArray.recycle();
        }
    }

    public SwitcherPanel(Context context) {
        this(context, null);
    }

    public SwitcherPanel(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    @SuppressWarnings("getResource().getDrawable(int) is deprecated.")
    public SwitcherPanel(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        this.dragHelper = ViewDragHelper.create(this, 0.5f, new DragHelperCallback());
        setFlingVelocity(FLING_VELOCITY_DEFAULT);

        coverHeight = (int) ViewUnit.dp2px(context, 100); // TODO
    }

    @Override
    protected ViewGroup.LayoutParams generateDefaultLayoutParams() {
        return new LayoutParams();
    }

    @Override
    protected ViewGroup.LayoutParams generateLayoutParams(ViewGroup.LayoutParams layoutParams) {
        return layoutParams instanceof MarginLayoutParams ? new LayoutParams((MarginLayoutParams) layoutParams) : new LayoutParams(layoutParams);
    }

    @Override
    public ViewGroup.LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new LayoutParams(getContext(), attrs);
    }

    @Override
    protected boolean checkLayoutParams(ViewGroup.LayoutParams layoutParams) {
        return layoutParams instanceof LayoutParams && super.checkLayoutParams(layoutParams);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (MeasureSpec.getMode(widthMeasureSpec) != MeasureSpec.EXACTLY) {
            throw new IllegalStateException("Width must have an exact value or MATCH_PARENT.");
        } else if (MeasureSpec.getMode(heightMeasureSpec) != MeasureSpec.EXACTLY) {
            throw new IllegalStateException("Height must have an exact value or MATCH_PARENT.");
        } else if (getChildCount() != 2) {
            throw new IllegalStateException("SwitcherPanel layout must have exactly 2 children!");
        }

        switcherView = getChildAt(0);
        contentView = getChildAt(1);

        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);
        int layoutWidth = widthSize - getPaddingLeft() - getPaddingRight();
        int layoutHeight = heightSize - getPaddingTop() - getPaddingBottom();

        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            LayoutParams layoutParams = (LayoutParams) child.getLayoutParams();

            int width = layoutWidth;
            int height = layoutHeight;
            if (child == switcherView) {
                width = width - layoutParams.leftMargin - layoutParams.rightMargin;
            } else if (child == contentView) {
                height = height - layoutParams.topMargin;
            }

            int childWidthSpec;
            if (layoutParams.width == ViewGroup.LayoutParams.WRAP_CONTENT) {
                childWidthSpec = MeasureSpec.makeMeasureSpec(width, MeasureSpec.AT_MOST);
            } else if (layoutParams.width == ViewGroup.LayoutParams.MATCH_PARENT) {
                childWidthSpec = MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY);
            } else {
                childWidthSpec = MeasureSpec.makeMeasureSpec(layoutParams.width, MeasureSpec.EXACTLY);
            }

            int childHeightSpec;
            if (layoutParams.height == ViewGroup.LayoutParams.WRAP_CONTENT) {
                childHeightSpec = MeasureSpec.makeMeasureSpec(height, MeasureSpec.AT_MOST);
            } else if (layoutParams.height == ViewGroup.LayoutParams.MATCH_PARENT) {
                childHeightSpec = MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY);
            } else {
                childHeightSpec = MeasureSpec.makeMeasureSpec(layoutParams.height, MeasureSpec.EXACTLY);
            }

            child.measure(childWidthSpec, childHeightSpec);
            if (child == contentView) {
                slideRange = contentView.getMeasuredHeight() - coverHeight;
            }
        }

        setMeasuredDimension(widthSize, heightSize);
    }

    @Override
    protected void onLayout(boolean change, int l, int t, int r, int b) {
        int paddingLeft = getPaddingLeft();
        int paddingTop = getPaddingTop();

        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            LayoutParams layoutParams = (LayoutParams) child.getLayoutParams();

            int top = paddingTop;
            if (child == contentView) {
                top = computeTopPosition(slideOffset);
            }
            int height = child.getMeasuredHeight();
            int bottom = top + height;
            int left = paddingLeft + layoutParams.leftMargin;
            int right = left + child.getMeasuredWidth();
            child.layout(left, top, right, bottom);
        }
        applyParallaxForCurrentSlideOffset(); // TODO
    }

    private int computeTopPosition(float slideOffset) {
        int slidePixelOffset = (int) (slideOffset * slideRange);
        return getMeasuredHeight() - getPaddingBottom() - coverHeight - slidePixelOffset;
    }

    private float computeSlideOffset(int topPosition) {
        return (computeTopPosition(0f) - ((float) topPosition)) / ((float) slideRange);
    }

    @Override
    public void computeScroll() {
        if (dragHelper != null && dragHelper.continueSettling(true)) {
            if (!isEnabled()) {
                dragHelper.abort();
                return;
            }
            ViewCompat.postInvalidateOnAnimation(this);
        }
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent motionEvent) {
        int action = motionEvent.getActionMasked();
        if (!isEnabled() || action == MotionEvent.ACTION_CANCEL) {
            return super.onInterceptTouchEvent(motionEvent);
        }

        if (shouldShowContentView(motionEvent)) {
            expanded();
            return true;
        }
        return super.onInterceptTouchEvent(motionEvent);
    }

    private boolean shouldShowContentView(@NonNull MotionEvent motionEvent) {
        int[] location = new int[2];
        contentView.getLocationOnScreen(location);

        int left = location[0];
        int right = left + contentView.getWidth();
        int top = location[1];
        int bottom = top + contentView.getHeight();
        return status == Status.COLLAPSED
                && left <= motionEvent.getRawX()
                && motionEvent.getRawX() <= right
                && top <= motionEvent.getRawY()
                && motionEvent.getRawY() <= bottom;
    }

    public void expanded() {
        smoothSlideTo(1f);
        status = Status.EXPANDED;
    }

    public void collapsed() {
        smoothSlideTo(0f);
        status = Status.COLLAPSED;
    }

    private void fling(int top) {
        status = Status.FLING;
        slideOffset = computeSlideOffset(top);

        applyParallaxForCurrentSlideOffset();
        dispatchOnFling();

        LayoutParams layoutParams = (LayoutParams) switcherView.getLayoutParams();
        int defaultHeight = getHeight() - getPaddingBottom() - getPaddingTop() - coverHeight;
        if (slideOffset < 0) {
            layoutParams.height = top - getPaddingBottom();
            switcherView.requestLayout();
        } else if (layoutParams.height != defaultHeight) {
            layoutParams.height = defaultHeight;
            switcherView.requestLayout();
        }
    }

    private void dispatchOnExpanded() {
        if (statusListener != null) {
            statusListener.onExpanded();
        }
    }

    private void dispatchOnCollapsed() {
        if (statusListener != null) {
            statusListener.onCollapsed();
        }
    }

    private void dispatchOnFling() {
        if (statusListener != null) {
            statusListener.onFling();
        }
    }

    private boolean smoothSlideTo(float slideOffset) {
        if (!isEnabled()) {
            return false;
        }

        int top = computeTopPosition(slideOffset);
        if (dragHelper.smoothSlideViewTo(contentView, contentView.getLeft(), top)) {
            setAllChildrenVisible();
            ViewCompat.postInvalidateOnAnimation(this);
            return true;
        }
        return false;
    }

    private void setAllChildrenVisible() {
        for (int i = 0, childCount = getChildCount(); i < childCount; i++) {
            final View child = getChildAt(i);
            if (child.getVisibility() == INVISIBLE) {
                child.setVisibility(VISIBLE);
            }
        }
    }

    private void applyParallaxForCurrentSlideOffset() {
        if (parallaxOffset > 0) {
            switcherView.setTranslationY(-(parallaxOffset * Math.max(slideOffset, 0)));
        }
    }
}
