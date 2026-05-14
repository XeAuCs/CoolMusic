package com.cool.music.bean;

import com.cool.music.dao.SheetDao;

public class SheetBean {
    private String id;
    private String name;
    private String description;
    private String cover_image;
    private String owner_id;
    private String song_count;
    private String play_count;
    private String collect_count;
    private String is_public;
    private String create_time;
    private String update_time;

    public SheetBean() {
    }

    public SheetBean(String id, String name, String description, String cover_image, String owner_id, String song_count, String play_count, String collect_count, String is_public, String create_time, String update_time) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.cover_image = cover_image;
        this.owner_id = owner_id;
        this.song_count = song_count;
        this.play_count = play_count;
        this.collect_count = collect_count;
        this.is_public = is_public;
        this.create_time = create_time;
        this.update_time = update_time;
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCover_image() {
        return cover_image;
    }

    public void setCover_image(String cover_image) {
        this.cover_image = cover_image;
    }

    public String getOwner_id() {
        return owner_id;
    }

    public void setOwner_id(String owner_id) {
        this.owner_id = owner_id;
    }

    public String getSong_count() {
        return SheetDao.getSongCount(this.getId());

    }

    public void setSong_count(String song_count) {
        this.song_count = song_count;
    }

    public String getPlay_count() {
        return play_count;
    }

    public void setPlay_count(String play_count) {
        this.play_count = play_count;
    }

    public String getCollect_count() {
        return collect_count;
    }

    public void setCollect_count(String collect_count) {
        this.collect_count = collect_count;
    }

    public String getIs_public() {
        return is_public;
    }

    public void setIs_public(String is_public) {
        this.is_public = is_public;
    }

    public String getCreate_time() {
        return create_time;
    }

    public void setCreate_time(String create_time) {
        this.create_time = create_time;
    }

    public String getUpdate_time() {
        return update_time;
    }

    public void setUpdate_time(String update_time) {
        this.update_time = update_time;
    }
}