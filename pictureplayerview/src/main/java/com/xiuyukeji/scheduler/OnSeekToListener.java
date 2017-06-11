package com.xiuyukeji.scheduler;

/**
 * 回调
 *
 * @author Created by jz on 2017/6/9 11:21
 */
public interface OnSeekToListener {
    void onSeekTo(long frameIndex);

    void onSeekUpdate(long frameIndex);

    boolean onSeekToComplete();
}