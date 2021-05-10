package com.example.musicboxzhu;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.media.MediaPlayer;
import android.os.IBinder;
import android.util.Log;

import java.io.IOException;

public class MusicService extends Service {

    MyReceiver serviceReceiver;
    AssetManager am;
    Thread processThread;
    static final String[] musics = { "legendsneverdie.mp3","promise.mp3", "wish.mp3", "beautiful.mp3" };
    MediaPlayer mPlayer;
    // 当前的状态，0x11代表没有播放；0x12代表正在播放；0x13代表暂停
    int status = 0x11;
    // 记录当前正在播放的音乐
    int current = 0;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        am = getAssets(); //获取附件管理器

        // 创建BroadcastReceiver
        serviceReceiver = new MyReceiver();
        // 创建IntentFilter
        IntentFilter filter = new IntentFilter();
        filter.addAction(MainActivity.CTL_ACTION);
        registerReceiver(serviceReceiver, filter);

        mPlayer = new MediaPlayer();
        // 为MediaPlayer播放完成事件绑定监听器
        mPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                Log.d("MusicService", "歌曲播放完毕");
                current++;
                if (current >= 4)
                {
                    current = 0;
                }
                //发送广播通知Activity更改文本框
                Intent sendIntent = new Intent(MainActivity.UPDATE_ACTION);
                sendIntent.putExtra("current", current);
                // 发送广播，将被Activity组件中的BroadcastReceiver接收到
                sendBroadcast(sendIntent);
                // 准备并播放音乐
                prepareAndPlay(musics[current]);
            }
        });
        processThread = new Thread(() -> {
            while (true)
            {
                // 如果是播放状态
                if (status == 0x12)
                {
                    try {
                        // 休眠一秒
                        Thread.sleep(1000);
                        // 给主线程发送广播
                        Intent sendIntent1 = new Intent(MainActivity.UPDATE_ACTION);
                        // 当前进度
                        sendIntent1.putExtra("currentTime", mPlayer.getCurrentPosition());
                        // 总进度
                        sendIntent1.putExtra("totalTime", mPlayer.getDuration());
                        // 发送广播，将被Activity组件中的BroadcastReceiver接收到
                        sendBroadcast(sendIntent1);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        processThread.start();


    }

    public class MyReceiver extends BroadcastReceiver
    {
        @Override
        public void onReceive(final Context context, Intent intent)
        {
            // 获取Intent中的Control状态
            int control = intent.getIntExtra("control", -1);
            switch (control)
            {
                // 播放或暂停
                case 1:
                    // 原来处于没有播放状态
                    if (status == 0x11)
                    {
                        // 准备并播放音乐
                        prepareAndPlay(musics[current]);
                        status = 0x12;
                    }
                    // 原来处于播放状态
                    else if (status == 0x12)
                    {
                        // 暂停
                        mPlayer.pause();
                        // 改变为暂停状态
                        status = 0x13;
                    }
                    // 原来处于暂停状态
                    else if (status == 0x13)
                    {
                        // 播放
                        mPlayer.start();
                        // 改变状态
                        status = 0x12;
                    }
                    break;
                //停止声音
                case 2:
                    if (status == 0x12 || status == 0x13) {
                        mPlayer.stop();
                        status = 0x11;
                    }
                    break;
                //上一首
                case 3:
                    // 如果原来正在播放或暂停
                    if (status == 0x12 || status == 0x13) {
                        mPlayer.stop();
                        if (current - 1 < 0) {
                            current = musics.length - 1;
                        } else {
                            current--;
                        }
                        prepareAndPlay(musics[current]);
                        status = 0x12;
                    }
                    break;
                //下一首
                case 4:
                    if (status == 0x12 || status == 0x13) {
                        mPlayer.stop();
                        if (current + 1 >= musics.length) {
                            current = 0;
                        } else {
                            current++;
                        }
                        prepareAndPlay(musics[current]);
                        status = 0x12;
                    }
                    break;
            }
            // 广播通知Activity更改图标、文本框
            Intent sendIntent = new Intent(MainActivity.UPDATE_ACTION);
            sendIntent.putExtra("update", status);
            sendIntent.putExtra("current", current);
            // 发送广播，将被Activity组件中的BroadcastReceiver接收到
            sendBroadcast(sendIntent);
        }
    }

    private void prepareAndPlay(String music)
    {
        try
        {
            // 打开指定音乐文件
            AssetFileDescriptor afd = am.openFd(music);
            mPlayer.reset();
            // 使用MediaPlayer加载指定的声音文件。
            mPlayer.setDataSource(afd.getFileDescriptor(),
                    afd.getStartOffset(), afd.getLength());
            // 准备声音
            mPlayer.prepare();
            // 播放
            mPlayer.start();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }



}
