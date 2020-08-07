package com.example.aspectj;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;

import androidx.annotation.Nullable;
import androidx.dynamicanimation.animation.DynamicAnimation;
import androidx.dynamicanimation.animation.SpringAnimation;

/**
 * Copyright (C) 2013, Xiaomi Inc. All rights reserved.
 */

public class ShakeImageAnimation extends ImageView {

    public ShakeImageAnimation(Context context) {
        super(context);
    }

    public ShakeImageAnimation(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public ShakeImageAnimation(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void startAnimation(){
        startshakeAnimation(new DynamicAnimation.OnAnimationEndListener() {
            @Override
            public void onAnimationEnd(DynamicAnimation animation, boolean canceled, float value, float velocity) {
                startAlpahAnimation(new Animator.AnimatorListener() {
                    @Override
                    public void onAnimationStart(Animator animation) {

                    }

                    @Override
                    public void onAnimationEnd(Animator animation) {

                    }

                    @Override
                    public void onAnimationCancel(Animator animation) {

                    }

                    @Override
                    public void onAnimationRepeat(Animator animation) {

                    }
                });
            }
        });
    }

    private void startAlpahAnimation(Animator.AnimatorListener animationListener){
       ObjectAnimator objectAnimator = ObjectAnimator.ofFloat(this,"alpha",1f,0.4f,1f,0.4f,1f);
        objectAnimator.setDuration(3000);
        objectAnimator.setInterpolator(new LinearInterpolator());
        objectAnimator.addListener(animationListener);
        objectAnimator.start();
    }

    private void startshakeAnimation(DynamicAnimation.OnAnimationEndListener onAnimationEndListener){
        SpringAnimation signUpBtnAnimY = new SpringAnimation(this, DynamicAnimation.ROTATION,0);
        signUpBtnAnimY.getSpring().setStiffness(700);
        signUpBtnAnimY.getSpring().setDampingRatio(0.25f);
        signUpBtnAnimY.setStartVelocity(1000);
        signUpBtnAnimY.addEndListener(onAnimationEndListener);
        signUpBtnAnimY.start();
    }

    public void stopAnimation(){

    }

}
