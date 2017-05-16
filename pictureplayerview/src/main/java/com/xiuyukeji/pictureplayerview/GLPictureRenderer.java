package com.xiuyukeji.pictureplayerview;

import android.graphics.Bitmap;
import android.opengl.GLES20;
import android.opengl.GLES30;

import com.xiuyukeji.pictureplayerview.interfaces.OnErrorListener;
import com.xiuyukeji.pictureplayerview.interfaces.OnStopListener;
import com.xiuyukeji.pictureplayerview.interfaces.OnUpdateListener;

import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import minus.android.support.opengl.GLTextureView;

/**
 * OpenGL实现图片播放器，渲染器
 *
 * @author Created by jz on 2017/4/10 15:25
 */

class GLPictureRenderer implements GLTextureView.Renderer, PicturePlayer.Renderer {

    static final int FIT_WIDTH = 0, FIT_HEIGHT = 1, FIT_CENTER = 2, FIT_CROP = 3;

    private int mProgram;

    private int mPositionHandle;
    private int mCoordinateHandle;
    private int mTextureHandle;

    private int[] mTextureIds;

    private FloatBuffer mVertexBuffer;
    private final FloatBuffer mCoordinateBuffer;

    private int mScaleType;//设置缩放类型

    private float mScale;
    private int mWidth;
    private int mHeight;

    private final Object mLock;

    private volatile boolean mPlaying;
    private volatile Bitmap mBitmap;

    private GLPicturePlayerView mGLTextureView;

    private OnUpdateListener mOnUpdateListener;
    private OnStopListener mOnStopListener;
    private OnErrorListener mOnErrorListener;

    GLPictureRenderer(int scaleType, GLPicturePlayerView glTextureView) {
        this.mScaleType = scaleType;
        this.mGLTextureView = glTextureView;

        mCoordinateBuffer = OpenGLUtils.getFloatBuffer(new float[]{
                0.0f, 0.0f,
                0.0f, 1.0f,
                1.0f, 0.0f,
                1.0f, 1.0f,
        });

        mLock = new Object();
    }

    void setScaleType(int scaleType) {
        this.mScaleType = scaleType;
    }

    void drawClear() {
        mGLTextureView.requestRender();
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        GLES20.glClearColor(0f, 0f, 0f, 0f);

        onInit();
    }

    private void onInit() {
        mProgram = OpenGLUtils.loadProgram(mGLTextureView.getContext(), R.raw.vertex, R.raw.fragment);

        mPositionHandle = GLES20.glGetAttribLocation(mProgram, "vPosition");
        mCoordinateHandle = GLES20.glGetAttribLocation(mProgram, "vCoordinate");
        mTextureHandle = GLES20.glGetUniformLocation(mProgram, "vTexture");
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        GLES20.glViewport(0, 0, width, height);
    }

    @Override
    public boolean onDrawFrame(GL10 gl) {
        if (mPlaying && mBitmap == null) {
            return false;
        }
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
        if (mBitmap == null) {
            return true;
        }
        synchronized (mLock) {
            glDraw();
            mLock.notify();//通知完成
        }
        return true;
    }

    @Override
    public void onSurfaceDestroyed() {
        if (mTextureIds != null) {
            GLES20.glDeleteTextures(1, mTextureIds, 0);
            mTextureIds = null;
        }
    }

    private void glDraw() {
        GLES20.glUseProgram(mProgram);

        bindTexture();

        GLES20.glEnableVertexAttribArray(mCoordinateHandle);
        GLES20.glVertexAttribPointer(mCoordinateHandle, 2, GLES20.GL_FLOAT, false, 0, mCoordinateBuffer);

        GLES20.glEnableVertexAttribArray(mPositionHandle);
        GLES20.glVertexAttribPointer(mPositionHandle, 2, GLES20.GL_FLOAT, false, 0, mVertexBuffer);

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);//四边形

        GLES20.glDisableVertexAttribArray(mPositionHandle);
        GLES20.glDisableVertexAttribArray(mCoordinateHandle);

        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, 0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
    }

    private void bindTexture() {
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);

        mTextureIds = OpenGLUtils.loadTexture(mBitmap, mTextureIds);

        GLES20.glUniform1i(mTextureHandle, 0);
    }

    private void calculateScale(int width, int height) {
        if (mPlaying) {
            return;
        }
        switch (mScaleType) {
            case FIT_WIDTH:
                callWidth(width, height);
                break;
            case FIT_HEIGHT:
                callHeight(width, height);
                break;
            case FIT_CENTER:
                if (getWidth() * height > getHeight() * width) {
                    callHeight(width, height);
                } else {
                    callWidth(width, height);
                }
                break;
            case FIT_CROP:
                if (getWidth() * height > getHeight() * width) {
                    callWidth(width, height);
                } else {
                    callHeight(width, height);
                }
                break;
            default:
                break;
        }

        float pointWidth = getWidth() / 2;
        float pointHeight = getHeight() / 2;

        float left = (pointWidth - calculateLeft()) / -pointWidth;
        float top = (pointHeight - calculateTop()) / pointHeight;
        float right = -left;
        float bottom = -top;

        mVertexBuffer = OpenGLUtils.getFloatBuffer(new float[]{
                left, top,
                left, bottom,
                right, top,
                right, bottom
        });

        mPlaying = true;
    }

    private void clearScale() {
        mScale = 0;
        mVertexBuffer.clear();
        mPlaying = false;
    }

    private void callWidth(int width, int height) {
        mScale = getWidth() / (float) width;
        mWidth = getWidth();
        mHeight = (int) (height * mScale);
    }

    private void callHeight(int width, int height) {
        mScale = getHeight() / (float) height;
        mWidth = (int) (width * mScale);
        mHeight = getHeight();
    }

    private int calculateLeft() {
        return (getWidth() - mWidth) / 2;
    }

    private int calculateTop() {
        return (getHeight() - mHeight) / 2;
    }

    private int getWidth() {
        return mGLTextureView.getWidth();
    }

    private int getHeight() {
        return mGLTextureView.getHeight();
    }

    @Override
    public void onDraw(int frameIndex, Bitmap bitmap) {
        if (mOnUpdateListener != null) {
            mOnUpdateListener.onUpdate(frameIndex);
        }
        if (bitmap == null) {
            return;
        }

        calculateScale(bitmap.getWidth(), bitmap.getHeight());

        synchronized (mLock) {
            try {
                mBitmap = bitmap;
                mGLTextureView.requestRender();
                mLock.wait();
                mBitmap = null;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onStop() {
        clearScale();
        if (mOnStopListener != null) {
            mOnStopListener.onStop();
        }
    }

    @Override
    public void onError(String message) {
        if (mOnErrorListener != null) {
            mOnErrorListener.onError(message);
        }
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
