package com.artifex.mupdf.viewer.gp.util;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.AttributeSet;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.ScaleAnimation;
import android.widget.ImageView;

import com.artifex.mupdf.viewer.R;

/**
 * Created by p1025 on 23.07.2015.
 */
public class CustomPulseProgress extends ViewGroup {

    private ImageView img1;
    private ImageView img2;
    private ImageView img3;

    ScaleAnimation scale1;
    ScaleAnimation scale11;
    ScaleAnimation scale2;
    ScaleAnimation scale22;
    ScaleAnimation scale3;
    ScaleAnimation scale33;

    public CustomPulseProgress(Context context) {
        super(context);
        init(context);
    }

    public CustomPulseProgress(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public void init(Context context){

        Drawable icon = getContext().getResources().getDrawable(R.drawable.progress_icon);
        icon.setColorFilter(ThemeColor.getInstance().getForegroundColorFilter());

        img1 = new ImageView(context);
        img1.layout(0, 15, 10, 25);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN)
            img1.setBackground(icon);
        else
            img1.setBackgroundDrawable(icon);
        img1.setVisibility(GONE);
        this.addView(img1);


        img2 = new ImageView(context);
        img2.layout(15, 15, 25, 25);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN)
            img2.setBackground(icon);
        else
            img2.setBackgroundDrawable(icon);
        img2.setVisibility(GONE);
        this.addView(img2);


        img3 = new ImageView(context);
        img3.layout(30, 15, 40, 25);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN)
            img3.setBackground(icon);
        else
            img3.setBackgroundDrawable(icon);
        img3.setVisibility(GONE);
        this.addView(img3);

        startAnim();
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {

    }

    public void startAnim() {

        scale1 = new ScaleAnimation(0f, 1.0f, 0f, 1.0f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        scale1.setDuration(950);
        scale1.setInterpolator(new DecelerateInterpolator());
        scale1.setStartOffset(0);
        scale1.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                img1.startAnimation(scale11);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });

        scale11 = new ScaleAnimation(1f, 0f, 1f, 0f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        scale11.setDuration(350);
        scale11.setInterpolator(new DecelerateInterpolator());
        scale11.setStartOffset(600);
        scale11.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                img1.startAnimation(scale1);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });

        scale2 = new ScaleAnimation(0f, 1.0f, 0f, 1.0f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        scale2.setDuration(650);
        scale2.setInterpolator(new DecelerateInterpolator());
        scale2.setStartOffset(300);
        scale2.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                img2.startAnimation(scale22);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        scale22 = new ScaleAnimation(1f, 0f, 1f, 0f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        scale22.setDuration(650);
        scale22.setInterpolator(new DecelerateInterpolator());
        scale22.setStartOffset(300);
        scale22.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                img2.startAnimation(scale2);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });

        scale3 = new ScaleAnimation(0f, 1.0f, 0f, 1.0f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        scale3.setDuration(350);
        scale3.setInterpolator(new DecelerateInterpolator());
        scale3.setStartOffset(600);
        scale3.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                img3.startAnimation(scale33);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });

        scale33 = new ScaleAnimation(1f, 0f, 1f, 0f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        scale33.setDuration(950);
        scale33.setInterpolator(new DecelerateInterpolator());
        scale33.setStartOffset(0);
        scale33.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                img3.startAnimation(scale3);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });

        img1.setVisibility(VISIBLE);
        img2.setVisibility(VISIBLE);
        img3.setVisibility(VISIBLE);

        img1.startAnimation(scale1);
        img2.startAnimation(scale2);
        img3.startAnimation(scale3);
    }

    public void stopAnim() {
        img1.clearAnimation();
        img1.invalidate();

        img2.clearAnimation();
        img2.invalidate();

        img3.clearAnimation();
        img3.invalidate();

    }
}
