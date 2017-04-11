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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 步骤3实现
 *
 * @author Created by jz on 2017/3/29 17:49
 */
public class PicturePlayerView3 extends BasePicturePlayerView {

    private static final int MAX_CACHE_NUMBER = 12;//这是代表读取最大缓存帧数，因为一张图片的大小有width*height*4这么大，内存吃不消

    private Paint mPaint;//画笔
    private Rect mSrcRect;
    private Rect mDstRect;

    private List<Bitmap> mCacheBitmaps;//缓存帧集合

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

        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG);//创建画笔
        mSrcRect = new Rect();
        mDstRect = new Rect();

        mCacheBitmaps = Collections.synchronizedList(new ArrayList<Bitmap>());
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
                    if (mCacheBitmaps.size() >= MAX_CACHE_NUMBER) {//如果读取的超过最大缓存则暂停读取
                        SystemClock.sleep(1);
                        continue;
                    }

                    Bitmap bmp = readBitmap(mPaths[mReadFrame]);
                    mCacheBitmaps.add(bmp);

                    mReadFrame++;

                    if (mReadFrame == 1) {//读取到第一帧后在开始调度器
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
            if (mCacheBitmaps.isEmpty()) {//如果当前没有帧，则直接跳过
                return;
            }

            Bitmap bitmap = mCacheBitmaps.remove(0);//获取第一帧同时从缓存里删除
            drawBitmap(bitmap);
            recycleBitmap(bitmap);
        }
    }

    private void drawBitmap(Bitmap bitmap) {
        Canvas canvas = lockCanvas();//锁定画布
        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);// 清空画布
        mSrcRect.set(0, 0, bitmap.getWidth(), bitmap.getHeight());//这里我将2个rect抽离出去，防止重复创建
        mDstRect.set(0, 0, getWidth(), bitmap.getHeight() * getWidth() / bitmap.getWidth());
        canvas.drawBitmap(bitmap, mSrcRect, mDstRect, mPaint);//将bitmap画到画布上
        unlockCanvasAndPost(canvas);//解锁画布同时提交
    }

    private static void recycleBitmap(Bitmap bitmap) {
        if (bitmap != null && !bitmap.isRecycled()) {
            bitmap.recycle();
        }
    }
}