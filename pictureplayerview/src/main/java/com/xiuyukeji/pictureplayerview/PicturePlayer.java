package com.xiuyukeji.pictureplayerview;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.SystemClock;
import android.support.annotation.IntRange;
import android.support.annotation.NonNull;

import com.xiuyukeji.pictureplayerview.annotations.PictureSource;
import com.xiuyukeji.pictureplayerview.utils.CacheList;
import com.xiuyukeji.pictureplayerview.utils.ImageUtil;
import com.xiuyukeji.scheduler.OnFrameUpdateListener;
import com.xiuyukeji.scheduler.OnSeekToListener;
import com.xiuyukeji.scheduler.OnSimpleFrameListener;
import com.xiuyukeji.scheduler.Scheduler;
import com.xiuyukeji.scheduler.SchedulerUtil;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import static com.xiuyukeji.pictureplayerview.annotations.PictureSource.FILE;

/**
 * 播放实现
 *
 * @author Created by jz on 2017/3/26 16:55
 */
class PicturePlayer {
    public static final int DEFAULT_MAX_CACHE_NUMBER = 12;

    private Context mContext;

    private int mSource;//设置来源
    private final int mCacheFrameNumber;//最大缓存帧数
    private final int mReusableFrameNumber;//最大复用缓存帧数

    private volatile int mReadFrame;
    private volatile int mSeekToIndex = -1;

    private CacheList<Bitmap> mCacheBitmaps;
    private CacheList<Bitmap> mReusableBitmaps;

    private final Object mSeekToLock = new Object();

    private volatile boolean mIsReadCancel;
    private volatile boolean mIsPlayCancel;
    private volatile boolean mIsCancel;

    private String[] mPaths;
    private long mDuration;
    private int mFrameCount;

    private ReadThread mReadThread;
    private Scheduler mScheduler;

    private Renderer mRenderer;

    PicturePlayer(@NonNull Context context,
                  @PictureSource int source,
                  @IntRange(from = 2) int cacheFrameNumber,
                  @NonNull Renderer renderer) {
        this.mContext = context;
        this.mSource = source;
        this.mCacheFrameNumber = cacheFrameNumber;
        this.mReusableFrameNumber = mCacheFrameNumber;
        this.mRenderer = renderer;

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
        reset();
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
        if (mScheduler.isStarted() && !mScheduler.isCanceled()) {
            mScheduler.stop();
        }
        SchedulerUtil.join(mReadThread);
    }

    void seekTo(int frameIndex) {
        if (!mScheduler.isStarted()//没有真正开始播放
                || mIsPlayCancel) {//或者已经播放结束都无法seekTo
            return;
        }
        if (!mScheduler.isSeekToComplete()) {//如果还没有seekTo完成则记录最后一个
            mSeekToIndex = frameIndex;
            return;
        }

        synchronized (mSeekToLock) {
            mReadFrame = frameIndex;

            int pool = frameIndex - mReadFrame + mCacheBitmaps.size();
            if (pool > 0 && pool < mCacheFrameNumber) {//说明有复用帧
                for (int i = 0; i < pool; i++) {
                    mCacheBitmaps.removeFirst();
                }
            } else {
                mCacheBitmaps.clear();
            }

            mScheduler.seekTo(frameIndex, mSeekListener);
        }
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

    int getFrameIndex() {
        if (mScheduler == null) {
            return 0;
        }
        return (int) mScheduler.getFrameIndex();
    }

    private void reset() {
        mCacheBitmaps.clear();//这里会把删除的数据提交到mReusableBitmaps
        int count = mReusableBitmaps.size();
        for (int i = 0; i < count; i++) {
            ImageUtil.recycleBitmap(mReusableBitmaps.removeFirst());
        }

        mReadFrame = 0;

        mIsReadCancel = false;
        mIsPlayCancel = false;
        mIsCancel = false;
    }

    private void error(Exception error) {
        error.printStackTrace();
        PicturePlayer.this.stop();
        mRenderer.onError("读取图片失败");
    }

    private final OnSeekToListener mSeekListener = new OnSeekToListener() {
        @Override
        public void onSeekTo(long frameIndex) {
            synchronized (mSeekToLock) {
                if (mCacheBitmaps.isEmpty()) {
                    SchedulerUtil.lockWait(mSeekToLock);
                }
            }
        }

        @Override
        public void onSeekUpdate(long frameIndex) {
            update((int) frameIndex, -1);
        }

        @Override
        public boolean onSeekToComplete() {
            if (mSeekToIndex != -1) {
                seekTo(mSeekToIndex);
                mSeekToIndex = -1;
                return false;
            }
            return true;
        }
    };

    private class ReadThread extends Thread {
        @Override
        public void run() {
            try {
                while (!mIsCancel && !mIsPlayCancel) {
                    if (mReadFrame >= mFrameCount) {
                        SystemClock.sleep(1);
                        continue;
                    }
                    int size = mCacheBitmaps.size();
                    if (size >= mCacheFrameNumber || (size >= 1 && isPaused())) {//暂停的情况下只读取一帧
                        SystemClock.sleep(1);
                        continue;
                    }

                    synchronized (mSeekToLock) {
                        Bitmap bitmap = readBitmap(mPaths[mReadFrame]);

                        if (bitmap == null || bitmap.isRecycled()) {
                            throw new NullPointerException("读取的图片有错误");
                        }

                        mCacheBitmaps.add(bitmap);
                        mReadFrame++;

                        mSeekToLock.notifyAll();
                    }

                    if (mReadFrame == 1//第一帧
                            && !mIsCancel//未取消
                            && !mScheduler.isStarted()) {//未开始
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
            is = mContext.getResources().getAssets().open(path);
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
        @Override
        public void onFrameUpdate(long frameIndex) {
            int index = (int) frameIndex;

            update(index, index);
        }
    }

    private void update(int readFrameIndex, int frameIndex) {
        Bitmap bitmap = getBitmap(readFrameIndex);

        mRenderer.onDraw(frameIndex, bitmap);

        if (bitmap != null) {
            mCacheBitmaps.removeFirst();//在这一帧画完后再放进复用池，防止画面撕裂
        }
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

    private class FrameListener extends OnSimpleFrameListener {
        @Override
        public void onStop() {
            mIsPlayCancel = true;
            threadStop();
        }
    }

    private void threadStop() {
        if (!mIsReadCancel
                || !mIsPlayCancel && mScheduler.isStarted()) {
            return;
        }

        reset();

        mRenderer.onStop();
    }

    interface Renderer {
        void onDraw(int frameIndex, Bitmap bitmap);

        void onStop();

        void onError(String message);
    }
}