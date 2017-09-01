package com.xiuyukeji.pictureplayerview;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.SurfaceTexture;
import android.support.annotation.IntRange;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.view.TextureView;
import android.view.TextureView.SurfaceTextureListener;

import com.xiuyukeji.pictureplayerview.annotations.FitSource;
import com.xiuyukeji.pictureplayerview.interfaces.OnChangeListener;
import com.xiuyukeji.pictureplayerview.interfaces.OnErrorListener;
import com.xiuyukeji.pictureplayerview.interfaces.OnStopListener;
import com.xiuyukeji.pictureplayerview.interfaces.OnUpdateListener;

import static com.xiuyukeji.pictureplayerview.PicturePlayer.DEFAULT_MAX_CACHE_NUMBER;
import static com.xiuyukeji.pictureplayerview.annotations.FitSource.FIT_CROP;
import static com.xiuyukeji.pictureplayerview.annotations.PictureSource.FILE;

/**
 * 图片播放器
 *
 * @author Created by jz on 2017/3/26 16:07
 */
public class PicturePlayerView extends TextureView implements SurfaceTextureListener {

    protected static final String TAG = "PicturePlayerView";

    private static final int STOP = 0, START = 1, PAUSE = 2;

    private boolean mIsLoop;//是否循环播放
    private boolean mIsOpaque;//背景是否透明
    private boolean mIsAntiAlias;//是否抗锯齿
    private boolean mIsFilterBitmap;//是否位图过滤
    private boolean mIsDither;//是否防抖动
    private int mSource;//设置来源
    private int mScaleType;//设置缩放类型
    private int mCacheFrameNumber;//缓存帧数

    private PicturePlayer mPlayer;
    private PictureRenderer mRenderer;

    private int mState = STOP;

    private boolean mIsEnabled = true;

