package com.example.aspectj;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;

import androidx.annotation.Nullable;

/**
 * Copyright (C) 2013, Xiaomi Inc. All rights reserved.
 */

public class VideoMoveSplashAdMoveLineView extends View {

    private Paint mPaint;
    private float dx,sx,tx,dy,sy,ty,mv;
    private Canvas mCanvas;
    private Bitmap mBitmap;
    private boolean isMove = false;
    private boolean disPathMoveListener = false;
    private OnMoveListener mOnMoveListener;


    public interface OnMoveListener {
        void onMove();
    }

    public VideoMoveSplashAdMoveLineView(Context context) {
        super(context);
        init();
    }

    public VideoMoveSplashAdMoveLineView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public VideoMoveSplashAdMoveLineView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        mPaint = new Paint();
        mPaint.setColor(Color.BLUE);
        mPaint.setStyle(Paint.Style.FILL);
        mPaint.setStrokeWidth(50);
        mPaint.setStrokeCap(Paint.Cap.ROUND);
        mPaint.setStrokeJoin(Paint.Join.ROUND);
        mPaint.setAntiAlias(true);
        mv = ViewConfiguration.get(getContext()).getScaledTouchSlop();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);
        if(width>0 && height >0){
            if(mCanvas == null && mBitmap == null){
                mBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
                mCanvas = new Canvas(mBitmap);
            }
        }

    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                sx = dx = event.getX();
                sy = dy = event.getY();
                break;
            case MotionEvent.ACTION_MOVE:
                if(Math.abs(dx-event.getX())>mv || Math.abs(dy-event.getY())>mv ){
                    isMove = true;
                    tx = event.getX();
                    ty = event.getY();
                    mCanvas.drawLine(sx,sy,tx,ty,mPaint);
                    invalidate();
                    sx = tx;
                    sy = ty;
                }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                dx = dy = sx = sy = tx = ty = 0;
                mCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
                if(isMove && !disPathMoveListener && mOnMoveListener != null){
                    disPathMoveListener = true;
                    mOnMoveListener.onMove();
                }
                break;
        }
        return true;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if(sx>0 && sy >0){
            canvas.drawBitmap(mBitmap,0,0,null);
        }
    }

    public void setOnMoveListener(OnMoveListener onMoveListener) {
        mOnMoveListener = onMoveListener;
    }
}
