package com.cool.music.player;

import android.text.TextUtils;
import android.util.Log;

import androidx.media3.common.MediaItem;

import com.cool.music.bean.MusicBean;
import com.cool.music.dao.SheetDao;

import java.util.ArrayList;
import java.util.List;

/**
 * 播放状态持久化管理器
 * 职责：保存和恢复播放状态
 */
public class PlayStatePersistence {

    private static final String TAG = "PlayStatePersistence";
    private static final long DEBOUNCE_INTERVAL = 500;

    private String lastPersistedMusicId = null;
    private long lastPersistTime = 0;

    /**
     * 持久化当前播放的音乐（带防抖）
     */
    public void persistCurrentMusic(String userId, MusicBean music) {
        if (TextUtils.isEmpty(userId) || music == null || music.getId() == null) {
            return;
        }

        // 防抖：同一首歌 500ms 内不重复持久化
        long now = System.currentTimeMillis();
        if (music.getId().equals(lastPersistedMusicId) && (now - lastPersistTime) < DEBOUNCE_INTERVAL) {
            return;
        }
        lastPersistedMusicId = music.getId();
        lastPersistTime = now;

        new Thread(() -> {
            try {
                SheetDao.setCurrentPlayingMusic(userId, music.getId());
                Log.d(TAG, "Persisted music: " + music.getName());
            } catch (Exception e) {
                Log.e(TAG, "Failed to persist music", e);
            }
        }).start();
    }

    /**
     * 恢复播放列表
     * @return 恢复结果，包含播放列表和起始索引
     */
    public RestoreResult restorePlaylist(String userId) {
        if (TextUtils.isEmpty(userId)) {
            return null;
        }

        List<MusicBean> allPlayList = SheetDao.getCurrentUserPlayMusic(userId);
        if (allPlayList == null || allPlayList.isEmpty()) {
            return null;
        }

        MusicBean lastPlaying = SheetDao.getCurrentPlayingMusic(userId);

        List<MediaItem> mediaItems = new ArrayList<>();
        int startIndex = 0;

        for (int i = 0; i < allPlayList.size(); i++) {
            MusicBean music = allPlayList.get(i);
            if (TextUtils.isEmpty(music.getPath())) continue;

            MediaItem item = new MediaItem.Builder()
                    .setUri(PlaylistManager.buildUri(music.getPath()))
                    .setMediaId(music.getId() != null ? music.getId() : String.valueOf(i))
                    .build();
            mediaItems.add(item);

            if (lastPlaying != null && music.getId() != null
                    && music.getId().equals(lastPlaying.getId())) {
                startIndex = mediaItems.size() - 1;
            }
        }

        if (mediaItems.isEmpty()) {
            return null;
        }

        Log.d(TAG, "Restored playlist: " + allPlayList.size() + " songs, start at: " + startIndex);
        return new RestoreResult(allPlayList, mediaItems, startIndex);
    }

    // PlayStatePersistence.java 中添加

    /**
     * 持久化播放列表
     */
    public void persistPlaylist(String userId, List<MusicBean> playlist) {
        if (TextUtils.isEmpty(userId) || playlist == null || playlist.isEmpty()) {
            return;
        }

        new Thread(() -> {
            try {
                // 调用 SheetDao 保存播放列表
                SheetDao.saveCurrentUserPlayMusic(userId, playlist);
                Log.d(TAG, "Persisted playlist: " + playlist.size() + " songs");
            } catch (Exception e) {
                Log.e(TAG, "Failed to persist playlist", e);
            }
        }).start();
    }

    /**
     * 恢复结果
     */
    public static class RestoreResult {
        public final List<MusicBean> playlist;
        public final List<MediaItem> mediaItems;
        public final int startIndex;

        public RestoreResult(List<MusicBean> playlist, List<MediaItem> mediaItems, int startIndex) {
            this.playlist = playlist;
            this.mediaItems = mediaItems;
            this.startIndex = startIndex;
        }
    }
}