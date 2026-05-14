package com.cool.music.event;

/**
 * 播放控制事件
 */
public class PlayControlEvent {
    public static final int ACTION_PLAY = 1;
    public static final int ACTION_PAUSE = 2;
    public static final int ACTION_TOGGLE = 3;  // 切换播放/暂停
    public static final int ACTION_NEXT = 4;
    public static final int ACTION_PREVIOUS = 5;

    private int action;

    public PlayControlEvent(int action) {
        this.action = action;
    }

    public int getAction() {
        return action;
    }
}