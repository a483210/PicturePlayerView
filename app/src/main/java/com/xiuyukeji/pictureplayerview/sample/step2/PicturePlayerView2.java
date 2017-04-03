package com.xiuyukeji.pictureplayerview.sample.step2;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.util.AttributeSet;

import com.xiuyukeji.pictureplayerview.sample.BasePicturePlayerView;
import com.xiuyukeji.scheduler.OnFrameUpdateListener;
import com.xiuyukeji.scheduler.Scheduler;

import java.io.IOException;

/**
 * 步骤2实现
 *
 * @author Created by jz on 2017/3/29 17:49
 */
public class PicturePlayerView2 extends BasePicturePlayerView {

    private Paint mPaint;//画笔
    private Rect mSrcRect;
    private Rect mDstRect;

    private String[] mPaths;//图片绝对地址集合

    private Scheduler mScheduler;

    //... 省略构造方法
    public PicturePlayerView2(Context context) {
        this(context, null);
    }

    public PicturePlayerView2(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PicturePlayerView2(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private void init() {
        setOpaque(false);//设置背景透明，记住这里是[是否不透明]

        setSurfaceTextureListener(this);//设置监听

        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG);//创建画笔
        mSrcRect = new Rect();
        mDstRect = new Rect();
    }

    //... 省略SurfaceTextureListener的方法

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        if (mScheduler != null && mScheduler.isStarted()) {
            mScheduler.stop();
        }
        return false;
    }

    //开始播放
    @Override
    public void start(String[] paths, long duration) {
        this.mPaths = paths;

        //开启线程
        mScheduler = new Scheduler(duration, paths.length,
                new FrameUpdateListener());
        mScheduler.start();
    }

    private Bitmap readBitmap(String path) throws IOException {
        return BitmapFactory.decodeStream(getResources().getAssets().open(path));
    }

    private class FrameUpdateListener implements OnFrameUpdateListener {
        @Override
        public void onFrameUpdate(long frameIndex) {
            try {
                Bitmap bitmap = readBitmap(mPaths[(int) frameIndex]);
                drawBitmap(bitmap);
                recycleBitmap(bitmap);
            } catch (Exception e) {
                e.printStackTrace();
            }
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