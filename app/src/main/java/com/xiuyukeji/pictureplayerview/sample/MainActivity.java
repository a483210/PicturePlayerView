package com.xiuyukeji.pictureplayerview.sample;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.xiuyukeji.pictureplayerview.PicturePlayerView;
import com.xiuyukeji.pictureplayerview.sample.step1.Step1Activity;
import com.xiuyukeji.pictureplayerview.sample.step2.Step2Activity;
import com.xiuyukeji.pictureplayerview.sample.step3.Step3Activity;
import com.xiuyukeji.pictureplayerview.sample.utils.PictureInfoUtil;

/**
 * 完整演示
 *
 * @author Created by jz on 2017/3/29 17:49
 */
public class MainActivity extends AppCompatActivity {

    private static final int ACTION_USE_OPAQUE = 0, ACTION_USE_TRANSPARENT = 1;

    private Toolbar mToolbar;
    private FloatingActionButton mStartFab;
    private FloatingActionButton mPauseFab;
    private FloatingActionButton mStopFab;
    private PicturePlayerView mPicturePlayerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        findView();
        initView();
        setListener();
    }

    private void findView() {
        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        mStartFab = (FloatingActionButton) findViewById(R.id.start);
        mPauseFab = (FloatingActionButton) findViewById(R.id.pause);
        mStopFab = (FloatingActionButton) findViewById(R.id.stop);
        mPicturePlayerView = (PicturePlayerView) findViewById(R.id.player);
    }

    private void initView() {
        setSupportActionBar(mToolbar);
        resetDataSource();
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
    }

    private void resetDataSource() {
        mPicturePlayerView.setDataSource(PictureInfoUtil.get().getPaths(),
                PictureInfoUtil.get().getDuration());
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.removeItem(ACTION_USE_OPAQUE);
        menu.removeItem(ACTION_USE_TRANSPARENT);

        if (mPicturePlayerView.isPlaying()) {
            return true;
        }
        if (PictureInfoUtil.get().getType() == PictureInfoUtil.TRANSPARENT) {
            menu.add(1, ACTION_USE_OPAQUE, 100, R.string.action_use_opaque);
        } else {
            menu.add(1, ACTION_USE_TRANSPARENT, 100, R.string.action_use_transparent);
        }
        return true;
    }

    @Override
    public boolean onCreatePanelMenu(int featureId, Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        switch (id) {
            case R.id.action_step1:
                startActivity(new Intent(this, Step1Activity.class));
                return true;
            case R.id.action_step2:
                startActivity(new Intent(this, Step2Activity.class));
                return true;
            case R.id.action_step3:
                startActivity(new Intent(this, Step3Activity.class));
                return true;
            case ACTION_USE_OPAQUE:
                PictureInfoUtil.get().setType(PictureInfoUtil.OPAQUE);
                resetDataSource();
                break;
            case ACTION_USE_TRANSPARENT:
                PictureInfoUtil.get().setType(PictureInfoUtil.TRANSPARENT);
                resetDataSource();
                break;
        }
        return super.onOptionsItemSelected(item);
    }
}
