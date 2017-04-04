package com.xiuyukeji.pictureplayerview.sample.step4;

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
import com.xiuyukeji.pictureplayerview.utils.ImageUtil;
import com.xiuyukeji.scheduler.OnFrameUpdateListener;
import com.xiuyukeji.scheduler.Scheduler;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 步骤4实现
 *
 * @author Created by jz on 2017/3/29 17:49
 */
public class PicturePlayerView4 extends BasePicturePlayerView {

    private static final int MAX_CACHE_NUMBER = 12;//这是代表读取最大缓存帧数，因为一张图片的大小有width*height*4这么大，内存吃不消
    private static final int MAX_REUSABLE_NUMBER = MAX_CACHE_NUMBER / 2;//这是代表读取最大复用帧数

    private Paint mPaint;//画笔
    private Rect mSrcRect;
    private Rect mDstRect;

    private List<Bitmap> mCacheBitmaps;//缓存帧集合
    private List<Bitmap> mReusableBitmaps;
    private int mCacheCount;//当前缓存的帧数

    private int mReadFrame;//当前读取到那一帧，总帧数相关

    private String[] mPaths;//图片绝对地址集合
    private int mFrameCount;//总帧数

    private ReadThread mReadThread;
    private Scheduler mScheduler;

    //... 省略构造方法
    public PicturePlayerView4(Context context) {
        this(context, null);
    }

    public PicturePlayerView4(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PicturePlayerView4(Context context, AttributeSet attrs, int defStyle) {
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
        mReusableBitmaps = Collections.synchronizedList(new ArrayList<Bitmap>());
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
                    if (mCacheCount >= MAX_REUSABLE_NUMBER) {//如果读取的超过最大缓存则暂停读取
                        SystemClock.sleep(1);
                        continue;
                    }

                    Bitmap bmp = readBitmap(mPaths[mReadFrame]);
                    mCacheBitmaps.add(bmp);

                    mReadFrame++;
                    mCacheCount++;

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
        InputStream is = getResources().getAssets().open(path);//这里需要以流的形式读取
        BitmapFactory.Options options = getReusableOptions(is);//获取参数设置
        Bitmap bmp = BitmapFactory.decodeStream(is, null, options);
        is.close();
        return bmp;
    }

    //实现复用
    private BitmapFactory.Options getReusableOptions(InputStream is) throws IOException {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        options.inSampleSize = 1;
        options.inJustDecodeBounds = true;//这里设置为不将图片读取到内存中
        is.mark(is.available());
        BitmapFactory.decodeStream(is, null, options);//获得大小
        options.inJustDecodeBounds = false;//设置回来
        is.reset();
        Bitmap inBitmap = getBitmapFromReusableSet(options);
        options.inMutable = true;
        if (inBitmap != null) {//如果有符合条件的设置属性
            options.inBitmap = inBitmap;
        }
        return options;
    }

    //从复用池中寻找合适的bitmap
    private Bitmap getBitmapFromReusableSet(BitmapFactory.Options options) {
        if (mReusableBitmaps.isEmpty()) {
            return null;
        }
        int count = mReusableBitmaps.size();
        for (int i = 0; i < count; i++) {
            Bitmap item = mReusableBitmaps.get(i);
            if (ImageUtil.canUseForInBitmap(item, options)) {//寻找符合条件的bitmap
                return mReusableBitmaps.remove(i);
            }
        }
        return null;
    }

    private void addReusable(Bitmap bitmap) {
        if (mReusableBitmaps.size() >= MAX_REUSABLE_NUMBER) {//如果超过则将其释放
            recycleBitmap(mReusableBitmaps.remove(0));
        }
        mReusableBitmaps.add(bitmap);
    }

    private class FrameUpdateListener implements OnFrameUpdateListener {
        @Override
        public void onFrameUpdate(long frameIndex) {
            if (mCacheCount <= 0) {//如果当前没有帧，则直接跳过
                return;
            }

            Bitmap bitmap = mCacheBitmaps.get(0);//获取第一帧
            drawBitmap(bitmap);

            addReusable(mCacheBitmaps.remove(0));//必须在画完之后在删除，不然会出现画面撕裂

            mCacheCount--;
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