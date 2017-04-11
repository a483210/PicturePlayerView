package com.xiuyukeji.pictureplayerview;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.SystemClock;
import android.support.annotation.NonNull;

import com.xiuyukeji.pictureplayerview.utils.CacheList;
import com.xiuyukeji.pictureplayerview.utils.ImageUtil;
import com.xiuyukeji.scheduler.OnFrameUpdateListener;
import com.xiuyukeji.scheduler.OnSimpleFrameListener;
import com.xiuyukeji.scheduler.Scheduler;
import com.xiuyukeji.scheduler.SchedulerUtil;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * 播放实现
 *
 * @author Created by jz on 2017/3/26 16:55
 */
public class PicturePlayer {

    public static final int FILE = 0, ASSETS = 1;

    public static final int MAX_CACHE_NUMBER = 12;

    private Context mContext;

    private int mSource;//设置来源
    private final int mCacheFrameNumber;//最大缓存帧数
    private final int mReusableFrameNumber;//最大复用缓存帧数

    private int mReadFrame;

    private CacheList<Bitmap> mCacheBitmaps;
    private CacheList<Bitmap> mReusableBitmaps;

    private volatile boolean mIsReadCancel;
    private volatile boolean mIsPlayCancel;
    private volatile boolean mIsCancel;

    private String[] mPaths;
    private long mDuration;
    private int mFrameCount;

    private ReadThread mReadThread;
    private Scheduler mScheduler;

    private Renderer mRenderer;

    public PicturePlayer(@NonNull Context context,
                         int source, int cacheFrameNumber,
                         @NonNull Renderer renderer) {
        this.mContext = context;
        this.mSource = source;
        this.mCacheFrameNumber = cacheFrameNumber;
        this.mReusableFrameNumber = mCacheFrameNumber / 2;
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

    public void setDataSource(String[] paths, long duration, int frameCount) {
        this.mPaths = paths;
        this.mDuration = duration;
        this.mFrameCount = frameCount;
    }

    public void start() {
        mReadThread = new ReadThread();
        mScheduler = new Scheduler(mDuration, mFrameCount,
                new FrameUpdateListener(),
                new FrameListener());
        mScheduler.setSkipFrame(true);
        mReadThread.start();
    }

    public boolean pause() {
        return mScheduler.pause();
    }

    public boolean resume() {
        return mScheduler.resume();
    }

    public void stop() {
        mIsCancel = true;
        mReadThread.interrupt();
        if (mScheduler.isStarted()) {
            mScheduler.stop();
        }
        SchedulerUtil.join(mReadThread);
    }

    public boolean isStarted() {
        return mScheduler != null && mScheduler.isStarted();
    }

    public boolean isRunning() {
        return mScheduler != null && mScheduler.isRunning();
    }

    public boolean isPaused() {
        return mScheduler != null && mScheduler.isPaused();
    }

    private void error(Exception error) {
        error.printStackTrace();
        PicturePlayer.this.stop();
        mRenderer.onError("读取图片失败");
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
        public void onFrameUpdate(long index) {
            int frameIndex = (int) index;

            Bitmap bitmap = getBitmap(frameIndex);

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

        mRenderer.onStop();
    }

    public interface Renderer {
        void onDraw(int frameIndex, Bitmap bitmap);

        void onStop();

        void onError(String message);
    }
}