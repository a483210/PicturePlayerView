package com.xiuyukeji.pictureplayerview.sample.step1;

import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;

import com.xiuyukeji.pictureplayerview.sample.PictureInfoUtil;
import com.xiuyukeji.pictureplayerview.sample.R;

/**
 * 步骤1演示
 *
 * @author Created by jz on 2017/3/29 17:17
 */
public class Step1Activity extends AppCompatActivity {

    private Toolbar mToolbar;
    private FloatingActionButton mFab;
    private PicturePlayerView1 mPicturePlayer;

    private boolean mIsPlayed;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_step1);
        findView();
        initView();
        setListener();
    }

    private void findView() {
        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        mFab = (FloatingActionButton) findViewById(R.id.fab);
        mPicturePlayer = (PicturePlayerView1) findViewById(R.id.player);
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
                mPicturePlayer.start(PictureInfoUtil.get().getPaths(),
                        PictureInfoUtil.get().getDuration());
            }
        });
    }
}
