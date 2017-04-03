package com.xiuyukeji.pictureplayerview.utils;

import android.support.annotation.NonNull;

/**
 * 缓存数组，线程安全
 *
 * @author Created by jz on 2017/4/1 15:45
 */
public class CacheList<T> {

    private volatile T[] mValues;

    private int mMaxCount;
    private int mCount;
    private int mIndex;//index代表下一个位置

    private OnRemoveListener<T> mOnRemoveListener;

    /**
     * 构造函数
     *
     * @param values 存储数据的数组
     */
    public CacheList(@NonNull T[] values) {
        this(values, null);
    }

    /**
     * 构造函数
     *
     * @param values 存储数据的数组
     * @param l      当某个数据被删除时回调，回调线程为调用{@link #removeFirst()}、{@link #remove(int)}、{@link #removeCount(int)、{@link #clear()}函数的线程
     */
    public CacheList(@NonNull T[] values, OnRemoveListener<T> l) {
        this.mValues = values;
        this.mMaxCount = values.length;
        this.mOnRemoveListener = l;
    }

    /**
     * 取出数组里的第一个
     */
    public synchronized T getFirst() {
        return get(0);
    }

    /**
     * 获得index的数据
     *
     * @param index 索引
     */
    public synchronized T get(int index) {
        if (index < 0 || index >= mCount) {
            return null;
        }
        return mValues[getRealIndex(index)];
    }

    /**
     * 添加到最后
     *
     * @param value 值
     */
    public synchronized void add(T value) {
        if (mCount == mMaxCount) {
            if (mOnRemoveListener != null) {
                mOnRemoveListener.onRemove(true, mValues[mIndex]);
            }
            mCount--;
        }
        mValues[mIndex++] = value;
        if (mIndex >= mMaxCount) {
            mIndex = 0;
        }
        mCount++;
    }

    /**
     * 删除第一个
     */
    public synchronized T removeFirst() {
        return remove(0);
    }

    /**
     * 删除索引的数据
     *
     * @param index 索引
     */
    public synchronized T remove(int index) {
        if (index >= mCount) {
            return null;
        }
        int realIndex = getRealIndex(index);
        T value = mValues[realIndex];

        int i;
        if (index < mCount / 2) {
            for (i = index; i > 0; i--) {
                mValues[getRealIndex(i)] = mValues[getRealIndex(i - 1)];
            }
        } else {
            for (i = index; i < mCount; i++) {
                mValues[getRealIndex(i)] = mValues[getRealIndex(i + 1)];
            }
        }
        mValues[getRealIndex(i)] = null;
        mCount--;

        if (mOnRemoveListener != null) {
            mOnRemoveListener.onRemove(false, value);
        }

        return value;
    }

    /**
     * 从第一个开始删除到count个
     *
     * @param count 数量
     */
    public synchronized void removeCount(int count) {
        if (count > mCount) {
            count = mCount;
        }
        for (int i = 0; i < count; i++) {
            int index = getRealIndex(i);
            if (mOnRemoveListener != null) {
                mOnRemoveListener.onRemove(false, mValues[index]);
            }
            mValues[index] = null;
        }

        mCount -= count;
    }

    /**
     * 清除所有
     */
    public synchronized void clear() {
        removeCount(mCount);
    }

    /**
     * 获得大小
     */
    public synchronized int size() {
        return mCount;
    }

    /**
     * 是否为空
     */
    public synchronized boolean isEmpty() {
        return mCount == 0;
    }

    private int getRealIndex(int index) {
        int realIndex = mIndex - mCount + index;
        if (realIndex < 0) {
            return mMaxCount + realIndex;
        } else {
            return realIndex;
        }
    }

    public interface OnRemoveListener<T> {
        void onRemove(boolean isOverflow, T value);
    }
}
