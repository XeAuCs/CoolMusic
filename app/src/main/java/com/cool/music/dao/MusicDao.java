package com.cool.music.dao;

import com.cool.music.bean.MusicBean;
import com.cool.music.util.SQLiteDbUtils;

import java.util.List;

public class MusicDao {


    public static boolean isLikedMusic(String user_id, String music_id) {
        String sql = "SELECT * FROM d_user_like_music WHERE user_id = ? AND music_id = ?";
        if(SQLiteDbUtils.queryOne(sql, MusicBean.class, user_id, music_id)==null){
            return false;
        }else{
            return true;
        }


    }

    public static void setLikedMusic(String user_id, String music_id, boolean isAdd) {
        if (isAdd) {
            String id = java.util.UUID.randomUUID().toString();
            String sql = "INSERT OR IGNORE INTO d_user_like_music(id, user_id, music_id) VALUES(?,?,?)";
            SQLiteDbUtils.executeUpdate(sql, id, user_id, music_id);
        } else {
            String sql = "DELETE FROM d_user_like_music WHERE user_id = ? AND music_id = ?";
            SQLiteDbUtils.executeUpdate(sql, user_id, music_id);
        }
    }

    public static void addListeningRecord(String user_id, String music_id, String play_duration,String is_completed) {
        String id = java.util.UUID.randomUUID().toString();
        String sql = "INSERT INTO d_play_record (id, user_id, music_id, play_duration_seconds,is_completed) VALUES (?, ?, ?, ?,?)";
        SQLiteDbUtils.executeUpdate(sql,id, user_id, music_id, play_duration,is_completed);
    }

    // 查询所有音乐，按总播放时长排序
    public static List<MusicBean> getAllMusicByPlayDuration() {
        String sql = "SELECT m.*, COALESCE(SUM(p.play_duration_seconds), 0) AS total_play_duration " +
                "FROM d_music m " +
                "LEFT JOIN d_play_record p ON m.id = p.music_id " +
                "GROUP BY m.id " +
                "ORDER BY total_play_duration DESC";
        return SQLiteDbUtils.queryList(sql, MusicBean.class);
    }




    /**
     * 获取用户喜欢的所有歌曲，按用户播放时长排序
     * @param userId 用户ID
     * @return 用户喜欢的音乐列表，按播放时长降序排列
     */
    public static List<MusicBean> getLikedMusicByUserPlayDuration(String userId) {
        String sql = "SELECT m.*, COALESCE(SUM(p.play_duration_seconds), 0) AS total_play_duration " +
                "FROM d_music m " +
                "INNER JOIN d_user_like_music l ON m.id = l.music_id " +
                "LEFT JOIN d_play_record p ON m.id = p.music_id AND p.user_id = ? " +
                "WHERE l.user_id = ? " +
                "GROUP BY m.id " +
                "ORDER BY total_play_duration DESC";
        return SQLiteDbUtils.queryList(sql, MusicBean.class, userId, userId);
    }


    // 查询所有音乐，按某用户的播放时长排序
    public static List<MusicBean> getAllMusicByUserPlayDuration(String userId) {
        String sql = "SELECT m.*, COALESCE(SUM(p.play_duration_seconds), 0) AS total_play_duration " +
                "FROM d_music m " +
                "LEFT JOIN d_play_record p ON m.id = p.music_id AND p.user_id = ? " +
                "GROUP BY m.id " +
                "ORDER BY total_play_duration DESC";
        return SQLiteDbUtils.queryList(sql, MusicBean.class, userId);
    }


    /**
     * 模糊搜索音乐
     * @param keyword 搜索关键词
     * @return 匹配的音乐列表
     */
    public static List<MusicBean> searchMusic(String keyword) {
        String sql = "SELECT * FROM d_music WHERE name LIKE ? OR album LIKE ? OR singer LIKE ?";
        String pattern = "%" + keyword + "%";
        return SQLiteDbUtils.queryList(sql, MusicBean.class, pattern, pattern, pattern);
    }


    public static MusicBean findMusicById(String id){
        String sql="SELECT * FROM d_music WHERE id = ?";
        return SQLiteDbUtils.queryOne(sql,MusicBean.class,id);
    }





}
