package com.cool.music.player;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;

import androidx.media3.common.AudioAttributes;
import androidx.media3.common.C;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;

import com.cool.music.bean.MusicBean;
import com.cool.music.event.MusicPlayEvent;
import com.cool.music.event.MusicProgressEvent;
import com.cool.music.event.PlayControlEvent;
import com.cool.music.event.SeekEvent;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.List;

/**
 * 音乐播放管理器（精简版）
 * 职责：协调各组件、提供统一的播放控制接口
 */
public class MusicPlayerManager {

    private static final String TAG = "MusicPlayerManager";
    private static volatile MusicPlayerManager instance;

    private ExoPlayer player;
    private final Handler handler = new Handler(Looper.getMainLooper());

    // 组件
    private final PlaylistManager playlistManager = new PlaylistManager();
    private final ProgressUpdater progressUpdater = new ProgressUpdater();
    private final PlayStatePersistence persistence = new PlayStatePersistence();

    // 状态标记
    private String currentUserId;
    private boolean isSeeking = false;
    private boolean isSwitchingTrack = false;

    private MusicPlayerManager() {}

    public static MusicPlayerManager getInstance() {
        if (instance == null) {
            synchronized (MusicPlayerManager.class) {
                if (instance == null) {
                    instance = new MusicPlayerManager();
                }
            }
        }
        return instance;
    }

    // ==================== 初始化 ====================

    public void init(Context context, String userId) {
        this.currentUserId = userId;

        if (player == null) {
            player = createPlayer(context);
            progressUpdater.attachPlayer(player);
            progressUpdater.setCallback(this::onProgressUpdate);
            registerEventBus();
        }

        restorePlaylistIfNeeded(userId);
    }

    private ExoPlayer createPlayer(Context context) {
        ExoPlayer exoPlayer = new ExoPlayer.Builder(context.getApplicationContext())
                .setAudioAttributes(
                        new AudioAttributes.Builder()
                                .setUsage(C.USAGE_MEDIA)
                                .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                                .build(),
                        true
                )
                .build();

        exoPlayer.setRepeatMode(Player.REPEAT_MODE_ALL);
        exoPlayer.addListener(new PlayerEventListener());

        return exoPlayer;
    }

    private void restorePlaylistIfNeeded(String userId) {
        if (!playlistManager.isEmpty() || TextUtils.isEmpty(userId)) {
            return;
        }

        PlayStatePersistence.RestoreResult result = persistence.restorePlaylist(userId);
        if (result != null) {
            playlistManager.setPlaylist(result.playlist, result.startIndex);
            player.setMediaItems(result.mediaItems, result.startIndex, 0);
            player.prepare();
        }
    }

    // ==================== 播放控制 ====================

    public void playList(List<MusicBean> musicList, int startIndex) {
        if (player == null || musicList == null || musicList.isEmpty()) return;

        PlaylistManager.MediaItemResult result = playlistManager.buildMediaItems(musicList, startIndex);
        if (result.isEmpty()) return;

        playlistManager.setPlaylist(result.validList, result.startIndex);

        player.setMediaItems(result.mediaItems, result.startIndex, 0);
        player.prepare();
        player.play();

        // ★ 新增：保存播放列表到数据库
        persistence.persistPlaylist(currentUserId, result.validList);

        persistAndTrack();
    }

    public void playSingle(MusicBean music) {
        if (player == null || music == null || TextUtils.isEmpty(music.getPath())) {
            return;
        }

        int existingIndex = playlistManager.findIndex(music);

        if (existingIndex >= 0) {
            // 歌曲已在列表中
            if (existingIndex == playlistManager.getCurrentIndex()) {
                return; // 已经在播放
            }
            player.seekTo(existingIndex, 0);
            playlistManager.setCurrentIndex(existingIndex);
        } else {
            // 新歌曲，插入到当前位置之后
            MediaItem item = playlistManager.buildSingleMediaItem(music);
            if (item == null) return;

            int insertIndex = playlistManager.getCurrentIndex() + 1;
            if (playlistManager.isEmpty()) {
                insertIndex = 0;
                playlistManager.insert(0, music);
                player.setMediaItem(item);
                player.prepare();
            } else {
                playlistManager.insert(insertIndex, music);
                player.addMediaItem(insertIndex, item);
                player.seekTo(insertIndex, 0);
            }
            playlistManager.setCurrentIndex(insertIndex);
        }

        if (!player.isPlaying()) {
            player.play();
        }

        persistAndTrack();
    }

    public void playOrPause() {
        if (player == null) return;
        if (player.isPlaying()) {
            player.pause();
        } else {
            player.play();
        }
    }

    public void play() {
        if (player != null) player.play();
    }

    public void pause() {
        if (player != null) player.pause();
    }

    public void stop() {
        if (player != null) player.stop();
    }

    public void next() {
        if (player == null || playlistManager.size() <= 1) return;

        isSwitchingTrack = true;
        ListeningRecordTracker.getInstance().stopTracking();

        if (player.hasNextMediaItem()) {
            player.seekToNextMediaItem();
        } else {
            player.seekTo(0, 0);
        }
        playlistManager.setCurrentIndex(player.getCurrentMediaItemIndex());

        if (!player.isPlaying()) {
            player.play();
        }

        handler.postDelayed(() -> {
            isSwitchingTrack = false;
            notifyMusicChanged();
            persistAndTrack();
        }, 300);
    }

