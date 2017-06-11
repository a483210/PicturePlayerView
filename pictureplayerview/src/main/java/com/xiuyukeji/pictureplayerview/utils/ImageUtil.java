package com.xiuyukeji.pictureplayerview.utils;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.util.Log;

/**
 * 图片缓存处理
 *
 * @author Created by jz on 2017/3/26 16:13
 */
public class ImageUtil {

    private ImageUtil() {
    }

    /*
         *  判断该Bitmap是否可以设置到BitmapFactory.Options.inBitmap上
         */
    public static boolean canUseForInBitmap(Bitmap bitmap, BitmapFactory.Options options) {
        // 在Android4.4以后，如果要使用inBitmap的话，只需要解码的Bitmap比inBitmap设置的小就行了，对inSampleSize没有限制
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            int width = options.outWidth;
            int height = options.outHeight;
            if (options.inSampleSize > 0) {
                width /= options.inSampleSize;
                height /= options.inSampleSize;
            }
            int byteCount = width * height * getBytesPerPixel(bitmap.getConfig());
            return byteCount <= bitmap.getAllocationByteCount();
        }
        // 在Android4.4之前，如果想使用inBitmap的话，解码的Bitmap必须和inBitmap设置的宽高相等，且inSampleSize为1
        return bitmap.getWidth() == options.outWidth
                && bitmap.getHeight() == options.outHeight
                && options.inSampleSize == 1;
    }

    //获取每个像素所占用的Byte数
    private static int getBytesPerPixel(Bitmap.Config config) {
        if (config == Bitmap.Config.ARGB_8888) {
            return 4;
        } else if (config == Bitmap.Config.RGB_565) {
            return 2;
        } else if (config == Bitmap.Config.ARGB_4444) {
            return 2;
        } else if (config == Bitmap.Config.ALPHA_8) {
            return 1;
        }
        return 1;
    }

    /**
     * 回收Bitmap内存
     *
     * @param bitmap 图片
     */
    public static void recycleBitmap(Bitmap bitmap) {
        if (bitmap != null && !bitmap.isRecycled()) {
            bitmap.recycle();
        }
    }

}
