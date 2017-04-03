package com.xiuyukeji.pictureplayerview.sample.step1;

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

import java.io.IOException;

/**
 * 步骤1实现
 *
 * @author Created by jz on 2017/3/29 17:49
 */
public class PicturePlayerView1 extends BasePicturePlayerView {

    private Paint mPaint;//画笔
    private Rect mSrcRect;
    private Rect mDstRect;

    private int mPlayFrame;//当前播放到那一帧，总帧数相关

    private String[] mPaths;//图片绝对地址集合
    private int mFrameCount;//总帧数
    private long mDelayTime;//播放帧间隔

    private PlayThread mPlayThread;

    //... 省略构造方法
    public PicturePlayerView1(Context context) {
        this(context, null);
    }

    public PicturePlayerView1(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PicturePlayerView1(Context context, AttributeSet attrs, int defStyle) {
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
        mPlayFrame = mFrameCount;

        try {
            if (mPlayThread != null) {
                mPlayThread.join();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return false;
    }

    //开始播放
    @Override
    public void start(String[] paths, long duration) {
        this.mPaths = paths;
        this.mFrameCount = paths.length;
        this.mDelayTime = duration / mFrameCount;

        //开启线程
        mPlayThread = new PlayThread();
        mPlayThread.start();
    }

    private class PlayThread extends Thread {
        @Override
        public void run() {
            try {
                while (mPlayFrame < mFrameCount) {//如果还没有播放完所有帧
                    Bitmap bitmap = readBitmap(mPaths[mPlayFrame]);
                    drawBitmap(bitmap);
                    recycleBitmap(bitmap);
                    mPlayFrame++;
                    SystemClock.sleep(mDelayTime);//暂停间隔时间
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private Bitmap readBitmap(String path) throws IOException {
        return BitmapFactory.decodeStream(getResources().getAssets().open(path));
    }

    private void drawBitmap(Bitmap bitmap) {
        Canvas canvas = lockCanvas();//锁定画布
        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);// 清空画布
        mSrcRect.set(0, 0, bitmap.getWidth(), bitmap.getHeight());
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