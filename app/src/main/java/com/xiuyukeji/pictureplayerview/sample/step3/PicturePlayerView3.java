package com.xiuyukeji.pictureplayerview.sample.step3;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.os.SystemClock;
import android.util.AttributeSet;

import com.xiuyukeji.pictureplayerview.sample.BasePicturePlayerView;
import com.xiuyukeji.scheduler.OnFrameUpdateListener;
import com.xiuyukeji.scheduler.Scheduler;

import java.io.IOException;

/**
 * 步骤3实现
 *
 * @author Created by jz on 2017/3/29 17:49
 */
public class PicturePlayerView3 extends BasePicturePlayerView {

    private static final int MAX_CACHE_NUMBER = 12;//这是代表读取最大缓存帧数，因为一张图片的大小有width*height*4这么大，内存吃不消

    private Paint mPaint;//画笔

    private Bitmap[] mCacheBitmaps;//缓存帧集合
    private int mCacheCount;//当前缓存的帧数

    private int mReadIndex;//读取缓存的游标，已经读取的位置
    private int mPlayIndex;//写入缓存的游标，已经写入的位置

    private int mReadFrame;//当前读取到那一帧，总帧数相关

    private String[] mPaths;//图片绝对地址集合
    private int mFrameCount;//总帧数

    private ReadThread mReadThread;
    private Scheduler mScheduler;

    //... 省略构造方法
    public PicturePlayerView3(Context context) {
        this(context, null);
    }

    public PicturePlayerView3(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PicturePlayerView3(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private void init() {
        setOpaque(false);//设置背景透明，记住这里是[是否不透明]

        setSurfaceTextureListener(this);//设置监听

        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG | Paint.FILTER_BITMAP_FLAG);//创建画笔

        mCacheBitmaps = new Bitmap[MAX_CACHE_NUMBER];
    }

    //... 省略SurfaceTextureListener的方法
    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        mReadFrame = mFrameCount;
        if (mScheduler != null && mScheduler.isStarted()) {
            mScheduler.stop();
        }
        return false;
    }

    //开始播放
    @Override
    public void start(String[] paths, long duration) {
        this.mPaths = paths;
        this.mFrameCount = paths.length;

        //开启线程
        mReadThread = new ReadThread();
        mReadThread.start();
        mScheduler = new Scheduler(duration, mFrameCount,
                new FrameUpdateListener());
    }

    private class ReadThread extends Thread {
        @Override
        public void run() {
            try {
                while (mReadFrame < mFrameCount) {//并且没有读完则继续读取
                    if (mCacheCount >= MAX_CACHE_NUMBER) {//
                        SystemClock.sleep(1);
                        continue;
                    }

                    Bitmap bmp = readBitmap(mPaths[mReadFrame]);
                    mCacheBitmaps[mReadIndex++] = bmp;

                    if (mReadIndex >= MAX_CACHE_NUMBER) {
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
            }
        }
    }

    private Bitmap readBitmap(String path) throws IOException {
        return BitmapFactory.decodeStream(getResources().getAssets().open(path));
    }

    private class FrameUpdateListener implements OnFrameUpdateListener {
        @Override
        public void onFrameUpdate(long frameIndex) {
            if (mCacheCount <= 0
                    || frameIndex >= mReadFrame) {
                return;
            }

            Bitmap bitmap = mCacheBitmaps[mPlayIndex];
            drawBitmap(bitmap);
            recycleBitmap(bitmap);

            mCacheBitmaps[mPlayIndex] = null;

            mPlayIndex++;
            if (mPlayIndex >= MAX_CACHE_NUMBER) {
                mPlayIndex = 0;
            }

            mCacheCount--;
        }
    }

    private void drawBitmap(Bitmap bitmap) {
        Canvas canvas = lockCanvas(new Rect(0, 0, getWidth(), getHeight()));//锁定画布
        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);// 清空画布
        Rect src = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
        Rect dst = new Rect(0, 0, getWidth(), bitmap.getHeight() * getWidth() / bitmap.getWidth());
        canvas.drawBitmap(bitmap, src, dst, mPaint);//将bitmap画到画布上
        unlockCanvasAndPost(canvas);//解锁画布同时提交
    }

    private static void recycleBitmap(Bitmap bitmap) {
        if (bitmap != null && !bitmap.isRecycled()) {
            bitmap.recycle();
        }
    }
}