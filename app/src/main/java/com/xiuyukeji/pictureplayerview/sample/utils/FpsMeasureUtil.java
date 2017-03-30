package com.xiuyukeji.pictureplayerview.sample.utils;

import android.os.SystemClock;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * 帧数测量
 *
 * @author Created by jz on 2017/3/30 9:28
 */
public class FpsMeasureUtil {

    private ArrayList<Long> mTimes;

    private DecimalFormat mDecimalFormat;

    public FpsMeasureUtil() {
        mTimes = new ArrayList<>();
        mDecimalFormat = new DecimalFormat("0.0 fps");
    }

    public void measureFps() {
        long currentTime = SystemClock.uptimeMillis();

        mTimes.add(currentTime);

        long lastTime = currentTime - 1000;

        Iterator<Long> ite = mTimes.iterator();
        while (ite.hasNext()) {
            if (ite.next() < lastTime) {
                ite.remove();
            } else {
                break;
            }
        }
    }

    public String getFpsText() {
        int count = mTimes.size();
        if (count <= 1) {
            return "0.0 fps";
        }
        float spaceTime = mTimes.get(count - 1) - mTimes.get(0);
        return mDecimalFormat.format((1000 - spaceTime) * count / spaceTime + count - 1);
    }

    /**
     * Fps回调
     *
     * @author Created by jz on 2017/3/30 10:59
     */
    public interface OnFpsListener {
        void onFps(String text);
    }

}
