package com.xiuyukeji.glpictureplayerview;

import android.content.Context;
import android.graphics.Bitmap;
import android.opengl.GLES20;
import android.opengl.GLUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/**
 * openGL工具类
 *
 * @author Created by jz on 2017/4/7 10:07
 */
public class OpenGLUtils {

    private OpenGLUtils() {
    }

    public static int[] loadTexture(Bitmap bitmap, int[] textureIds) {
        if (textureIds == null) {
            textureIds = new int[1];
            GLES20.glGenTextures(1, textureIds, 0);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureIds[0]);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                    GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                    GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                    GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                    GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);

            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);
        } else {
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureIds[0]);
            GLUtils.texSubImage2D(GLES20.GL_TEXTURE_2D, 0, 0, 0, bitmap);
        }
        return textureIds;
    }

    public static FloatBuffer getFloatBuffer(float[] values) {
        final FloatBuffer buffer = getBuffer(values.length).asFloatBuffer();
        buffer.put(values);
        buffer.position(0);
        return buffer;
    }

    private static ByteBuffer getBuffer(int length) {
        return ByteBuffer.allocateDirect(length * 4)
                .order(ByteOrder.nativeOrder());
    }

    public static int loadProgram(Context context, int vertexResourceId, int fragmentResourceId) {
        final int vertexShader = OpenGLUtils.loadShader(GLES20.GL_VERTEX_SHADER,
                OpenGLUtils.readShader(context, vertexResourceId));
        final int fragmentShader = OpenGLUtils.loadShader(GLES20.GL_FRAGMENT_SHADER,
                OpenGLUtils.readShader(context, fragmentResourceId));

        final int program = GLES20.glCreateProgram();
        GLES20.glAttachShader(program, vertexShader);
        GLES20.glAttachShader(program, fragmentShader);
        GLES20.glLinkProgram(program);

        GLES20.glDeleteShader(vertexShader);
        GLES20.glDeleteShader(fragmentShader);
        return program;
    }

    public static int loadShader(int type, String shaderCode) {
        final int shader = GLES20.glCreateShader(type);

        GLES20.glShaderSource(shader, shaderCode);
        GLES20.glCompileShader(shader);
        return shader;
    }

    public static String readShader(Context context, int resourceId) {
        final StringBuilder body = new StringBuilder();

        try {
            final InputStream inputStream = context.getResources().openRawResource(
                    resourceId);
            final InputStreamReader inputStreamReader = new InputStreamReader(
                    inputStream);
            final BufferedReader bufferedReader = new BufferedReader(
                    inputStreamReader);

            String nextLine;
            while ((nextLine = bufferedReader.readLine()) != null) {
                body.append(nextLine);
                body.append('\n');
            }
            inputStream.close();
            inputStreamReader.close();
            bufferedReader.close();
        } catch (IOException e) {
            return null;
        }
        return body.toString();
    }
}
