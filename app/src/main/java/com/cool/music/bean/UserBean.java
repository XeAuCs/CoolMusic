package com.cool.music.bean;

public class UserBean {
    private String account;
    private String nickname;
    private String password;
    private String avatar_path;
    private String background_path;
    private String sex;
    private String address;
    private String permission;
    private String song_count;
    private String listening_time;
    private String registration_time;

    public UserBean() {
    }

    public UserBean(String account, String nickname, String password, String avatar_path, String background_path, String sex, String address, String permission, String song_count, String listening_time, String registration_time) {
        this.account = account;
        this.nickname = nickname;
        this.password = password;
        this.avatar_path = avatar_path;
        this.background_path = background_path;
        this.sex = sex;
        this.address = address;
        this.permission = permission;
        this.song_count = song_count;
        this.listening_time = listening_time;
        this.registration_time = registration_time;
    }

    public String getAccount() {
        return account;
    }

    public void setAccount(String account) {
        this.account = account;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getAvatar_path() {
        return avatar_path;
    }

    public void setAvatar_path(String avatar_path) {
        this.avatar_path = avatar_path;
    }

    public String getBackground_path() {
        return background_path;
    }

    public void setBackground_path(String background_path) {
        this.background_path = background_path;
    }

    public String getSex() {
        return sex;
    }

    public void setSex(String sex) {
        this.sex = sex;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getPermission() {
        return permission;
    }

    public void setPermission(String permission) {
        this.permission = permission;
    }

    public String getSong_count() {
        return song_count;
    }

    public void setSong_count(String song_count) {
        this.song_count = song_count;
    }

    public String getListening_time() {
        return listening_time;
    }

    public void setListening_time(String listening_time) {
        this.listening_time = listening_time;
    }

    public String getRegistration_time() {
        return registration_time;
    }

    public void setRegistration_time(String registration_time) {
        this.registration_time = registration_time;
    }
}