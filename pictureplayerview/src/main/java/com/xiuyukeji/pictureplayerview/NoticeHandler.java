package com.xiuyukeji.pictureplayerview;

import android.os.Handler;
import android.os.Message;

import com.xiuyukeji.pictureplayerview.interfaces.OnErrorListener;
import com.xiuyukeji.pictureplayerview.interfaces.OnStopListener;
import com.xiuyukeji.pictureplayerview.interfaces.OnUpdateListener;

/**
 * 通知
 *
 * @author Created by jz on 2017/3/26 16:52
 */
public class NoticeHandler extends Handler {

    private static final int UPDATE = 0, STOP = 1, ERROR = -1;

    private OnUpdateListener mOnUpdateListener;
    private OnStopListener mOnStopListener;
    private OnErrorListener mOnErrorListener;

    @Override
    public void handleMessage(Message msg) {
        super.handleMessage(msg);
        switch (msg.what) {
            case UPDATE:
                if (mOnUpdateListener != null) {
                    mOnUpdateListener.onUpdate(msg.arg1);
                }
                break;
            case STOP:
                if (mOnStopListener != null) {
                    mOnStopListener.onStop();
                }
                break;
            case ERROR:
                if (mOnErrorListener != null) {
                    mOnErrorListener.onError(String.valueOf(msg.obj));
                }
                break;
            default:
                break;
        }
    }

    public void noticeUpdate(int frame) {
        Message message = Message.obtain();
        message.what = UPDATE;
        message.arg1 = frame;
        sendMessage(message);
    }

    public void setOnUpdateListener(OnUpdateListener l) {
        this.mOnUpdateListener = l;
    }

    public void noticeStop() {
        sendEmptyMessage(STOP);
    }

    public void setOnStopListener(OnStopListener l) {
        this.mOnStopListener = l;
    }

    public void noticeError(String msg) {
        Message message = Message.obtain();
        message.what = ERROR;
        message.obj = msg;
        sendMessage(message);
    }

    public void setOnErrorListener(OnErrorListener l) {
        this.mOnErrorListener = l;
    }

}

