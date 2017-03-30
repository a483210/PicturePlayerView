package com.xiuyukeji.pictureplayerview;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.util.AttributeSet;
import android.view.TextureView;
import android.view.TextureView.SurfaceTextureListener;

import com.xiuyukeji.pictureplayerview.interfaces.OnChangeListener;
import com.xiuyukeji.pictureplayerview.interfaces.OnErrorListener;
import com.xiuyukeji.pictureplayerview.interfaces.OnStopListener;
import com.xiuyukeji.pictureplayerview.interfaces.OnUpdateListener;

/**
 * 图片播放器
 *
 * @author Created by jz on 2017/3/26 16:07
 */
public class PicturePlayerView extends TextureView implements SurfaceTextureListener {

    protected static final String TAG = "PicturePlayerView";

    private static final int STOP = 0, START = 1, STOPPING = 2;

    private boolean mIsLoop;//是否循环播放
    private boolean mIsOpaque;//背景是否透明
    private boolean mIsAntiAlias;//是否抗锯齿
    private int mSource;//设置来源
    private int mScaleType;//设置缩放类型

    private PicturePlayer mPlayerCore;

    private int mState = STOP;
    private boolean mIsRelease = false;

    private boolean mIsWaitPlay;

    private boolean mIsEnabled = true;

    private OnChangeListener mOnChangeListener;
    private OnStopListener mOnStopListener;
    private NoticeHandler mNoticeHandler;

    public PicturePlayerView(Context context) {
        this(context, null);
    }

    public PicturePlayerView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PicturePlayerView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initAttrs(attrs);
        findView();
        initView();
        setListener();
    }

    private void initAttrs(AttributeSet attrs) {
        if (attrs == null)
            return;
        TypedArray typedArray = getContext().obtainStyledAttributes(attrs, R.styleable.PicturePlayerView);
        mIsLoop = typedArray.getBoolean(R.styleable.PicturePlayerView_loop, false);
        mIsOpaque = typedArray.getBoolean(R.styleable.PicturePlayerView_opaque, true);
        mIsAntiAlias = typedArray.getBoolean(R.styleable.PicturePlayerView_antiAlias, true);
        mSource = typedArray.getInt(R.styleable.PicturePlayerView_source, PicturePlayer.FILE);
        mScaleType = typedArray.getInt(R.styleable.PicturePlayerView_scaleType, PicturePlayer.FIT_WIDTH);
        typedArray.recycle();
    }

    private void findView() {
        mNoticeHandler = new NoticeHandler();

        mPlayerCore = new PicturePlayer(mIsAntiAlias, mSource, mScaleType, this, mNoticeHandler);
    }

    private void initView() {
        setOpaque(mIsOpaque);
    }

    private void setListener() {
        setSurfaceTextureListener(this);
        mNoticeHandler.setOnStopListener(new OnStopListener() {
            @Override
            public void onStop() {
                if (!mIsLoop || mState == STOPPING) {//如果不循环或者已经调用stop方法则停止
                    drawClear();
                    mState = STOP;
                    if (mOnStopListener != null)
                        mOnStopListener.onStop();
                    if (mIsWaitPlay) {
                        start();
                        mIsWaitPlay = false;
                    }
                } else {//重新开始播放
                    mPlayerCore.start();
                }
            }
        });
    }

    @Override
    public void setEnabled(boolean enabled) {
        this.mIsEnabled = enabled;
    }

    @Override
    public boolean isEnabled() {
        return mIsEnabled;
    }

    /**
     * 设置数据源
     *
     * @param path     文件夹地址
     * @param names    名称集合
     * @param duration 总时长
     */
    public void setDataSource(String path, String[] names, long duration) {
        int count = names.length;
        String[] paths = new String[names.length];
        for (int i = 0; i < count; i++)
            paths[i] = path + "/" + names[i];
        setDataSource(paths, duration);
    }

    /**
     * 设置数据源
     *
     * @param paths    地址集合
     * @param duration 总时长
     */
    public void setDataSource(String[] paths, long duration) {
        if (mState != STOP) {
            return;
        }
        mPlayerCore.setDataSource(paths, duration, paths.length);
    }

    /**
     * 开始播放
     */
    public void start() {
        if (!mIsEnabled)
            return;
        if (mState == START)
            return;
        if (mState == STOPPING) {
            mIsWaitPlay = true;
            return;
        }

        mState = START;

        mPlayerCore.start();
    }

    /**
     * 停止播放
     */
    public void stop() {
        if (mState == STOP || mState == STOPPING) {
            return;
        }

        cancel();

        mState = STOPPING;
    }

    /**
     * 只有在停止播放时设置该值有效
     *
     * @param scaleType 值
     */
    public void setScaleType(int scaleType) {
        if (mState != STOP) {
            return;
        }
        mPlayerCore.setScaleType(scaleType);
    }

    /**
     * 设置是否循环播放
     *
     * @param isLoop 值
     */
    public void setLoop(boolean isLoop) {
        this.mIsLoop = isLoop;
    }

    private void cancel() {
        mPlayerCore.cancel();
    }

    /**
     * 释放
     */
    public void release() {
        stop();
        mIsRelease = true;
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        drawClear();
        if (mOnChangeListener != null) {
            mOnChangeListener.onCreated();
        }
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        stop();
        if (mOnChangeListener != null) {
            mOnChangeListener.onDestroyed();
        }
        return true;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
    }

    private void drawClear() {
        if (mIsRelease) {
            return;
        }
        if (getWidth() == 0 || getHeight() == 0) {
            return;
        }
        Canvas canvas = lockCanvas(new Rect(0, 0, getWidth(), getHeight()));
        if (canvas == null) {
            return;
        }
        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
        unlockCanvasAndPost(canvas);
    }

    public boolean isPlaying() {
        return mState != STOP;
    }

    public void setOnUpdateListener(OnUpdateListener l) {
        this.mNoticeHandler.setOnUpdateListener(l);
    }

    public void setOnStopListener(OnStopListener l) {
        this.mOnStopListener = l;
    }

    public void setOnErrorListener(OnErrorListener l) {
        this.mNoticeHandler.setOnErrorListener(l);
    }

    public void setOnChangeListener(OnChangeListener l) {
        this.mOnChangeListener = l;
    }

}