    private OnChangeListener mOnChangeListener;
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
        if (attrs == null) {
            return;
        }
        TypedArray typedArray = getContext().obtainStyledAttributes(attrs, R.styleable.PicturePlayerView);
        mIsLoop = typedArray.getBoolean(R.styleable.PicturePlayerView_picture_loop, false);
        mIsOpaque = typedArray.getBoolean(R.styleable.PicturePlayerView_picture_opaque, true);
        mIsAntiAlias = typedArray.getBoolean(R.styleable.PicturePlayerView_picture_antiAlias, true);
        mIsFilterBitmap = typedArray.getBoolean(R.styleable.PicturePlayerView_picture_filterBitmap, false);
        mIsDither = typedArray.getBoolean(R.styleable.PicturePlayerView_picture_dither, false);
        mSource = typedArray.getInt(R.styleable.PicturePlayerView_picture_source, FILE);
        mScaleType = typedArray.getInt(R.styleable.PicturePlayerView_picture_scaleType, FIT_CROP);
        mCacheFrameNumber = typedArray.getInt(R.styleable.PicturePlayerView_picture_cacheFrameNumber, DEFAULT_MAX_CACHE_NUMBER);
        typedArray.recycle();
    }

    private void findView() {
        mNoticeHandler = new NoticeHandler();

        mRenderer = new PictureRenderer(mIsAntiAlias, mIsFilterBitmap, mIsDither, mScaleType, this);
        mPlayer = new PicturePlayer(getContext(), mSource, mCacheFrameNumber, mRenderer);
    }

    private void initView() {
        setOpaque(mIsOpaque);
    }

    private void setListener() {
        setSurfaceTextureListener(this);
        mRenderer.setOnUpdateListener(new OnUpdateListener() {
            @Override
            public void onUpdate(int frame) {
                mNoticeHandler.noticeUpdate(frame);
            }
        });
        mRenderer.setOnStopListener(new OnStopListener() {
            @Override
            public void onStop() {
                if (mState != STOP && mIsLoop) {//重新开始播放
                    mPlayer.start();
                } else {
                    mRenderer.drawClear();
                    mState = STOP;
                    mNoticeHandler.noticeStop();
                }
            }
        });
        mRenderer.setOnErrorListener(new OnErrorListener() {
            @Override
            public void onError(String msg) {
                mNoticeHandler.noticeError(msg);
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
    public void setDataSource(@NonNull String path, @NonNull String[] names, @IntRange(from = 1) long duration) {
        int count = names.length;
        String[] paths = new String[names.length];
        for (int i = 0; i < count; i++) {
            paths[i] = String.format("%s/%s", path, names[i]);
        }
        setDataSource(paths, duration);
    }

    /**
     * 设置数据源
     *
     * @param paths    地址集合
     * @param duration 总时长
     */
    public void setDataSource(@NonNull String[] paths, @IntRange(from = 1) long duration) {
        if (mState != STOP) {
            return;
        }
        mPlayer.setDataSource(paths, duration, paths.length);
    }

    /**
     * 开始播放
     */
    public void start() {
        if (!mIsEnabled) {
            return;
        }
        if (mState == START) {
            return;
        }

        mState = START;

        mPlayer.start();
    }

    /**
     * 恢复播放
     */
    public void resume() {
        if (mState != PAUSE || !mPlayer.isStarted()) {
            return;
        }

        if (mPlayer.resume()) {
            mState = START;
        }
    }

    /**
     * 暂停播放
     */
    public void pause() {
        if (mState != START || !mPlayer.isStarted()) {
            return;
        }

        if (mPlayer.pause()) {
            mState = PAUSE;
        }
    }

    /**
     * 停止播放
     */
    public void stop() {
        if (mState == STOP) {
            return;
        }

        mState = STOP;

        mPlayer.stop();
    }

    /**
     * 跳转到某一帧
     *
     * @param frameIndex 帧序列
     */
    public void seekTo(int frameIndex) {
        if (mState == STOP) {
            return;
        }
        if (getFrameIndex() != frameIndex) {
            mPlayer.seekTo(frameIndex);
        }
    }

    /**
     * 只有在停止播放时设置该值有效
     *
     * @param scaleType 值
     */
    public void setScaleType(@FitSource int scaleType) {
        if (mState != STOP) {
            return;
        }
        mRenderer.setScaleType(scaleType);
    }

    /**
     * 设置是否循环播放
     *
     * @param isLoop 值
     */
    public void setLoop(boolean isLoop) {
        this.mIsLoop = isLoop;
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        mRenderer.drawClear();
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

    /**
     * 是否暂停
     */
    public boolean isPaused() {
        return mState == PAUSE;
    }

    /**
     * 是否在播放中
     */
    public boolean isPlaying() {
        return mState != STOP;
    }

    /**
     * 返回当前帧序列
     */
    public int getFrameIndex() {
        return mPlayer.getFrameIndex();
    }

    /**
     * 设置更新回调
     *
     * @param l 回调
     */
    public void setOnUpdateListener(OnUpdateListener l) {
        this.mNoticeHandler.setOnUpdateListener(l);
    }

    /**
     * 设置停止回调
     *
     * @param l 回调
     */
    public void setOnStopListener(OnStopListener l) {
        this.mNoticeHandler.setOnStopListener(l);
    }

    /**
     * 设置错误回调
     *
     * @param l 回调
     */
    public void setOnErrorListener(OnErrorListener l) {
        this.mNoticeHandler.setOnErrorListener(l);
    }

    /**
     * 设置TextureView生命周期回调
     *
     * @param l 回调
     */
    public void setOnChangeListener(OnChangeListener l) {
        this.mOnChangeListener = l;
    }

    /**
     * 解除所有回调，同时停止播放，该方法可以不调用
     */
    public void release() {
        setOnUpdateListener(null);
        setOnStopListener(null);
        setOnErrorListener(null);
        setOnChangeListener(null);
        stop();
    }
}