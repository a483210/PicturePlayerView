package com.xiuyukeji.pictureplayerview.sample.gl;

import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.xiuyukeji.glpictureplayerview.GLPicturePlayerView;
import com.xiuyukeji.pictureplayerview.interfaces.OnStopListener;
import com.xiuyukeji.pictureplayerview.interfaces.OnUpdateListener;
import com.xiuyukeji.pictureplayerview.sample.R;
import com.xiuyukeji.pictureplayerview.sample.utils.FpsMeasureUtil;
import com.xiuyukeji.pictureplayerview.sample.utils.PictureInfoUtil;

/**
 * 基础步骤类
 *
 * @author Created by jz on 2017/3/30 11:13
 */
public class GLActivity extends AppCompatActivity {

    private Toolbar mToolbar;
    private FloatingActionButton mStartFab;
    private FloatingActionButton mPauseFab;
    private FloatingActionButton mStopFab;
    private GLPicturePlayerView mPicturePlayerView;
    private TextView mFpsView;

    private FpsMeasureUtil mFpsMeasureUtil;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gl);
        findView();
        initView();
        setListener();
    }

    private void findView() {
        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        mStartFab = (FloatingActionButton) findViewById(R.id.start);
        mPauseFab = (FloatingActionButton) findViewById(R.id.pause);
        mStopFab = (FloatingActionButton) findViewById(R.id.stop);
        mPicturePlayerView = (GLPicturePlayerView) findViewById(R.id.player);
        mFpsView = (TextView) findViewById(R.id.fps);

        mFpsMeasureUtil = new FpsMeasureUtil();
    }

    private void initView() {
        setSupportActionBar(mToolbar);

        mPicturePlayerView.setLoop(true);
        mPicturePlayerView.setDataSource(PictureInfoUtil.get().getPaths(),
                PictureInfoUtil.get().getDuration());
    }

    private void setListener() {
        mStartFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mPicturePlayerView.isPlaying()) {
                    mPicturePlayerView.resume();
                } else {
                    mPicturePlayerView.start();
                }
            }
        });
        mPauseFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mPicturePlayerView.pause();
            }
        });
        mStopFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mPicturePlayerView.stop();
            }
        });
        mPicturePlayerView.setOnUpdateListener(new OnUpdateListener() {
            @Override
            public void onUpdate(int frame) {
                mFpsMeasureUtil.measureFps();
                mFpsView.setText(mFpsMeasureUtil.getFpsText());
            }
        });
        mPicturePlayerView.setOnStopListener(new OnStopListener() {
            @Override
            public void onStop() {
                Log.i("Tool", "stop");
            }
        });
    }
}
