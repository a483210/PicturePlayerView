package com.xiuyukeji.scheduler;

/**
 * 回调
 *
 * @author Created by jz on 2017/3/28 21:12
 */
public interface OnFrameListener {
    void onStart();

    void onStop();

    void onCancel();
}