    public void previous() {
        if (player == null || playlistManager.size() <= 1) return;

        isSwitchingTrack = true;
        ListeningRecordTracker.getInstance().stopTracking();

        if (player.hasPreviousMediaItem()) {
            player.seekToPreviousMediaItem();
        } else {
            player.seekTo(player.getMediaItemCount() - 1, 0);
        }
        playlistManager.setCurrentIndex(player.getCurrentMediaItemIndex());

        if (!player.isPlaying()) {
            player.play();
        }

        handler.postDelayed(() -> {
            isSwitchingTrack = false;
            notifyMusicChanged();
            persistAndTrack();
        }, 300);
    }

    public void seekTo(long positionMs) {
        if (player != null) {
            ListeningRecordTracker.getInstance().onSeek(positionMs);
            player.seekTo(positionMs);
        }
    }

    // ==================== Getter ====================

    public boolean isPlaying() {
        return player != null && player.isPlaying();
    }

    public MusicBean getCurrentMusic() {
        return playlistManager.getCurrentMusic();
    }

    public long getCurrentPosition() {
        return player != null ? player.getCurrentPosition() : 0;
    }

    public long getDuration() {
        return player != null ? player.getDuration() : 0;
    }

    public List<MusicBean> getPlayList() {
        return playlistManager.getPlaylist();
    }

    public int getCurrentIndex() {
        return playlistManager.getCurrentIndex();
    }

    public ExoPlayer getPlayer() {
        return player;
    }

    // ==================== 辅助方法 ====================

    private void persistAndTrack() {
        MusicBean music = getCurrentMusic();
        if (music != null) {
            persistence.persistCurrentMusic(currentUserId, music);
        }

        handler.postDelayed(() -> {
            MusicBean currentMusic = getCurrentMusic();
            long duration = player != null ? player.getDuration() : 0;
            if (currentMusic != null && duration > 0 && currentUserId != null) {
                ListeningRecordTracker.getInstance().startTracking(currentUserId, currentMusic, duration);
            }
        }, 500);
    }

    private void notifyMusicChanged() {
        MusicBean music = getCurrentMusic();
        if (music != null && player != null) {
            EventBus.getDefault().post(new MusicPlayEvent(music, player.isPlaying()));
        }
    }

    private void onProgressUpdate(long position, long duration) {
        ListeningRecordTracker.getInstance().updatePlaybackPosition(position);
    }

    // ==================== EventBus ====================

    private void registerEventBus() {
        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this);
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onPlayControlEvent(PlayControlEvent event) {
        if (player == null) return;

        switch (event.getAction()) {
            case PlayControlEvent.ACTION_PLAY:     play(); break;
            case PlayControlEvent.ACTION_PAUSE:    pause(); break;
            case PlayControlEvent.ACTION_TOGGLE:   playOrPause(); break;
            case PlayControlEvent.ACTION_NEXT:     next(); break;
            case PlayControlEvent.ACTION_PREVIOUS: previous(); break;
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onSeekEvent(SeekEvent event) {
        if (player == null) return;

        isSeeking = true;
        int position = event.getPosition();
        player.seekTo(position);
        ListeningRecordTracker.getInstance().onSeek(position);

        handler.postDelayed(() -> isSeeking = false, 400);

        long duration = player.getDuration();
        if (duration > 0) {
            EventBus.getDefault().post(new MusicProgressEvent(position, (int) duration));
        }
    }

    // ==================== Player 事件监听 ====================

    private class PlayerEventListener implements Player.Listener {

        @Override
        public void onIsPlayingChanged(boolean isPlaying) {
            if (isSeeking || isSwitchingTrack) return;

            MusicBean music = getCurrentMusic();
            if (music != null) {
                EventBus.getDefault().post(new MusicPlayEvent(music, isPlaying));
            }

            if (isPlaying) {
                ListeningRecordTracker.getInstance().resumeTracking();
                progressUpdater.start();
            } else {
                ListeningRecordTracker.getInstance().pauseTracking();
                progressUpdater.stop();
            }
        }

        @Override
        public void onMediaItemTransition(MediaItem mediaItem, int reason) {
            playlistManager.setCurrentIndex(player.getCurrentMediaItemIndex());
            MusicBean music = getCurrentMusic();

            if (!isSwitchingTrack && music != null) {
                persistence.persistCurrentMusic(currentUserId, music);
                EventBus.getDefault().post(new MusicPlayEvent(music, player.isPlaying()));

                long duration = player.getDuration();
                if (duration > 0 && currentUserId != null) {
                    ListeningRecordTracker.getInstance().startTracking(currentUserId, music, duration);
                }
            }
        }

        @Override
        public void onPlaybackStateChanged(int playbackState) {
            if (playbackState == Player.STATE_ENDED) {
                progressUpdater.stop();
                MusicBean music = getCurrentMusic();
                if (music != null) {
                    EventBus.getDefault().post(new MusicPlayEvent(music, false));
                }
                ListeningRecordTracker.getInstance().stopTracking();
            }

            if (playbackState == Player.STATE_READY && player.isPlaying()) {
                MusicBean music = getCurrentMusic();
                long duration = player.getDuration();
                if (music != null && duration > 0 && currentUserId != null) {
                    ListeningRecordTracker tracker = ListeningRecordTracker.getInstance();
                    if (!music.getId().equals(tracker.getCurrentMusicId())) {
                        tracker.startTracking(currentUserId, music, duration);
                    }
                }
            }
        }
    }

    // ==================== 生命周期 ====================

    public void release() {
        progressUpdater.release();
        ListeningRecordTracker.getInstance().stopTracking();

        if (EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().unregister(this);
        }

        if (player != null) {
            player.release();
            player = null;
        }

        playlistManager.clear();
        instance = null;
    }


}