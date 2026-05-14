package com.cool.music.bean;

import java.io.Serializable;
import java.util.Date;

public class MusicBean implements Serializable {
    private String id;
    private String name;
    private String picture;
    private String singer;
    private String path;
    private String lyric_path;
    private String create_time;

    public MusicBean() {
    }

    public MusicBean(String id, String name, String picture, String singer, String path, String lyric_path, String create_time) {
        this.id = id;
        this.name = name;
        this.picture = picture;
        this.singer = singer;
        this.path = path;
        this.lyric_path = lyric_path;
        this.create_time = create_time;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPicture() {
        return picture;
    }

    public void setPicture(String picture) {
        this.picture = picture;
    }

    public String getSinger() {
        return singer;
    }

    public void setSinger(String singer) {
        this.singer = singer;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getLyric_path() {
        return lyric_path;
    }

    public void setLyric_path(String lyric_path) {
        this.lyric_path = lyric_path;
    }

    public String getCreate_time() {
        return create_time;
    }

    public void setCreate_time(String create_time) {
        this.create_time = create_time;
    }
}