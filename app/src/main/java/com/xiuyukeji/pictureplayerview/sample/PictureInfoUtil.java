package com.xiuyukeji.pictureplayerview.sample;

import android.support.annotation.IntDef;

/**
 * 图片信息管理
 *
 * @author Created by jz on 2017/3/29 17:36
 */
public class PictureInfoUtil {

    private static volatile PictureInfoUtil instance;

    public static PictureInfoUtil get() {
        if (instance == null) {
            synchronized (PictureInfoUtil.class) {
                if (instance == null)
                    instance = new PictureInfoUtil();
            }
        }
        return instance;
    }

    public static final int OPAQUE = 0, TRANSPARENT = 1;

    @IntDef({OPAQUE, TRANSPARENT})
    private @interface PictureType {
    }

    private final String mFileName = "lottielogo";
    private final String mTransparentFileName = "lottielogo_transparent";

    private String[] mPaths;
    private String[] mTransparentPaths;

    private long mDuration;

    private int mType = OPAQUE;

    private PictureInfoUtil() {
        int count = 271;

        mPaths = new String[count];
        mTransparentPaths = new String[count];

        for (int i = 0; i < count; i++) {
            mPaths[i] = String.format("%s/lottie_%s.png", mFileName, getIndex(count, i + 1));
            mTransparentPaths[i] = String.format("%s/lottie_%s.png", mTransparentFileName, getIndex(count, i + 1));
        }

        mDuration = count * 1000 / 25;
    }

    private static String getIndex(int max, int i) {
        return String.format("%0" + String.valueOf(max).length() + "d", i);
    }

    public void setType(@PictureType int type) {
        this.mType = type;
    }

    public int getType() {
        return mType;
    }

    public long getDuration() {
        return mDuration;
    }

    public String getFileName() {
        return mType == OPAQUE ? mFileName : mTransparentFileName;
    }

    public String[] getPaths() {
        return mType == OPAQUE ? mPaths : mTransparentPaths;
    }
}
