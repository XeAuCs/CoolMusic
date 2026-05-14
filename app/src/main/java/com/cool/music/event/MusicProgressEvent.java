package com.cool.music.event;

/**
 * 播放进度更新事件
 */
public class MusicProgressEvent {
    private int currentPosition;
    private int duration;

    public MusicProgressEvent(int currentPosition, int duration) {
        this.currentPosition = currentPosition;
        this.duration = duration;
    }

    public int getCurrentPosition() {
        return currentPosition;
    }

    public int getDuration() {
        return duration;
    }
}