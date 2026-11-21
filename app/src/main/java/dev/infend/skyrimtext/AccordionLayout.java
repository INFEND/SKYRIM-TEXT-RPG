package dev.infend.skyrimtext;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.LinearLayout;

/**
 * A single clipped height animation, instead of AutoTransition's sequential
 * fade/bounds/fade. Children are laid out at their natural size throughout.
 * The requested state, not transient visibility, controls repeated taps.
 */
final class AccordionLayout extends LinearLayout {
    private ValueAnimator motion;
    private boolean targetOpen;
    private int animatedHeight = -1;
    private int naturalHeight;

    AccordionLayout(Context context) {
        super(context);
        setOrientation(VERTICAL);
        setClipChildren(true);
    }

    boolean isExpanded() {
        return motion != null ? targetOpen : getVisibility() == VISIBLE;
    }

    void setExpanded(boolean open, boolean animate) {
        int start = animatedHeight >= 0 ? animatedHeight :
            (getVisibility() == VISIBLE ? getHeight() : 0);
        stopMotion();
        targetOpen = open;
        if (!animate || !isAttachedToWindow() || !ValueAnimator.areAnimatorsEnabled()) {
            finish(open);
            return;
        }
        ViewGroup parent = getParent() instanceof ViewGroup ? (ViewGroup) getParent() : null;
        int width = getWidth();
        if (parent != null && parent.getWidth() > 0) {
            width = parent.getWidth() - parent.getPaddingLeft() - parent.getPaddingRight();
            if (getLayoutParams() instanceof ViewGroup.MarginLayoutParams) {
                ViewGroup.MarginLayoutParams p = (ViewGroup.MarginLayoutParams) getLayoutParams();
                width -= p.leftMargin + p.rightMargin;
            }
        }
        if (width <= 0) { finish(open); return; }
        // Measure once with unrestricted height; never fade or hide the body mid-motion.
        animatedHeight = -1;
        setVisibility(VISIBLE);
        measure(MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
        int end = open ? getMeasuredHeight() : 0;
        if (start == end) { finish(open); return; }
        animatedHeight = start;
        setImportantForAccessibility(open ? IMPORTANT_FOR_ACCESSIBILITY_AUTO :
                IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
        requestLayout();
        motion = ValueAnimator.ofInt(start, end);
        motion.setDuration(180);
        motion.setInterpolator(new AccelerateDecelerateInterpolator());
        motion.addUpdateListener(value -> {
            animatedHeight = (Integer) value.getAnimatedValue();
            requestLayout();
            invalidate();
        });
        motion.addListener(new AnimatorListenerAdapter() {
            @Override public void onAnimationEnd(Animator animation) {
                if (motion == animation) {
                    motion = null;
                    finish(targetOpen);
                }
            }
        });
        motion.start();
    }

    private void stopMotion() {
        if (motion != null) {
            ValueAnimator old = motion;
            motion = null;
            old.removeAllListeners();
            old.removeAllUpdateListeners();
            old.cancel();
        }
    }

    private void finish(boolean open) {
        animatedHeight = -1;
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_AUTO);
        setVisibility(open ? VISIBLE : GONE);
        requestLayout();
        invalidate();
    }

    @Override protected void onMeasure(int widthSpec, int heightSpec) {
        super.onMeasure(widthSpec, animatedHeight >= 0 ?
            MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED) : heightSpec);
        naturalHeight = getMeasuredHeight();
        if (animatedHeight >= 0) setMeasuredDimension(getMeasuredWidth(), animatedHeight);
    }

    @Override protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right,
            animatedHeight >= 0 ? top + naturalHeight : bottom);
    }

    @Override protected void dispatchDraw(Canvas canvas) {
        int save = canvas.save();
        canvas.clipRect(0, 0, getWidth(), getHeight());
        super.dispatchDraw(canvas);
        canvas.restoreToCount(save);
    }

    @Override protected void onDetachedFromWindow() {
        if (motion != null) {
            stopMotion();
            finish(targetOpen);
        }
        super.onDetachedFromWindow();
    }

    static void rotateArrow(View arrow, boolean open, boolean animate) {
        arrow.animate().cancel();
        if (animate && ValueAnimator.areAnimatorsEnabled())
            arrow.animate().rotation(open ? 180f : 0f).setDuration(180).start();
        else arrow.setRotation(open ? 180f : 0f);
    }
}
