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

    private static final int MSG_QUIT = -1, MSG_FRAME = 0, MSG_SEEK = 1;

    private final Object mLock = new Object();

    private FrameThread mFrameThread;
    private FrameHandler mHandler;

    private long mDuration;
    private long mFrameCount;

    private double mDelayTime;
    private double mCurrentUptimeMs;

    private volatile long mFrameIndex;

    private boolean mIsSkipFrame = false;

    private volatile boolean mIsStared = false;
    private volatile boolean mIsRunning = false;
    private volatile boolean mIsPaused = false;
    private volatile boolean mIsCancel = false;
    private volatile boolean mIsRunPause = false;
    private volatile boolean mIsWaitResume = false;
    private volatile boolean mIsSeekToComplete = true;

    private OnFrameUpdateListener mOnFrameUpdateListener;
    private OnFrameListener mOnFrameListener;
    private OnSeekToListener mOnSeekToListener;

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
        if (duration < 1) {
            throw new RuntimeException("duration must be greater than 0");
        }
        if (frameCount < 2) {
            throw new RuntimeException("frameCount must be greater than 2");
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
        if (!isSeekToComplete()) {
            mIsWaitResume = true;
            return true;
        }

        synchronized (mLock) {
            if (mIsRunPause) {//如果暂停被阻塞，则等待
                SchedulerUtil.lockWait(mLock);
            }
            if (!mIsRunning) {//如果在等待过程中发现线程被停止则跳出
                return false;
            }
            mIsPaused = false;
            mCurrentUptimeMs = SystemClock.uptimeMillis();
            next(mCurrentUptimeMs);
        }

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

        mIsRunPause = true;
        mIsPaused = true;
        mHandler.removeMessages(MSG_FRAME);
        synchronized (mLock) {
            mIsRunPause = false;
            mLock.notifyAll();
        }

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
        mHandler.removeMessages(MSG_SEEK);
        nextQuit();
        SchedulerUtil.join(mFrameThread);//等待线程执行结束
    }

    /**
     * 跳转到某一帧，{@link #isRunning()}返回true调用才有效
     *
     * @param frameIndex 跳转帧序列
     */
    public void seekTo(@IntRange(from = 0) long frameIndex, @NonNull OnSeekToListener l) {
        if (!isRunning() || mIsCancel) {
            return;
        }
        if (frameIndex == mFrameIndex || !isSeekToComplete()) {
            return;
        }

        mIsSeekToComplete = false;
        mOnSeekToListener = l;

        if (!isPaused()) {
            pause();
            mIsWaitResume = true;
        }

        if (frameIndex >= mFrameCount) {
            mFrameIndex = mFrameCount - 1;
        } else if (frameIndex < 0) {
            mFrameIndex = 0;
        } else {
            mFrameIndex = frameIndex;
        }

        mHandler.sendMessageAtTime(mHandler.obtainMessage(MSG_SEEK), SystemClock.uptimeMillis());
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
     * 调用{@link #stop()}后返回True
     *
     * @return 是否被取消
     */
    public boolean isCanceled() {
        return mIsCancel;
    }

    /**
     * 调用{@link #setSkipFrame(boolean)}后完成返回True
     *
     * @return seekTo是否完成
     */
    public boolean isSeekToComplete() {
        return mIsSeekToComplete;
    }

    /**
     * 返回当前帧
     *
     * @return 当前帧序列
     */
    public long getFrameIndex() {
        return mFrameIndex;
    }

    /**
     * 是否跳帧，必须在没有开始运行之前调用
     * 设置为True后当{@link #update(long)}被阻塞的时间超过{@link #mDelayTime}后将开始跳帧
     *
     * @param isSkipFrame 是否跳帧
     */
    public void setSkipFrame(boolean isSkipFrame) {
        if (isStarted()) {
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

    private void nextQuit() {
        mHandler.sendMessageAtTime(mHandler.obtainMessage(MSG_QUIT), SystemClock.uptimeMillis());
    }

    private void cancel() {
        if (mIsCancel && mOnFrameListener != null) {
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

    private final class FrameThread extends HandlerThread {
        FrameThread(String name) {
            super(name);
        }

        @Override
        public void onLooperPrepared() {
            mHandler = new FrameHandler();

            mIsRunning = true;

            mCurrentUptimeMs = SystemClock.uptimeMillis();

            prepare();
            next(mCurrentUptimeMs);
        }
    }

    @SuppressLint("HandlerLeak")
    private final class FrameHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_FRAME:
                    synchronized (mLock) {
                        if (!mIsCancel && mIsPaused) {//如果被暂停了，跳出
                            break;
                        }
                        update(mFrameIndex);
                        if (mIsSkipFrame) {
                            double delayTime = SystemClock.uptimeMillis() - mCurrentUptimeMs - mDelayTime;
                            if (delayTime > 0) {
                                long delayIndex = (long) Math.ceil(delayTime / mDelayTime);
                                mFrameIndex += delayIndex;
                                mCurrentUptimeMs += delayIndex * mDelayTime;
                            }
                        }
                        mCurrentUptimeMs += mDelayTime;
                        mFrameIndex++;
                        if (!mIsCancel) {
                            if (mFrameIndex >= mFrameCount) {
                                nextQuit();
                            } else if (!mIsPaused) {
                                next(mCurrentUptimeMs);
                            }
                        }
                    }
                    break;
                case MSG_SEEK:
                    mOnSeekToListener.onSeekTo(mFrameIndex);

                    if (!mIsWaitResume) {
                        mOnSeekToListener.onSeekUpdate(mFrameIndex);
                    }

                    mIsSeekToComplete = true;

                    boolean isResume = mOnSeekToListener.onSeekToComplete();

                    if (isResume) {
                        mOnSeekToListener = null;
                        if (mIsWaitResume) {
                            resume();
                            mIsWaitResume = false;
                        }
                    }
                    break;
                case MSG_QUIT:
                    cancel();
                    quit();
                    break;
                default:
                    break;
            }
        }
    }
}