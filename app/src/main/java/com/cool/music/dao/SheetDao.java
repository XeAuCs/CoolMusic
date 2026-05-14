package com.cool.music.dao;

import com.cool.music.bean.MusicBean;
import com.cool.music.bean.SheetBean;
import com.cool.music.util.SQLiteDbUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class SheetDao {

    public static List<SheetBean> getRandomSheets(int count) {
        String sql = "select * from d_sheet where is_public = '1'";
        List<SheetBean> allMusics = SQLiteDbUtils.queryList(sql, SheetBean.class);
        List<SheetBean> randomMusics = new ArrayList<>();

        // 打乱列表顺序
        Collections.shuffle(allMusics);

        // 取前count个元素，避免数组越界
        int actualCount = Math.min(count, allMusics.size());
        for (int i = 0; i < actualCount; i++) {
            randomMusics.add(allMusics.get(i));
        }

        return randomMusics;
    }

    public static SheetBean getOneSheet(String sheet_id) {
        String sql = "select * from d_sheet_music where sheet_id = ?";
        return SQLiteDbUtils.queryOne(sql, SheetBean.class, sheet_id);
    }

    public static String getSongCount(String sheet_id) {
        String sql = "select * from d_sheet_music where sheet_id = ?";
        int count = SQLiteDbUtils.queryCount(sql, sheet_id);
        return String.valueOf(count);
    }

    public static List<MusicBean> getPlayMusicOnSheet(String sheetId) {
        // 用 JOIN 一步到位
        String sql = "SELECT m.* FROM d_music m " +
                "INNER JOIN d_sheet_music sm ON m.id = sm.music_id " +
                "WHERE sm.sheet_id = ?";
        return SQLiteDbUtils.queryList(sql, MusicBean.class, sheetId);
    }

    // 修复：插入前先检查是否已存在
    public static void insertPlayMusic(String user_id, String music_id) {
        String id = java.util.UUID.randomUUID().toString();
        String sql = "INSERT OR IGNORE INTO d_music_play_list (id, music_id, user_id) VALUES(?, ?, ?)";
        SQLiteDbUtils.executeUpdate(sql, id, music_id, user_id);
    }

    public static List<MusicBean> getCurrentUserPlayMusic(String user_id) {
        // 按加入时间排序（升序：最早加入的在前面）
        String sql = "SELECT * FROM d_music_play_list WHERE user_id = ? ORDER BY add_time ASC";
        List<Map<String, String>> list = SQLiteDbUtils.queryMapList(sql, user_id);

        List<MusicBean> result = new ArrayList<>();
        for (Map<String, String> map : list) {
            String musicId = map.get("music_id");
            if (musicId != null) {
                String sql2 = "SELECT * FROM d_music WHERE id = ?";
                MusicBean music = SQLiteDbUtils.queryOne(sql2, MusicBean.class, musicId);
                if (music != null) {
                    result.add(music);
                }
            }
        }
        return result;
    }

    public static void updateCurrentUserPlayMusic(String user_id, String is_playing) {
        String sql = "UPDATE d_music_play_list SET is_playing = ? WHERE user_id = ?";
        SQLiteDbUtils.executeUpdate(sql, is_playing, user_id);
    }

    public static void updateCurrentUserPlayMusic(String user_id, String music_id, String is_playing) {
        String sql = "UPDATE d_music_play_list SET is_playing = ? WHERE user_id = ? AND music_id = ?";
        SQLiteDbUtils.executeUpdate(sql, is_playing, user_id, music_id);
    }


    public static MusicBean getCurrentUserPlayMusic(String user_id, String is_playing){
        String sql = "SELECT * FROM d_music_play_list WHERE user_id = ? AND is_playing = ?";  // ✅ 改成 SELECT
        List<Map<String, String>> list = SQLiteDbUtils.queryMapList(sql, user_id, is_playing);
        if (list != null && list.size() > 0) {
            Map<String, String> map = list.get(0);
            String id = map.get("music_id");
            String sql1 = "SELECT * FROM d_music WHERE id = ?";
            MusicBean music = SQLiteDbUtils.queryOne(sql1, MusicBean.class, id);
            return music;
        }
        return null;
    }

    public static void setCurrentPlayingMusic(String userId, String musicId) {
        // 1. 先把该用户所有歌曲的 is_playing 设为 0
        String resetSql = "UPDATE d_music_play_list SET is_playing = '0' WHERE user_id = ?";
        SQLiteDbUtils.executeUpdate(resetSql, userId);

        // 2. 确保这首歌在播放列表中
        insertPlayMusic(userId, musicId);


        // 3. 设置当前歌曲为播放状态
        String updateSql = "UPDATE d_music_play_list SET is_playing = '1' WHERE user_id = ? AND music_id = ?";
        SQLiteDbUtils.executeUpdate(updateSql, userId, musicId);
    }

    /**
     * 获取当前正在播放的音乐（修复后的版本）
     */
    public static MusicBean getCurrentPlayingMusic(String userId) {
        // 查询 is_playing = '1' 的记录
        String sql = "SELECT * FROM d_music_play_list WHERE user_id = ? AND is_playing = '1'";
        List<Map<String, String>> list = SQLiteDbUtils.queryMapList(sql, userId);

        if (list != null && !list.isEmpty()) {
            String musicId = list.get(0).get("music_id");
            if (musicId != null) {
                String musicSql = "SELECT * FROM d_music WHERE id = ?";
                return SQLiteDbUtils.queryOne(musicSql, MusicBean.class, musicId);
            }
        }
        return null;
    }

    /**
     * 清除播放状态（暂停/停止时调用）
     */
    public static void clearPlayingState(String userId) {
        String sql = "UPDATE d_music_play_list SET is_playing = '0' WHERE user_id = ?";
        SQLiteDbUtils.executeUpdate(sql, userId);
    }

    public static List<SheetBean> getSheetByUserId(String userId) {
        String sql = "SELECT * FROM d_sheet WHERE owner_id = ?";
        return SQLiteDbUtils.queryList(sql, SheetBean.class, userId);
    }

    public static SheetBean getSheetBySheetId(String sheetId) {
        String sql = "SELECT * FROM d_sheet WHERE id = ?";
        return SQLiteDbUtils.queryOne(sql, SheetBean.class, sheetId);
    }


    public static String createNewSheet(String userId){
        String id = java.util.UUID.randomUUID().toString();
        String sql = "INSERT INTO d_sheet (id,name,owner_id) VALUES(?,?,?)";
        SQLiteDbUtils.executeUpdate(sql,id,"新建歌单",userId);
        return id;
    }

    public static void deleteSheet(String sheetId) {
        // 1. 先删除歌单中的所有歌曲关联
        String deleteMusics = "DELETE FROM d_sheet_music WHERE sheet_id = ?";
        SQLiteDbUtils.executeUpdate(deleteMusics, sheetId);

        // 2. 再删除歌单本身
        String deleteSheet = "DELETE FROM d_sheet WHERE id = ?";
        SQLiteDbUtils.executeUpdate(deleteSheet, sheetId);
    }
    public static void removeMusicFromSheet(String sheetId, String musicId) {
        String sql = "DELETE FROM d_sheet_music WHERE sheet_id = ? AND music_id = ?";
        SQLiteDbUtils.executeUpdate(sql, sheetId, musicId);
    }


    /**
     * 保存用户的播放列表
     * 先清空原有列表，再批量插入新列表
     */
    public static void saveCurrentUserPlayMusic(String userId, List<MusicBean> playlist) {
        if (userId == null || playlist == null || playlist.isEmpty()) {
            return;
        }

        // 1. 清空该用户原有的播放列表
        String deleteSql = "DELETE FROM d_music_play_list WHERE user_id = ?";
        SQLiteDbUtils.executeUpdate(deleteSql, userId);

        // 2. 批量插入新的播放列表
        String insertSql = "INSERT INTO d_music_play_list (id, music_id, user_id, is_playing) VALUES (?, ?, ?, ?)";

        for (int i = 0; i < playlist.size(); i++) {
            MusicBean music = playlist.get(i);
            if (music == null || music.getId() == null) {
                continue;
            }

            String id = UUID.randomUUID().toString();
            // 第一首歌默认设为正在播放
            String isPlaying = (i == 0) ? "1" : "0";

            SQLiteDbUtils.executeUpdate(insertSql, id, music.getId(), userId, isPlaying);
        }
    }

    /**
     * 保存用户的播放列表（增量模式，不删除原有歌曲）
     */
    public static void saveCurrentUserPlayMusicIncremental(String userId, List<MusicBean> playlist) {
        if (userId == null || playlist == null || playlist.isEmpty()) {
            return;
        }

        for (MusicBean music : playlist) {
            if (music == null || music.getId() == null) {
                continue;
            }
            // insertPlayMusic 已经有 INSERT OR IGNORE，会自动去重
            insertPlayMusic(userId, music.getId());
        }
    }

    /**
     * 更新歌单信息
     * @param sheetId 歌单ID
     * @param name 歌单名称
     * @param description 歌单描述
     * @param coverPath 封面图片路径
     * @param isPublic 是否公开 ("1"=公开, "0"=私密)
     */
    public static void updateSheetInfo(String sheetId, String name, String description, String coverPath, String isPublic) {
        if (sheetId == null || sheetId.isEmpty()) {
            return;
        }

        String sql = "UPDATE d_sheet SET name = ?, description = ?, cover_image = ?, is_public = ? WHERE id = ?";
        SQLiteDbUtils.executeUpdate(sql, name, description, coverPath, isPublic, sheetId);
    }


    public static boolean addMusicToSheet(String sheet_id, String music_id) {
        String id = java.util.UUID.randomUUID().toString();
        String sql = "INSERT INTO d_sheet_music (id,sheet_id, music_id) VALUES (?,?, ?)";
        return SQLiteDbUtils.executeUpdate(sql, id, sheet_id, music_id) > 0;
    }

    public static boolean isMusicInSheet(String sheet_id, String music_id) {
        String sql = "SELECT * FROM d_sheet_music WHERE sheet_id = ? AND music_id = ?";
        int count = SQLiteDbUtils.queryCount(sql, sheet_id, music_id);
        return count > 0;
    }
}