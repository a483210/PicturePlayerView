package com.xiuyukeji.pictureplayerview;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.os.SystemClock;
import android.view.TextureView;

import com.xiuyukeji.pictureplayerview.interfaces.OnErrorListener;
import com.xiuyukeji.pictureplayerview.interfaces.OnStopListener;
import com.xiuyukeji.pictureplayerview.interfaces.OnUpdateListener;
import com.xiuyukeji.pictureplayerview.utils.CacheList;
import com.xiuyukeji.pictureplayerview.utils.ImageUtil;
import com.xiuyukeji.scheduler.OnFrameUpdateListener;
import com.xiuyukeji.scheduler.OnSimpleFrameListener;
import com.xiuyukeji.scheduler.Scheduler;
import com.xiuyukeji.scheduler.SchedulerUtils;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * 播放实现
 *
 * @author Created by jz on 2017/3/26 16:55
 */
class PicturePlayer {

    public static final int FILE = 0, ASSETS = 1;

    public static final int FIT_WIDTH = 0, FIT_HEIGHT = 1, FIT_CENTER = 2, FIT_CROP = 3;

    public static final int MAX_CACHE_NUMBER = 12;

    private Paint mPaint;
    private Rect mSrcRect;
    private Rect mDstRect;

    private int mSource;//设置来源
    private int mScaleType;//设置缩放类型
    private final int mCacheFrameNumber;//最大缓存帧数
    private final int mReusableFrameNumber;//最大复用缓存帧数

    private CacheList<Bitmap> mCacheBitmaps;
    private CacheList<Bitmap> mReusableBitmaps;

    private int mReadFrame;

    private boolean mIsReadCancel;
    private boolean mIsPlayCancel;
    private boolean mIsCancel;

    private String[] mPaths;
    private long mDuration;
    private int mFrameCount;

    private ReadThread mReadThread;
    private Scheduler mScheduler;

    private TextureView mTextureView;

    private OnUpdateListener mOnUpdateListener;
    private OnStopListener mOnStopListener;
    private OnErrorListener mOnErrorListener;

    PicturePlayer(boolean isAntiAlias, int source, int scaleType, int cacheFrameNumber, TextureView textureView) {
        this.mSource = source;
        this.mScaleType = scaleType;
        this.mCacheFrameNumber = cacheFrameNumber;
        this.mReusableFrameNumber = mCacheFrameNumber / 2;
        this.mTextureView = textureView;

        mPaint = new Paint();
        if (isAntiAlias) {
            mPaint.setAntiAlias(true);
            mPaint.setDither(true);
        }
        mSrcRect = new Rect();
        mDstRect = new Rect();

        mCacheBitmaps = new CacheList<>(new Bitmap[mCacheFrameNumber],
                new CacheList.OnRemoveListener<Bitmap>() {
                    @Override
                    public void onRemove(boolean isOverflow, Bitmap value) {
                        mReusableBitmaps.add(value);
                    }
                });
        mReusableBitmaps = new CacheList<>(new Bitmap[mReusableFrameNumber],
                new CacheList.OnRemoveListener<Bitmap>() {
                    @Override
                    public void onRemove(boolean isOverflow, Bitmap value) {
                        if (isOverflow) {
                            ImageUtil.recycleBitmap(value);
                        }
                    }
                });
    }

    void setDataSource(String[] paths, long duration, int frameCount) {
        this.mPaths = paths;
        this.mDuration = duration;
        this.mFrameCount = frameCount;
    }

    void start() {
        mReadThread = new ReadThread();
        mScheduler = new Scheduler(mDuration, mFrameCount,
                new FrameUpdateListener(),
                new FrameListener());
        mScheduler.setSkipFrame(true);
        mReadThread.start();
    }

    boolean pause() {
        return mScheduler.pause();
    }

    boolean resume() {
        return mScheduler.resume();
    }

    void stop() {
        mIsCancel = true;
        mReadThread.interrupt();
        if (mScheduler.isStarted()) {
            mScheduler.stop();
        }
        SchedulerUtils.join(mReadThread);
    }

    void setScaleType(int scaleType) {
        this.mScaleType = scaleType;
    }

    boolean isStarted() {
        return mScheduler != null && mScheduler.isStarted();
    }

    boolean isRunning() {
        return mScheduler != null && mScheduler.isRunning();
    }

    boolean isPaused() {
        return mScheduler != null && mScheduler.isPaused();
    }

    private void error(Exception error) {
        error.printStackTrace();
        PicturePlayer.this.stop();
        if (mOnErrorListener != null) {
            mOnErrorListener.onError("读取图片失败");
        }
    }

    private class ReadThread extends Thread {
        @Override
        public void run() {
            try {
                while (!mIsCancel && mReadFrame < mFrameCount && !mIsPlayCancel) {
                    if (mCacheBitmaps.size() >= mCacheFrameNumber) {
                        SystemClock.sleep(1);
                        continue;
                    }

                    Bitmap bitmap = readBitmap(mPaths[mReadFrame]);

                    if (bitmap == null || bitmap.isRecycled()) {
                        throw new NullPointerException("读取的图片有错误");
                    }

                    mCacheBitmaps.add(bitmap);
                    mReadFrame++;

                    if (mReadFrame == 1) {
                        mScheduler.start();
                    }
                }
            } catch (Exception e) {
                error(e);
            }
            mIsReadCancel = true;
            threadStop();
        }
    }

