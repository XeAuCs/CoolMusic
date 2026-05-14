package com.cool.music.util;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.Nullable;

import com.cool.music.R;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class DBUtil extends SQLiteOpenHelper {

    private static final int version = 79;
    private static final String databaseName = "db_music.db";

    private static DBUtil instance;
    private static Context appContext; // 改为静态变量，在 super() 之前赋值
    public static SQLiteDatabase con;

    // 私有构造函数
    private DBUtil(@Nullable Context context) {
        super(context, databaseName, null, version);
        // 注意：不要在这里赋值 context，因为 super() 可能已经触发了 onCreate/onUpgrade
    }

    // 单例模式获取实例
    public static synchronized DBUtil getInstance(Context context) {
        if (instance == null) {
            // 关键：在调用构造函数之前先保存 context
            appContext = context.getApplicationContext();
            instance = new DBUtil(context);
        }
        return instance;
    }

    // 单独的初始化方法，在合适的时机调用
    public void init() {
        if (con == null) {
            con = this.getWritableDatabase();
        }
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        createTables(db);
        insertInitialData(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS d_sheet");
        db.execSQL("DROP TABLE IF EXISTS d_user");
        db.execSQL("DROP TABLE IF EXISTS d_music");
        db.execSQL("DROP TABLE IF EXISTS d_sheet_music");
        db.execSQL("DROP TABLE IF EXISTS d_music_play_list");
        db.execSQL("DROP TABLE IF EXISTS d_music_temp");
        db.execSQL("DROP TABLE IF EXISTS d_user_like_music");
        db.execSQL("DROP TABLE IF EXISTS d_play_record");


        createTables(db);
        insertInitialData(db);
    }

    // 抽取建表逻辑
    private void createTables(SQLiteDatabase db) {
        db.execSQL("PRAGMA foreign_keys = false");

        // 创建用户表
        db.execSQL("create table d_user(" +
                "account VARCHAR(50) PRIMARY KEY NOT NULL," +
                "nickname VARCHAR(50) NOT NULL," +
                "password VARCHAR(50) NOT NULL," +
                "avatar_path VARCHAR(255) DEFAULT ''," +
                "background_path VARCHAR(255) DEFAULT ''," +
                "sex VARCHAR(10) DEFAULT ''," +
                "address VARCHAR(100) DEFAULT ''," +
                "permission VARCHAR(10) DEFAULT '0'," +
                "song_count VARCHAR(20) DEFAULT '0'," +
                "listening_time VARCHAR(100) DEFAULT '0'," +
                "registration_time DATETIME DEFAULT CURRENT_TIMESTAMP" +
                ")");

        // 创建歌单表
        db.execSQL("CREATE TABLE d_sheet (" +
                "id VARCHAR(50) PRIMARY KEY," +
                "name VARCHAR(50) NOT NULL," +
                "description VARCHAR(500) DEFAULT ''," +
                "cover_image VARCHAR(255) DEFAULT ''," +
                "owner_id VARCHAR(50) NOT NULL," +
                "song_count VARCHAR(20) DEFAULT '0'," +
                "play_count VARCHAR(20) DEFAULT '0'," +
                "collect_count VARCHAR(20) DEFAULT '0'," +
                "is_public VARCHAR(10) DEFAULT '1'," +
                "create_time DATETIME DEFAULT CURRENT_TIMESTAMP," +
                "update_time DATETIME DEFAULT CURRENT_TIMESTAMP" +
                ")");

        // 创建触发器
        db.execSQL("CREATE TRIGGER update_sheet_time " +
                "AFTER UPDATE ON d_sheet " +
                "FOR EACH ROW " +
                "BEGIN " +
                "UPDATE d_sheet SET update_time = datetime('now','localtime') WHERE id = OLD.id; " +
                "END");

        db.execSQL("PRAGMA foreign_keys = true");

        db.execSQL("CREATE TABLE d_music (" +
                "id VARCHAR(50) PRIMARY KEY," +
                "name VARCHAR(50) NOT NULL," +
                "album VARCHAR(100)," +
                "singer VARCHAR(100)," +
                "path VARCHAR(255)," +
                "lyric_path VARCHAR(255),"+
                "create_time DATETIME DEFAULT CURRENT_TIMESTAMP" +
                ")");

        db.execSQL("CREATE TABLE d_sheet_music (" +
                "id VARCHAR(50) PRIMARY KEY," +
                "sheet_id VARCHAR(50) NOT NULL," +
                "music_id VARCHAR(50) NOT NULL," +
                "sort_order VARCHAR(20) DEFAULT '0'," +
                "add_time DATETIME DEFAULT CURRENT_TIMESTAMP," +
                "FOREIGN KEY (sheet_id) REFERENCES d_sheet(id) ON DELETE CASCADE," +
                "FOREIGN KEY (music_id) REFERENCES d_music(id) ON DELETE CASCADE," +
                "UNIQUE (sheet_id, music_id)" +
                ")");

        db.execSQL("CREATE INDEX idx_sheet_music_sheet ON d_sheet_music(sheet_id)");
        db.execSQL("CREATE INDEX idx_sheet_music_music ON d_sheet_music(music_id)");


        db.execSQL("CREATE TABLE d_music_play_list (" +
                "id VARCHAR(50) PRIMARY KEY," +
                "user_id VARCHAR(50) NOT NULL," +
                "music_id VARCHAR(50)," +
                "is_playing VARCHAR(50) DEFAULT '0'," +
                "add_time DATETIME DEFAULT CURRENT_TIMESTAMP," +
                "FOREIGN KEY (user_id) REFERENCES d_user(account) ON DELETE CASCADE," +
                "FOREIGN KEY (music_id) REFERENCES d_music(id) ON DELETE CASCADE" +
                ")");

// ✅ 添加唯一索引，防止重复插入
        db.execSQL("CREATE UNIQUE INDEX idx_play_list_unique ON d_music_play_list(user_id, music_id)");

        db.execSQL("CREATE TABLE d_user_like_music (" +
                "id VARCHAR(50) PRIMARY KEY," +
                "user_id VARCHAR(50) NOT NULL," +
                "music_id VARCHAR(50)," +
                "FOREIGN KEY (user_id) REFERENCES d_user(account) ON DELETE CASCADE," +
                "FOREIGN KEY (music_id) REFERENCES d_music(id) ON DELETE CASCADE" +
                ")");


        db.execSQL("CREATE UNIQUE INDEX idx_user_music ON d_user_like_music(user_id, music_id)");


        // 播放记录表
        db.execSQL("CREATE TABLE d_play_record (" +
                "id VARCHAR(50) PRIMARY KEY," +
                "user_id VARCHAR(50) NOT NULL," +
                "music_id VARCHAR(50) NOT NULL," +
                "played_at DATETIME DEFAULT CURRENT_TIMESTAMP," +
                "play_duration_seconds INTEGER DEFAULT 0," +
                "is_completed VARCHAR(10) DEFAULT '0'," +
                "FOREIGN KEY (user_id) REFERENCES d_user(account) ON DELETE CASCADE," +
                "FOREIGN KEY (music_id) REFERENCES d_music(id) ON DELETE CASCADE" +
                ")");

// 索引：按用户查询播放记录
        db.execSQL("CREATE INDEX idx_play_record_user ON d_play_record(user_id)");
// 索引：按时间排序
        db.execSQL("CREATE INDEX idx_play_record_time ON d_play_record(played_at)");


    }

    // 抽取数据插入逻辑，使用 try-catch 保护
    private void insertInitialData(SQLiteDatabase db) {
        try {
            // 插入用户数据
            db.execSQL("insert into d_user (account,nickname,password,permission,address,sex) values(?,?,?,?,?,?)",
                    new String[]{"admin", "小希", "123456", "0", "北京", "女"});
            db.execSQL("insert into d_user (account,nickname,password,permission,address,sex) values(?,?,?,?,?,?)",
                    new String[]{"root", "绳匠123456", "123456", "1", "上海", "男"});

            if (appContext != null) {
                // 同步保存封面图片
                String coverPath = FileUtil.saveImageSync(appContext, R.drawable.icon_default_cover_image);
                if (coverPath == null) coverPath = "";

                db.execSQL("INSERT INTO d_sheet (id,name,description,cover_image,owner_id,song_count,play_count,collect_count,is_public) VALUES(?,?,?,?,?,?,?,?,?)", new String[]{"1", "默认歌单", "系统默认歌单", coverPath, "admin", "0", "0", "0", "1"});
                db.execSQL("INSERT INTO d_sheet (id,name,description,cover_image,owner_id,song_count,play_count,collect_count,is_public) VALUES(?,?,?,?,?,?,?,?,?)", new String[]{"2", "我的歌单", "我喜欢的音乐", coverPath, "root", "12", "88", "5", "1"});
                db.execSQL("INSERT INTO d_sheet (id,name,description,cover_image,owner_id,song_count,play_count,collect_count,is_public) VALUES(?,?,?,?,?,?,?,?,?)", new String[]{"3", "摇滚", "燃烧你的灵魂", coverPath, "root", "25", "320", "48", "1"});
                db.execSQL("INSERT INTO d_sheet (id,name,description,cover_image,owner_id,song_count,play_count,collect_count,is_public) VALUES(?,?,?,?,?,?,?,?,?)", new String[]{"4", "流行", "当下最火热单曲", coverPath, "root", "50", "1200", "230", "1"});
                db.execSQL("INSERT INTO d_sheet (id,name,description,cover_image,owner_id,song_count,play_count,collect_count,is_public) VALUES(?,?,?,?,?,?,?,?,?)", new String[]{"5", "古典", "穿越时空的旋律", coverPath, "root", "30", "450", "89", "1"});




                // 只需修改这个数字即可
                int musicCount = 5;

                for (int i = 0; i < musicCount; i++) {
                    // 动态获取资源ID: music_01, music_02, ...
                    String resourceName = String.format("music_%02d", i + 1);
                    int resId = appContext.getResources().getIdentifier(resourceName, "raw", appContext.getPackageName());

                    if (resId == 0) continue; // 资源不存在则跳过

                    String musicPath = MusicFileUtil.saveMusicSync(appContext, resId);

                    if (musicPath != null && !musicPath.isEmpty()) {
                        MusicMetadataUtil.MusicInfo info = MusicMetadataUtil.getMusicInfo(musicPath);

                        String title = (info != null && info.getTitle() != null) ? info.getTitle() : "";
                        String artist = (info != null && info.getArtist() != null) ? info.getArtist() : "";
                        String album = (info != null && info.getAlbum() != null) ? info.getAlbum() : "";

                        int id = i + 1;
                        db.execSQL("INSERT INTO d_music (id, name, singer, album, path) VALUES (?, ?, ?, ?, ?)",
                                new String[]{String.valueOf(id), title, artist, album, musicPath});

                        db.execSQL("INSERT INTO d_sheet_music (id, sheet_id, music_id) VALUES (?, ?, ?)",
                                new String[]{String.valueOf(id), "2", String.valueOf(id)});

                        // 动态获取歌词资源ID: lyric_01, lyric_02, ...
                        String lyricResourceName = String.format("lyric_%02d", i + 1);
                        int lyricResId = appContext.getResources().getIdentifier(lyricResourceName, "raw", appContext.getPackageName());

                        if (lyricResId != 0) {
                            String lyricPath = MusicFileUtil.saveLrcSync(appContext, lyricResId, title);
                            if (lyricPath != null && !lyricPath.isEmpty()) {
                                db.execSQL("UPDATE d_music SET lyric_path = ? WHERE id = ?",
                                        new String[]{lyricPath, String.valueOf(id)});
                            }
                        }
                    }
                }







                db.execSQL("insert into d_sheet_music (id,sheet_id,music_id) values(?,?,?)", new String[]{String.valueOf(musicCount+1), "4", "5"});


                //db.execSQL("insert into d_music_play_list (id,music_id,user_id) values(?,?,?)", new String[]{"1", "1", "admin"});
                db.execSQL("insert into d_user_like_music (id,user_id,music_id) values(?,?,?)", new String[]{"1", "root", "3"});



            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}