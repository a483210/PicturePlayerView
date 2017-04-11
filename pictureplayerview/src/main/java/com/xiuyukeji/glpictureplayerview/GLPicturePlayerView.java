package com.xiuyukeji.glpictureplayerview;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.SurfaceTexture;
import android.opengl.GLSurfaceView;
import android.support.annotation.IntRange;
import android.support.annotation.NonNull;
import android.util.AttributeSet;

import com.xiuyukeji.pictureplayerview.NoticeHandler;
import com.xiuyukeji.pictureplayerview.PicturePlayer;
import com.xiuyukeji.pictureplayerview.R;
import com.xiuyukeji.pictureplayerview.interfaces.OnChangeListener;
import com.xiuyukeji.pictureplayerview.interfaces.OnErrorListener;
import com.xiuyukeji.pictureplayerview.interfaces.OnStopListener;
import com.xiuyukeji.pictureplayerview.interfaces.OnUpdateListener;

import minus.android.support.opengl.GLTextureView;

/**
 * gl实现图片播放器
 *
 * @author Created by jz on 2017/4/7 9:29
 */
public class GLPicturePlayerView extends GLTextureView {

    protected static final String TAG = "GLPicturePlayerView";

    private static final int STOP = 0, START = 1, PAUSE = 2;

    private boolean mIsLoop;//是否循环播放
    private boolean mIsOpaque;//背景是否透明
    private int mSource;//设置来源
    private int mScaleType;//设置缩放类型
    private int mCacheFrameNumber;//缓存帧数

    private PicturePlayer mPlayer;
    private GLPictureRenderer mRenderer;

    private int mState = STOP;

    private boolean mIsEnabled = true;

    private OnChangeListener mOnChangeListener;
    private NoticeHandler mNoticeHandler;

    public GLPicturePlayerView(Context context) {
        this(context, null);
    }

    public GLPicturePlayerView(Context context, AttributeSet attrs) {
        super(context, attrs);
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
        mSource = typedArray.getInt(R.styleable.PicturePlayerView_picture_source, PicturePlayer.FILE);
        mScaleType = typedArray.getInt(R.styleable.PicturePlayerView_picture_scaleType, GLPictureRenderer.FIT_CROP);
        mCacheFrameNumber = typedArray.getInt(R.styleable.PicturePlayerView_picture_cacheFrameNumber, PicturePlayer.MAX_CACHE_NUMBER);
        typedArray.recycle();
    }

    private void findView() {
        mNoticeHandler = new NoticeHandler();

        mRenderer = new GLPictureRenderer(mScaleType, this);
        mPlayer = new PicturePlayer(getContext(), mSource, mCacheFrameNumber, mRenderer);
    }

    private void initView() {
        setOpaque(mIsOpaque);

        setEGLContextClientVersion(2);
        setEGLConfigChooser(8, 8, 8, 8, 0, 0);
        setRenderer(mRenderer);
        setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
    }

    private void setListener() {
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
     * 只有在停止播放时设置该值有效
     *
     * @param scaleType 值
     */
    public void setScaleType(int scaleType) {
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
        super.onSurfaceTextureAvailable(surface, width, height);
        if (mOnChangeListener != null) {
            mOnChangeListener.onCreated();
        }
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        super.onSurfaceTextureSizeChanged(surface, width, height);
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        stop();
        if (mOnChangeListener != null) {
            mOnChangeListener.onDestroyed();
        }
        return super.onSurfaceTextureDestroyed(surface);
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
        super.onSurfaceTextureUpdated(surface);
    }

    public boolean isPaused() {
        return mState == PAUSE;
    }

    public boolean isPlaying() {
        return mState != STOP;
    }

    public void setOnUpdateListener(OnUpdateListener l) {
        this.mNoticeHandler.setOnUpdateListener(l);
    }

    public void setOnStopListener(OnStopListener l) {
        this.mNoticeHandler.setOnStopListener(l);
    }

    public void setOnErrorListener(OnErrorListener l) {
        this.mNoticeHandler.setOnErrorListener(l);
    }

    public void setOnChangeListener(OnChangeListener l) {
        this.mOnChangeListener = l;
    }
}