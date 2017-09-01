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
class NoticeHandler extends Handler {

    private static final int UPDATE = 0, STOP = 1, ERROR = -1;

    private OnUpdateListener mOnUpdateListener;
    private OnStopListener mOnStopListener;
    private OnErrorListener mOnErrorListener;

    @Override
    public void handleMessage(Message msg) {
        super.handleMessage(msg);

        HandlerObject object = (HandlerObject) msg.obj;
        switch (msg.what) {
            case UPDATE:
                ((OnUpdateListener) object.listener).onUpdate((int) object.value);
                break;
            case STOP:
                ((OnStopListener) object.listener).onStop();
                break;
            case ERROR:
                ((OnErrorListener) object.listener).onError((String) object.value);
                break;
            default:
                break;
        }
    }

    void noticeUpdate(int frame) {
        if (mOnUpdateListener == null) {
            return;
        }
        Message message = Message.obtain();
        message.what = UPDATE;
        message.obj = new HandlerObject(mOnUpdateListener, frame);
        sendMessage(message);
    }

    void setOnUpdateListener(OnUpdateListener l) {
        this.mOnUpdateListener = l;
    }

    void noticeStop() {
        if (mOnStopListener == null) {
            return;
        }
        Message message = Message.obtain();
        message.what = STOP;
        message.obj = new HandlerObject(mOnStopListener, null);
        sendMessage(message);
    }

    void setOnStopListener(OnStopListener l) {
        this.mOnStopListener = l;
    }

    void noticeError(String msg) {
        if (mOnErrorListener == null) {
            return;
        }
        Message message = Message.obtain();
        message.what = ERROR;
        message.obj = new HandlerObject(mOnErrorListener, msg);
        sendMessage(message);
    }

    void setOnErrorListener(OnErrorListener l) {
        this.mOnErrorListener = l;
    }

    private static class HandlerObject {
        Object listener;
        Object value;

        HandlerObject(Object listener, Object value) {
            this.listener = listener;
            this.value = value;
        }
    }
}

