package com.cool.music.player;

import android.os.Handler;
import android.os.Looper;

import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;

import com.cool.music.event.MusicProgressEvent;

import org.greenrobot.eventbus.EventBus;

/**
 * 播放进度更新器
 * 职责：定时发送播放进度事件
 */
public class ProgressUpdater {

    private static final long UPDATE_INTERVAL = 50;

    private final Handler handler;
    private Runnable progressRunnable;
    private boolean isUpdating = false;

    private ExoPlayer player;
    private ProgressCallback callback;

    public interface ProgressCallback {
        void onProgressUpdate(long position, long duration);
    }

    public ProgressUpdater() {
        handler = new Handler(Looper.getMainLooper());
    }

    public void attachPlayer(ExoPlayer player) {
        this.player = player;
    }

    public void setCallback(ProgressCallback callback) {
        this.callback = callback;
    }

    public void start() {
        if (isUpdating || player == null) {
            return;
        }
        isUpdating = true;

        progressRunnable = new Runnable() {
            @Override
            public void run() {
                if (player != null && isUpdating) {
                    int playbackState = player.getPlaybackState();

                    if (player.isPlaying() && playbackState == Player.STATE_READY) {
                        long position = player.getCurrentPosition();
                        long duration = player.getDuration();

                        if (duration > 0 && position >= 0 && position <= duration) {
                            // 发送 EventBus 事件
                            EventBus.getDefault().post(new MusicProgressEvent(
                                    (int) position, (int) duration));

                            // 回调通知
                            if (callback != null) {
                                callback.onProgressUpdate(position, duration);
                            }
                        }
                    }

                    if (player.isPlaying() || playbackState == Player.STATE_BUFFERING) {
                        handler.postDelayed(this, UPDATE_INTERVAL);
                    } else {
                        isUpdating = false;
                    }
                } else {
                    isUpdating = false;
                }
            }
        };
        handler.post(progressRunnable);
    }

    public void stop() {
        isUpdating = false;
        if (handler != null && progressRunnable != null) {
            handler.removeCallbacks(progressRunnable);
            progressRunnable = null;
        }
    }

    public void release() {
        stop();
        player = null;
        callback = null;
    }
}