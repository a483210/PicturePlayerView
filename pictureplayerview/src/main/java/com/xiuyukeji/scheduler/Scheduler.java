package com.xiuyukeji.scheduler;

import android.annotation.SuppressLint;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.os.SystemClock;
import android.support.annotation.IntRange;
import android.support.annotation.NonNull;

/**
 * 调度器
 *
 * @author Created by jz on 2017/3/28 21:26
 */
public final class Scheduler {

    private static final int MSG_FRAME = 0;

    private FrameThread mFrameThread;
    private FrameHandler mHandler;

    private long mDuration;
    private long mFrameCount;

    private double mDelayTime;
    private double mCurrentUptimeMs;

    private long mFrameIndex;

    private boolean mIsSkipFrame = false;
    private boolean mIsCancel = false;

    private OnFrameUpdateListener mOnFrameUpdateListener;
    private OnFrameListener mOnFrameListener;

    public Scheduler(@IntRange(from = 1) long duration, @IntRange(from = 2) long frameCount, @NonNull OnFrameUpdateListener l) {
        this(duration, frameCount, l, null);
    }

    public Scheduler(@IntRange(from = 1) long duration, @IntRange(from = 2) long frameCount, @NonNull OnFrameUpdateListener l, OnFrameListener fl) {
        if (frameCount > duration)
            throw new RuntimeException("duration must be greater than frameCount");

        this.mDuration = duration;
        this.mFrameCount = frameCount;
        this.mOnFrameUpdateListener = l;
        this.mOnFrameListener = fl;

        mDelayTime = mDuration / (double) (mFrameCount - 1);
    }

    public synchronized void start() {
        if (mFrameThread != null) {
            throw new RuntimeException("scheduler can only run once");
        }
        mFrameThread = new FrameThread("scheduler");
        mFrameThread.start();
    }

    public synchronized void stop() {
        if (mFrameThread == null) {
            throw new RuntimeException("scheduler not yet running");
        }
        if (mIsCancel) {
            throw new RuntimeException("scheduler has stopped");
        }

        mIsCancel = true;
    }

    public boolean isRunning() {
        return mFrameThread != null;
    }

    public void setSkipFrame(boolean isSkipFrame) {
        if (mFrameThread != null) {
            throw new RuntimeException("scheduler has been running");
        }

        this.mIsSkipFrame = isSkipFrame;
    }

    private void next(double uptimeMs) {
        mHandler.sendMessageAtTime(mHandler.obtainMessage(MSG_FRAME), Math.round(uptimeMs));
    }

    private void prepare() {
        if (mOnFrameListener != null) {
            mOnFrameListener.onStart();
        }
    }

    private void cancel() {
        if (mOnFrameListener != null) {
            mOnFrameListener.onCancel();
        }
    }

    private void quit() {
        mFrameThread.quit();
        mFrameThread.interrupt();
        if (mOnFrameListener != null) {
            mOnFrameListener.onStop();
        }
    }

    private void update(long frameIndex) {
        mOnFrameUpdateListener.onFrameUpdate(frameIndex);
    }

    public void setOnFrameListener(OnFrameListener l) {
        this.mOnFrameListener = l;
    }

    private final class FrameThread extends HandlerThread {
        FrameThread(String name) {
            super(name);
        }

        @Override
        public void onLooperPrepared() {
            mHandler = new FrameHandler();

            mCurrentUptimeMs = SystemClock.uptimeMillis() + mDelayTime;

            prepare();
            update(0);
            next(mCurrentUptimeMs);
        }
    }

    @SuppressLint("HandlerLeak")
    private final class FrameHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_FRAME:
                    if (mIsCancel) {
                        cancel();
                        quit();
                        break;
                    }
                    if (mIsSkipFrame) {
                        double delayTime = SystemClock.uptimeMillis() - mCurrentUptimeMs - mDelayTime;
                        if (delayTime > 0) {
                            long delayIndex = (long) Math.ceil(delayTime / mDelayTime);
                            mFrameIndex += delayIndex;
                            mCurrentUptimeMs += delayIndex * mDelayTime;
                        }
                    }
                    mFrameIndex++;
                    if (mFrameIndex >= mFrameCount - 1) {
                        update(mFrameCount - 1);
                        quit();
                    } else {
                        update(mFrameIndex);
                        mCurrentUptimeMs += mDelayTime;
                        next(mCurrentUptimeMs);
                    }
                    break;
            }
        }
    }
}