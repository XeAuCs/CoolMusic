package com.cool.music.event;

/**
 * 拖动进度条事件
 */
public class SeekEvent {
    private int position;

    public SeekEvent(int position) {
        this.position = position;
    }

    public int getPosition() {
        return position;
    }
}