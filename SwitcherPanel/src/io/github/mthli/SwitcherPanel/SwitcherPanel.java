package io.github.mthli.SwitcherPanel;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
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
    private Drawable shadowDrawable;

    private int contentHeight = 0; // TODO
    private int slideRange = 0;
    private float slideOffset = 1f;

    private static final int SHADOW_HEIGHT_DEFAULT = 4;
    private int shadowHeight = SHADOW_HEIGHT_DEFAULT;
    public void setShadowHeight(int shadowHeight) {
        this.shadowHeight = shadowHeight;
    }

    private static final int FLING_VELOCITY_DEFAULT = 400;
    public void setFlingVelocity(int flingVelocity) {
        if (dragHelper != null) {
            dragHelper.setMinVelocity(flingVelocity * ViewUnit.getDensity(getContext()));
        }
    }

    public enum Status {
        SHOW,
        HIDE
    }
    private static final Status STATUS_DEFAULT = Status.HIDE;
    private Status status = STATUS_DEFAULT;

    public interface StatusListener {
        void onShow();
        void onHide();
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
        public void onViewDragStateChanged(int state) {
            if (dragHelper.getViewDragState() == ViewDragHelper.STATE_IDLE) {
                slideOffset = computeSlideOffset(contentView.getTop());
                if (slideOffset == 1 && status != Status.SHOW) {
                    status = Status.SHOW;
                    dispatchOnShow();
                } else if (slideOffset == 0 && status != Status.HIDE) {
                    status = Status.HIDE;
                    dispatchOnHide();
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

    public SwitcherPanel(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    // TODO: contentHeight
    public SwitcherPanel(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);

        this.shadowDrawable = getResources().getDrawable(R.drawable.shadow, null);
        this.dragHelper = ViewDragHelper.create(this, 0.5f, new DragHelperCallback());
        setFlingVelocity(FLING_VELOCITY_DEFAULT);
        setWillNotDraw(false);
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
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);
        setMeasuredDimension(widthSize, heightSize);

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
                slideRange = contentView.getMeasuredHeight() - contentHeight;
            }
        }
    }

    @Override
    protected void onLayout(boolean change, int l, int t, int r, int b) {
        int paddingLeft = getPaddingLeft();
        int paddingTop = getPaddingTop();

        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(0);
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
    }

    @Override
    public void draw(@NonNull Canvas canvas) {
        super.draw(canvas);

        if (shadowDrawable != null && shadowHeight > 0) {
            shadowDrawable.setBounds(
                    contentView.getLeft(),
                    contentView.getTop() - shadowHeight,
                    contentView.getRight(),
                    contentView.getTop()
            );
            shadowDrawable.draw(canvas);
        }
    }

    private int computeTopPosition(float slideOffset) {
        int slidePixelOffset = (int) (slideOffset * slideRange);
        return getMeasuredHeight() - getPaddingBottom() - contentHeight - slidePixelOffset;
    }

    private float computeSlideOffset(int topPosition) {
        int topBoundCollapsed = computeTopPosition(0f);
        return (topBoundCollapsed - topPosition) / slideRange;
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
        if (!isEnabled()) {
            dragHelper.cancel();
            return super.onInterceptTouchEvent(motionEvent);
        }

        int action = motionEvent.getActionMasked();
        if (action == MotionEvent.ACTION_CANCEL || action == MotionEvent.ACTION_UP) {
            dragHelper.cancel();
            return false;
        }

        return dragHelper.shouldInterceptTouchEvent(motionEvent);
    }

    @Override
    public boolean onTouchEvent(@NonNull MotionEvent motionEvent) {
        if (!isEnabled()) {
            return super.onTouchEvent(motionEvent);
        }

        dragHelper.processTouchEvent(motionEvent);
        return true;
    }

    private void dispatchOnShow() {
        if (statusListener != null) {
            statusListener.onShow();
        }
    }

    private void dispatchOnHide() {
        if (statusListener != null) {
            statusListener.onHide();
        }
    }

    private void setAllChildrenVisible() {
        for (int i = 0, childCount = getChildCount(); i < childCount; i++) {
            final View child = getChildAt(i);
            if (child.getVisibility() == INVISIBLE) {
                child.setVisibility(VISIBLE);
            }
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

    public void show() {
        status = Status.SHOW;
        smoothSlideTo(1f);
        dispatchOnShow();
    }

    public void hide() {
        status = Status.HIDE;
        smoothSlideTo(0f);
        dispatchOnHide();
    }
}
