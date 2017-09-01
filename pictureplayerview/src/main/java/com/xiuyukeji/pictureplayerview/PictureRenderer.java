package com.xiuyukeji.pictureplayerview;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.view.TextureView;

import com.xiuyukeji.pictureplayerview.interfaces.OnErrorListener;
import com.xiuyukeji.pictureplayerview.interfaces.OnStopListener;
import com.xiuyukeji.pictureplayerview.interfaces.OnUpdateListener;

import static com.xiuyukeji.pictureplayerview.annotations.FitSource.FIT_CENTER;
import static com.xiuyukeji.pictureplayerview.annotations.FitSource.FIT_CROP;
import static com.xiuyukeji.pictureplayerview.annotations.FitSource.FIT_HEIGHT;
import static com.xiuyukeji.pictureplayerview.annotations.FitSource.FIT_WIDTH;

/**
 * 图片播放器渲染类
 *
 * @author Created by jz on 2017/4/10 14:51
 */
class PictureRenderer implements PicturePlayer.Renderer {

    private static final int WIDTH = 0, HEIGHT = 1;

    private int mScaleType;//设置缩放类型

    private float mScale;
    private int mWidth;
    private int mHeight;
    private int state = WIDTH;

    private Paint mPaint;
    private Rect mSrcRect;
    private Rect mDstRect;

    private TextureView mTextureView;

    private OnUpdateListener mOnUpdateListener;
    private OnStopListener mOnStopListener;
    private OnErrorListener mOnErrorListener;

    PictureRenderer(boolean isAntiAlias, boolean isFilterBitmap, boolean isDither, int scaleType, TextureView textureView) {
        this.mScaleType = scaleType;
        this.mTextureView = textureView;

        mPaint = new Paint();
        if (isAntiAlias) {
            mPaint.setAntiAlias(true);
        }
        if (isFilterBitmap) {
            mPaint.setFilterBitmap(true);
        }
        if (isDither) {
            mPaint.setDither(true);
        }

        mSrcRect = new Rect();
        mDstRect = new Rect();
    }

    void setScaleType(int scaleType) {
        this.mScaleType = scaleType;
    }

    void drawClear() {
        if (getWidth() == 0 || getHeight() == 0) {
            return;
        }
        Canvas canvas = mTextureView.lockCanvas();
        if (canvas == null) {
            return;
        }
        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
        mTextureView.unlockCanvasAndPost(canvas);
    }

    @Override
    public void onDraw(int frameIndex, Bitmap bitmap) {
        if (mOnUpdateListener != null && frameIndex != -1) {
            mOnUpdateListener.onUpdate(frameIndex);
        }

        if (bitmap == null) {
            return;
        }

        calculateScale(bitmap.getWidth(), bitmap.getHeight());

        Canvas canvas = mTextureView.lockCanvas();
        if (canvas != null) {
            canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);// 清空画布

            int left = 0;
            int top = 0;
            if (state == WIDTH) {
                top = calculateTop();
            } else {
                left = calculateLeft();
            }
            int right = left + mWidth;
            int bottom = top + mHeight;

            mSrcRect.set(0, 0, bitmap.getWidth(), bitmap.getHeight());
            mDstRect.set(left, top, right, bottom);
            canvas.drawBitmap(bitmap, mSrcRect, mDstRect, mPaint);

            mTextureView.unlockCanvasAndPost(canvas);
        }
    }

    //这里默认只计算第一张图片的大小，如果接下来的图片大小不一致可能会变形
    private void calculateScale(int width, int height) {
        if (mScale != 0) {
            return;
        }
        switch (mScaleType) {
            case FIT_WIDTH:
                callWidth(width, height);
                break;
            case FIT_HEIGHT:
                callHeight(width, height);
                break;
            case FIT_CENTER:
                if (getWidth() * height > getHeight() * width) {
                    callHeight(width, height);
                } else {
                    callWidth(width, height);
                }
                break;
            case FIT_CROP:
                if (getWidth() * height > getHeight() * width) {
                    callWidth(width, height);
                } else {
                    callHeight(width, height);
                }
                break;
            default:
                break;
        }
    }

    private void callWidth(int width, int height) {
        mScale = getWidth() / (float) width;
        mWidth = getWidth();
        mHeight = (int) (height * mScale);
        state = WIDTH;
    }

    private void callHeight(int width, int height) {
        mScale = getHeight() / (float) height;
        mWidth = (int) (width * mScale);
        mHeight = getHeight();
        state = HEIGHT;
    }

    private int calculateLeft() {
        return (getWidth() - mWidth) / 2;
    }

    private int calculateTop() {
        return (getHeight() - mHeight) / 2;
    }

    private int getWidth() {
        return mTextureView.getWidth();
    }

    private int getHeight() {
        return mTextureView.getHeight();
    }

    @Override
    public void onStop() {
        mScale = 0;
        if (mOnStopListener != null) {
            mOnStopListener.onStop();
        }
    }

    @Override
    public void onError(String message) {
        if (mOnErrorListener != null) {
            mOnErrorListener.onError(message);
        }
    }

    void setOnUpdateListener(OnUpdateListener l) {
        this.mOnUpdateListener = l;
    }

    void setOnStopListener(OnStopListener l) {
        this.mOnStopListener = l;
    }

    void setOnErrorListener(OnErrorListener l) {
        this.mOnErrorListener = l;
    }
}
