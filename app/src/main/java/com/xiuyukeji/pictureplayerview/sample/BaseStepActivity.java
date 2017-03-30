package com.xiuyukeji.pictureplayerview.sample;

import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.TextView;

import com.xiuyukeji.pictureplayerview.sample.utils.FpsMeasureUtil.OnFpsListener;
import com.xiuyukeji.pictureplayerview.sample.utils.PictureInfoUtil;

/**
 * 基础步骤类
 *
 * @author Created by jz on 2017/3/30 11:13
 */
public abstract class BaseStepActivity extends AppCompatActivity {

    private Toolbar mToolbar;
    private FloatingActionButton mFab;
    private BasePicturePlayerView mPicturePlayerView;
    private TextView mFpsView;

    private boolean mIsPlayed;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(getLayoutId());
        findView();
        initView();
        setListener();
    }

    protected abstract int getLayoutId();

    private void findView() {
        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        mFab = (FloatingActionButton) findViewById(R.id.fab);
        mPicturePlayerView = (BasePicturePlayerView) findViewById(R.id.player);
        mFpsView = (TextView) findViewById(R.id.fps);
    }

    private void initView() {
        setSupportActionBar(mToolbar);
    }

    private void setListener() {
        mFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mIsPlayed) {
                    Snackbar.make(mFab, "不不不，这里只能播放一次！！！", Snackbar.LENGTH_LONG).show();
                    return;
                }

                mIsPlayed = true;
                mPicturePlayerView.start(PictureInfoUtil.get().getPaths(),
                        PictureInfoUtil.get().getDuration());
            }
        });
        mPicturePlayerView.setOnFpsListener(new OnFpsListener() {
            @Override
            public void onFps(String text) {
                mFpsView.setText(text);
            }
        });
    }
}
