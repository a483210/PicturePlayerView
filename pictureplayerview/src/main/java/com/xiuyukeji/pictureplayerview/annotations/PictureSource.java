package com.xiuyukeji.pictureplayerview.annotations;

import android.support.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import static com.xiuyukeji.pictureplayerview.annotations.PictureSource.ASSETS;
import static com.xiuyukeji.pictureplayerview.annotations.PictureSource.FILE;

/**
 * 图片来源
 *
 * @author Created by jz on 2017/6/6 10:39
 */
@IntDef({FILE, ASSETS})
@Retention(RetentionPolicy.SOURCE)
public @interface PictureSource {
    /**
     * 来自sd文件
     */
    int FILE = 0;
    /**
     * 来自assets文件
     */
    int ASSETS = 1;
}