# PicturePlayerView

[![Download](https://img.shields.io/badge/Download-0.1.1-blue.svg)](https://bintray.com/a483210/maven/pictureplayerview/_latestVersion)
[![License](https://img.shields.io/badge/license-Apache%202-green.svg)](https://www.apache.org/licenses/LICENSE-2.0)

PicturePlayerView是基于TextureView的一个图片播放器，适用于播放多张图片组成的动画的场景。

**[文章地址](http://www.jianshu.com/p/53f9bd1fa1a6)**

## 效果

图片素材我使用的是[lottie-android](https://github.com/airbnb/lottie-android)的Logo

![不透明底](gifts/lottielogo_gif.gif)
![透明底](gifts/lottielogo_transparent_gif.gif)

## 引用

Gradle

    compile 'com.xiuyukeji.pictureplayerview:pictureplayerview:0.1.1'
    
Maven

    <dependency>
      <groupId>com.xiuyukeji.pictureplayerview</groupId>
      <artifactId>pictureplayerview</artifactId>
      <version>0.1.1</version>
      <type>pom</type>
    </dependency>

## 使用

首先添加至XML

    <com.xiuyukeji.pictureplayerview.PicturePlayerView
        android:id="@+id/player"
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        app:picture_opaque="false"
        app:picture_source="assets" />

然后在代码中设置数据源

    mPicturePlayerView.setDataSource("图片地址集合", "播放总时长");

最后调用start播放

    mPicturePlayerView.start();

**如果它有解决你的问题的话，请star下，谢谢。**
