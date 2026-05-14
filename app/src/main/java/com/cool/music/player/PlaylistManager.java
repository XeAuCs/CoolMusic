package com.cool.music.player;

import android.net.Uri;
import android.text.TextUtils;

import androidx.media3.common.MediaItem;

import com.cool.music.bean.MusicBean;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * 播放列表管理器
 * 职责：管理播放列表数据、构建 MediaItem、查找歌曲索引
 */
public class PlaylistManager {

    private final List<MusicBean> playList = new ArrayList<>();
    private int currentIndex = -1;

    public void setPlaylist(List<MusicBean> list, int startIndex) {
        playList.clear();
        playList.addAll(list);
        currentIndex = startIndex;
    }

    public void clear() {
        playList.clear();
        currentIndex = -1;
    }

    public void setCurrentIndex(int index) {
        this.currentIndex = index;
    }

    public int getCurrentIndex() {
        return currentIndex;
    }

    public MusicBean getCurrentMusic() {
        if (currentIndex >= 0 && currentIndex < playList.size()) {
            return playList.get(currentIndex);
        }
        return null;
    }

    public List<MusicBean> getPlaylist() {
        return new ArrayList<>(playList);
    }

    public int size() {
        return playList.size();
    }

    public boolean isEmpty() {
        return playList.isEmpty();
    }

    /**
     * 查找歌曲在列表中的索引
     */
    public int findIndex(MusicBean music) {
        if (music == null) return -1;

        for (int i = 0; i < playList.size(); i++) {
            MusicBean m = playList.get(i);
            if ((music.getId() != null && music.getId().equals(m.getId()))
                    || (music.getPath() != null && music.getPath().equals(m.getPath()))) {
                return i;
            }
        }
        return -1;
    }

    /**
     * 在指定位置插入歌曲
     */
    public void insert(int index, MusicBean music) {
        playList.add(index, music);
    }

    /**
     * 构建 MediaItem 列表
     * @return MediaItem 列表和有效歌曲列表的封装
     */
    public MediaItemResult buildMediaItems(List<MusicBean> musicList, int startIndex) {
        List<MusicBean> validList = new ArrayList<>();
        List<MediaItem> items = new ArrayList<>();
        int mappedStart = -1;

        for (int i = 0; i < musicList.size(); i++) {
            MusicBean m = musicList.get(i);
            String path = m.getPath();
            if (TextUtils.isEmpty(path)) continue;

            Uri uri = buildUri(path);
            MediaItem item = new MediaItem.Builder()
                    .setUri(uri)
                    .setMediaId(m.getId() == null ? String.valueOf(i) : m.getId())
                    .build();

            if (i == startIndex) {
                mappedStart = items.size();
            }

            validList.add(m);
            items.add(item);
        }

        if (mappedStart < 0) mappedStart = 0;

        return new MediaItemResult(validList, items, mappedStart);
    }

    public MediaItem buildSingleMediaItem(MusicBean music) {
        if (music == null || TextUtils.isEmpty(music.getPath())) {
            return null;
        }

        Uri uri = buildUri(music.getPath());
        return new MediaItem.Builder()
                .setUri(uri)
                .setMediaId(music.getId() != null ? music.getId() : String.valueOf(System.currentTimeMillis()))
                .build();
    }

    public static Uri buildUri(String path) {
        if (path.startsWith("content://") || path.startsWith("http://")
                || path.startsWith("https://") || path.startsWith("file://")) {
            return Uri.parse(path);
        } else {
            return Uri.fromFile(new File(path));
        }
    }

    /**
     * MediaItem 构建结果
     */
    public static class MediaItemResult {
        public final List<MusicBean> validList;
        public final List<MediaItem> mediaItems;
        public final int startIndex;

        public MediaItemResult(List<MusicBean> validList, List<MediaItem> mediaItems, int startIndex) {
            this.validList = validList;
            this.mediaItems = mediaItems;
            this.startIndex = startIndex;
        }

        public boolean isEmpty() {
            return mediaItems.isEmpty();
        }
    }
}