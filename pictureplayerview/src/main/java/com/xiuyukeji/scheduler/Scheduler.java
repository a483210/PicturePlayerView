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

    private static final int MSG_FRAME = 0, MSG_MANUAL_QUIT = 1;

    private FrameThread mFrameThread;
    private FrameHandler mHandler;

    private long mDuration;
    private long mFrameCount;

    private double mDelayTime;
    private double mCurrentUptimeMs;

    private long mFrameIndex;

    private boolean mIsSkipFrame = false;

    private boolean mIsStared = false;
    private boolean mIsRunning = false;
    private boolean mIsPaused = false;
    private boolean mIsCancel = false;

    private OnFrameUpdateListener mOnFrameUpdateListener;
    private OnFrameListener mOnFrameListener;

    /**
     * 调度器只能运行一次
     *
     * @param duration   总时间
     * @param frameCount 总帧数
     * @param l          更新回调
     */
    public Scheduler(@IntRange(from = 1) long duration, @IntRange(from = 2) long frameCount, @NonNull OnFrameUpdateListener l) {
        this(duration, frameCount, l, null);
    }

    /**
     * 构造函数
     *
     * @param duration   总时间
     * @param frameCount 总帧数
     * @param l          更新回调
     * @param fl         其他回调
     */
    public Scheduler(@IntRange(from = 1) long duration, @IntRange(from = 2) long frameCount, @NonNull OnFrameUpdateListener l, OnFrameListener fl) {
        if (frameCount > duration) {
            throw new RuntimeException("duration must be greater than frameCount");
        }

        this.mDuration = duration;
        this.mFrameCount = frameCount;
        this.mOnFrameUpdateListener = l;
        this.mOnFrameListener = fl;

        mDelayTime = mDuration / (double) (mFrameCount - 1);
    }

    /**
     * 开始调度器，只能运行一次
     */
    public void start() {
        if (isStarted()) {
            throw new RuntimeException("scheduler can only run once");
        }
        mIsStared = true;

        mFrameThread = new FrameThread("scheduler");
        mFrameThread.start();
    }

    /**
     * 恢复调度器，{@link #isRunning()}返回true调用才有效
     *
     * @return 是否调用成功
     */
    public boolean resume() {
        if (!isRunning() || !isPaused() || mIsCancel) {
            return false;
        }

        mIsPaused = false;
        mCurrentUptimeMs = SystemClock.uptimeMillis();
        next(mCurrentUptimeMs);

        return true;
    }

    /**
     * 暂停调度器，{@link #isRunning()}返回true调用才有效
     *
     * @return 是否调用成功
     */
    public boolean pause() {
        if (!isRunning() || isPaused() || mIsCancel) {
            return false;
        }

        mIsPaused = true;
        mHandler.removeMessages(MSG_FRAME);

        return true;
    }

    /**
     * 结束调度器，调用{@link #start()}后才能调用，且只能调用一次
     */
    public void stop() {
        if (!isStarted()) {
            throw new RuntimeException("scheduler not yet running");
        }
        if (mIsCancel) {
            throw new RuntimeException("scheduler has stopped");
        }
        if (!isRunning()) {//代表调度器已经结束
            return;
        }

        mIsCancel = true;
        mHandler.removeMessages(MSG_FRAME);
        mHandler.sendMessageAtTime(mHandler.obtainMessage(MSG_MANUAL_QUIT), SystemClock.uptimeMillis());
        join();//等待update执行完成
    }

    /**
     * 调用{@link #start()}后返回True
     *
     * @return 是否开始运行
     */
    public boolean isStarted() {
        return mIsStared;
    }

    /**
     * 真实开始后返回True，在{@link #mFrameThread}的run执行完成后，结束后返回False
     *
     * @return 是否在运行中
     */
    public boolean isRunning() {
        return mIsRunning;
    }

    /**
     * 调用{@link #pause()}后返回True
     *
     * @return 是否暂停
     */
    public boolean isPaused() {
        return mIsPaused;
    }

    /**
     * 是否跳帧，必须在没有开始运行之前调用，设置为True后当{@link #update(long)}被阻塞的时间超过{@link #mDelayTime}后将开始跳帧
     *
     * @param isSkipFrame 是否跳帧
     */
    public void setSkipFrame(boolean isSkipFrame) {
        if (!isStarted()) {
            throw new RuntimeException("scheduler has been running");
        }

        this.mIsSkipFrame = isSkipFrame;
    }

    private void join() {
        if (Thread.currentThread().getId() == mFrameThread.getId()) {//如果是同一个线程调用则不等待，防止死循环
            return;
        }

        try {
            mFrameThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
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
        mIsRunning = false;
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

            mIsRunning = true;

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
                        if (!mIsPaused && !mIsCancel) {
                            mCurrentUptimeMs += mDelayTime;
                            next(mCurrentUptimeMs);
                        }
                    }
                    break;
                case MSG_MANUAL_QUIT:
                    cancel();
                    quit();
                    break;
            }
        }
    }
}