    private Bitmap readBitmap(String path) throws IOException {
        InputStream is;
        if (mSource == FILE) {
            is = new BufferedInputStream(new FileInputStream(path));
        } else {
            is = mTextureView.getResources().getAssets().open(path);
        }
        BitmapFactory.Options options = getReusableOptions(is);
        Bitmap bmp = BitmapFactory.decodeStream(is, null, options);
        is.close();
        return bmp;
    }

    //实现复用
    private BitmapFactory.Options getReusableOptions(InputStream is) throws IOException {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        options.inSampleSize = 1;
        options.inJustDecodeBounds = true;
        is.mark(is.available());
        BitmapFactory.decodeStream(is, null, options);//获得大小
        options.inJustDecodeBounds = false;
        is.reset();
        Bitmap inBitmap = getBitmapFromReusableSet(options);
        options.inMutable = true;
        if (inBitmap != null) {
            options.inBitmap = inBitmap;
        }
        return options;
    }

    private Bitmap getBitmapFromReusableSet(BitmapFactory.Options options) {
        if (mReusableBitmaps.isEmpty()) {
            return null;
        }
        int count = mReusableBitmaps.size();
        for (int i = 0; i < count; i++) {
            Bitmap item = mReusableBitmaps.get(i);
            if (ImageUtil.canUseForInBitmap(item, options)) {
                return mReusableBitmaps.remove(i);
            }
        }
        return null;
    }

    private class FrameUpdateListener implements OnFrameUpdateListener {

        private static final int WIDTH = 0, HEIGHT = 1;

        private float mScale;
        private int mWidth;
        private int mHeight;
        private int state = WIDTH;

        @Override
        public void onFrameUpdate(long index) {
            int frameIndex = (int) index;

            update(frameIndex);
            Bitmap bitmap = getBitmap(frameIndex);

            if (bitmap == null) {
                return;
            }

            if (mScale == 0) {
                switch (mScaleType) {
                    case FIT_WIDTH:
                        callWidth(bitmap);
                        break;
                    case FIT_HEIGHT:
                        callHeight(bitmap);
                        break;
                    case FIT_CENTER:
                        if (getWidth() * bitmap.getWidth() > getHeight() * bitmap.getHeight()) {
                            callHeight(bitmap);
                        } else {
                            callWidth(bitmap);
                        }
                        break;
                    case FIT_CROP:
                        if (getWidth() * bitmap.getWidth() > getHeight() * bitmap.getHeight()) {
                            callWidth(bitmap);
                        } else {
                            callHeight(bitmap);
                        }
                        break;
                }
            }

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

            mCacheBitmaps.removeFirst();//在这一帧画完后再放进复用池，防止画面撕裂
        }

        private Bitmap getBitmap(int frameIndex) {
            if (mCacheBitmaps.isEmpty()) {
                return null;
            }

            Bitmap bitmap = null;

            int firstIndex = mReadFrame - mCacheBitmaps.size();
            if (frameIndex == firstIndex) {
                bitmap = mCacheBitmaps.getFirst();
            } else if (frameIndex > firstIndex) {
                if (frameIndex >= mReadFrame) {
                    mCacheBitmaps.clear();
                    mReadFrame = frameIndex + 1;
                } else {
                    mCacheBitmaps.removeCount(frameIndex - firstIndex);
                    bitmap = mCacheBitmaps.getFirst();
                }
            }

            return bitmap;
        }

        private void update(int frameIndex) {
            if (mOnUpdateListener != null) {
                mOnUpdateListener.onUpdate(frameIndex);
            }
        }

        private void callWidth(Bitmap bitmap) {
            mScale = getWidth() / (float) bitmap.getWidth();
            mWidth = getWidth();
            mHeight = (int) (bitmap.getHeight() * mScale);
            state = WIDTH;
        }

        private void callHeight(Bitmap bitmap) {
            mScale = getHeight() / (float) bitmap.getHeight();
            mWidth = (int) (bitmap.getWidth() * mScale);
            mHeight = getHeight();
            state = HEIGHT;
        }

        private int calculateTop() {
            return (getHeight() - mHeight) / 2;
        }

        private int calculateLeft() {
            return (getWidth() - mWidth) / 2;
        }
    }

    private class FrameListener extends OnSimpleFrameListener {
        @Override
        public void onStop() {
            mIsPlayCancel = true;
            threadStop();
        }
    }

    private void threadStop() {
        if (!mIsReadCancel || !mIsPlayCancel) {
            return;
        }
        mCacheBitmaps.clear();//这里会把删除的数据提交到mReusableBitmaps
        int count = mReusableBitmaps.size();
        for (int i = 0; i < count; i++) {
            ImageUtil.recycleBitmap(mReusableBitmaps.removeFirst());
        }

        mReadFrame = 0;

        mIsReadCancel = false;
        mIsPlayCancel = false;
        mIsCancel = false;

        if (mOnStopListener != null) {
            mOnStopListener.onStop();
        }
    }

    private int getWidth() {
        return mTextureView.getWidth();
    }

    private int getHeight() {
        return mTextureView.getHeight();
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