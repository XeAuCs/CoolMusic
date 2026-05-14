package com.cool.music.event;

import com.cool.music.bean.MusicBean;

/**
 * 音乐播放事件
 */
public class MusicPlayEvent {
    private MusicBean music;
    private boolean isPlaying;

    public MusicPlayEvent(MusicBean music, boolean isPlaying) {
        this.music = music;
        this.isPlaying = isPlaying;
    }

    public MusicBean getMusic() {
        return music;
    }

    public boolean isPlaying() {
        return isPlaying;
    }
}