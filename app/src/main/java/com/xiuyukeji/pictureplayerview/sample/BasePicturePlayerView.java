package com.xiuyukeji.pictureplayerview.sample;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.util.AttributeSet;
import android.view.TextureView;

import com.xiuyukeji.pictureplayerview.sample.utils.FpsMeasureUtil;
import com.xiuyukeji.pictureplayerview.sample.utils.FpsMeasureUtil.OnFpsListener;

/**
 * 基础类，实现fps计算
 *
 * @author Created by jz on 2017/3/30 11:01
 */
public abstract class BasePicturePlayerView extends TextureView implements TextureView.SurfaceTextureListener {

    private FpsMeasureUtil mFpsMeasureUtil;
    private OnFpsListener mOnFpsListener;

    public BasePicturePlayerView(Context context) {
        super(context);
    }

    public BasePicturePlayerView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public BasePicturePlayerView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
        if (mFpsMeasureUtil == null)
            mFpsMeasureUtil = new FpsMeasureUtil();
        mFpsMeasureUtil.measureFps();
        if (mOnFpsListener != null)
            mOnFpsListener.onFps(mFpsMeasureUtil.getFpsText());
    }

    public void setOnFpsListener(OnFpsListener l) {
        this.mOnFpsListener = l;
    }


    public abstract void start(String[] paths, long duration);
}
