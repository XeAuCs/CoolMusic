package com.cool.music.util;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;

/**
 * 音乐文件元数据读取工具类
 */
public class MusicMetadataUtil {

    /**
     * 音乐元数据信息类
     */
    public static class MusicInfo {
        private String title;       // 歌名
        private String artist;      // 歌手
        private String album;       // 专辑名
        private int duration;      // 时长（毫秒）
        private Bitmap coverBitmap; // 封面图片
        private String year;        // 年份
        private String genre;       // 流派

        // Getters and Setters
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }

        public String getArtist() { return artist; }
        public void setArtist(String artist) { this.artist = artist; }

        public String getAlbum() { return album; }
        public void setAlbum(String album) { this.album = album; }

        public int getDuration() { return duration; }
        public void setDuration(int duration) { this.duration = duration; }

        public Bitmap getCoverBitmap() { return coverBitmap; }
        public void setCoverBitmap(Bitmap coverBitmap) { this.coverBitmap = coverBitmap; }

        public String getYear() { return year; }
        public void setYear(String year) { this.year = year; }

        public String getGenre() { return genre; }
        public void setGenre(String genre) { this.genre = genre; }

        /**
         * 获取格式化的时长字符串 (mm:ss)
         */
        public String getFormattedDuration() {
            long seconds = duration / 1000;
            long minutes = seconds / 60;
            seconds = seconds % 60;
            return String.format("%02d:%02d", minutes, seconds);
        }
    }

    /**
     * 从音乐文件路径读取元数据
     * @param path 音乐文件的绝对路径
     * @return MusicInfo对象，如果读取失败返回null
     */
    public static MusicInfo getMusicInfo(String path) {
        if (path == null || path.isEmpty()) {
            return null;
        }

        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        MusicInfo musicInfo = new MusicInfo();

        try {
            retriever.setDataSource(path);

            // 读取歌名
            String title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE);
            musicInfo.setTitle(title != null ? title : getFileNameWithoutExtension(path));

            // 读取歌手
            String artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST);
            musicInfo.setArtist(artist != null ? artist : "未知歌手");

            // 读取专辑名
            String album = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM);
            musicInfo.setAlbum(album != null ? album : "未知专辑");

            // 读取时长
            String durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
            if (durationStr != null) {
                musicInfo.setDuration(Integer.parseInt(durationStr));
            }

            // 读取年份
            String year = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_YEAR);
            musicInfo.setYear(year);

            // 读取流派
            String genre = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_GENRE);
            musicInfo.setGenre(genre);

            // 读取封面图片
            byte[] albumArt = retriever.getEmbeddedPicture();
            if (albumArt != null) {
                Bitmap bitmap = BitmapFactory.decodeByteArray(albumArt, 0, albumArt.length);
                musicInfo.setCoverBitmap(bitmap);
            }

            return musicInfo;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            try {
                retriever.release();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 只获取封面图片
     */
    public static Bitmap getCoverBitmap(String path) {
        if (path == null || path.isEmpty()) {
            return null;
        }

        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try {
            retriever.setDataSource(path);
            byte[] albumArt = retriever.getEmbeddedPicture();
            if (albumArt != null) {
                return BitmapFactory.decodeByteArray(albumArt, 0, albumArt.length);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                retriever.release();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    /**
     * 从路径中提取文件名（不含扩展名）
     */
    private static String getFileNameWithoutExtension(String path) {
        if (path == null) return "未知歌曲";
        int lastSlash = path.lastIndexOf('/');
        int lastDot = path.lastIndexOf('.');
        if (lastSlash < 0) lastSlash = -1;
        if (lastDot < 0 || lastDot < lastSlash) lastDot = path.length();
        return path.substring(lastSlash + 1, lastDot);
    }
}