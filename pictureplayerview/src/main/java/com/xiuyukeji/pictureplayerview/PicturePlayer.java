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
import com.xiuyukeji.scheduler.OnFrameUpdateListener;
import com.xiuyukeji.scheduler.OnSimpleFrameListener;
import com.xiuyukeji.scheduler.Scheduler;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

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

    private int mSource;//设置来源
    private int mScaleType;//设置缩放类型
    private final int mCacheFrameNumber;//最大缓存帧数
    private final int mReusableFrameNumber;//最大复用缓存帧数

    private int mCacheCount;
    private Bitmap[] mCacheBitmaps;
    private ArrayList<Bitmap> mReusableBitmaps;

    private int mReadIndex;
    private int mPlayIndex;

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
        this.mReusableFrameNumber = mCacheFrameNumber + mCacheFrameNumber / 2;
        this.mTextureView = textureView;

        mPaint = new Paint();
        if (isAntiAlias) {
            mPaint.setAntiAlias(true);
            mPaint.setDither(true);
            mPaint.setFilterBitmap(true);
        }

        mCacheBitmaps = new Bitmap[mCacheFrameNumber];
        mReusableBitmaps = new ArrayList<>();
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
        join();
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

    private void join() {
        if (Thread.currentThread().getId() == mReadThread.getId()) {//如果是同一个线程调用则不等待，防止死循环
            return;
        }

        try {
            mReadThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private class ReadThread extends Thread {
        @Override
        public void run() {
            try {
                while (!mIsCancel && mReadFrame < mFrameCount && !mIsPlayCancel) {
                    if (mCacheCount >= mCacheFrameNumber) {
                        SystemClock.sleep(1);
                        continue;
                    }

                    Bitmap bmp = readBitmap(mPaths[mReadFrame]);

                    if (bmp == null || bmp.isRecycled()) {
                        throw new NullPointerException("读取的图片有错误");
                    }

                    mCacheBitmaps[mReadIndex++] = bmp;

                    if (mReadIndex >= mCacheFrameNumber) {
                        mReadIndex = 0;
                    }
                    mReadFrame++;
                    mCacheCount++;

                    if (mReadFrame == 1) {
                        mScheduler.start();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                PicturePlayer.this.stop();
                if (mOnErrorListener != null) {
                    mOnErrorListener.onError("读取图片失败");
                }
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
            if (ImageCache.canUseForInBitmap(item, options)) {
                return mReusableBitmaps.remove(i);
            }
        }
        for (int i = mReusableFrameNumber; i < count; i++) {
            ImageCache.recycleBitmap(mReusableBitmaps.remove(0));
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
        public void onFrameUpdate(long frameIndex) {
            update(frameIndex);
            if (mCacheCount <= 0) {
                return;
            } else if (frameIndex >= mReadFrame) {
                for (int i = 0; i < mCacheCount; i++) {
                    Bitmap bitmap = mCacheBitmaps[i];
                    if (bitmap != null) {
                        mReusableBitmaps.add(bitmap);
                    }
                    mCacheBitmaps[i] = null;
                }
                mReadFrame = (int) (frameIndex + 1);
                mReadIndex = 0;
                mCacheCount = 0;
                mPlayIndex = 0;
                return;
            }

            Bitmap bitmap = mCacheBitmaps[mPlayIndex];

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
            Canvas canvas = mTextureView.lockCanvas(new Rect(0, 0, getWidth(), getHeight()));
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

            Rect src = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
            Rect dst = new Rect(left, top, right, bottom);
            canvas.drawBitmap(bitmap, src, dst, mPaint);

            mTextureView.unlockCanvasAndPost(canvas);

            mCacheBitmaps[mPlayIndex] = null;
            mReusableBitmaps.add(bitmap);//将已经用过的Bitmap放回复用集合

            mPlayIndex++;
            if (mPlayIndex >= mCacheFrameNumber) {
                mPlayIndex = 0;
            }

            mCacheCount--;
        }

        private void update(long frameIndex) {
            if (mOnUpdateListener != null) {
                mOnUpdateListener.onUpdate((int) frameIndex);
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
        for (int i = 0; i < mCacheFrameNumber; i++) {
            ImageCache.recycleBitmap(mCacheBitmaps[i]);
            mCacheBitmaps[i] = null;
        }
        mCacheCount = 0;
        for (Bitmap bitmap : mReusableBitmaps) {
            ImageCache.recycleBitmap(bitmap);
        }
        mReusableBitmaps.clear();

        mReadIndex = 0;
        mPlayIndex = 0;
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