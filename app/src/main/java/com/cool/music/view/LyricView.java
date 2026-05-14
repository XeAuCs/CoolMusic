package com.cool.music.view;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

import com.cool.music.lyric.LrcParser;
import com.cool.music.lyric.LrcParser.LrcLine;

import java.util.List;

/**
 * 歌词显示 View
 * 支持自动滚动、手势拖动、高亮当前行
 */
public class LyricView extends View {

    // ==================== 绘制配置 ====================
    private Paint normalPaint;      // 普通歌词画笔
    private Paint highlightPaint;   // 高亮歌词画笔
    private Paint timePaint;        // 时间指示器画笔

    private float lineHeight = 80f;         // 行高
    private float normalTextSize = 40f;     // 普通字号
    private float highlightTextSize = 50f;  // 高亮字号
    private int normalColor = Color.parseColor("#80FFFFFF");
    private int highlightColor = Color.WHITE;

    // ==================== 歌词数据 ====================
    private LrcParser lrcParser;
    private List<LrcLine> lrcLines;
    private int currentLineIndex = -1;

    // ==================== 滚动相关 ====================
    private float scrollY = 0f;             // 当前滚动位置
    private float targetScrollY = 0f;       // 目标滚动位置
    private ValueAnimator scrollAnimator;

    // ==================== 手势相关 ====================
    private GestureDetector gestureDetector;
    private boolean isUserTouching = false;
    private boolean isDragging = false;
    private long lastTouchTime = 0;
    private static final long TOUCH_TIMEOUT = 3000; // 触摸后 3 秒恢复自动滚动

    // ==================== 回调 ====================
    private OnSeekListener onSeekListener;

    public interface OnSeekListener {
        void onSeek(long time);
    }

    public LyricView(Context context) {
        super(context);
        init();
    }

    public LyricView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public LyricView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        lrcParser = new LrcParser();

        // 初始化普通歌词画笔
        normalPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        normalPaint.setColor(normalColor);
        normalPaint.setTextSize(normalTextSize);
        normalPaint.setTextAlign(Paint.Align.CENTER);

        // 初始化高亮歌词画笔
        highlightPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        highlightPaint.setColor(highlightColor);
        highlightPaint.setTextSize(highlightTextSize);
        highlightPaint.setTextAlign(Paint.Align.CENTER);
        highlightPaint.setTypeface(Typeface.DEFAULT_BOLD);

        // 初始化时间指示器画笔
        timePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        timePaint.setColor(Color.parseColor("#60FFFFFF"));
        timePaint.setTextSize(28f);

        // 初始化手势检测
        gestureDetector = new GestureDetector(getContext(), new LyricGestureListener());
    }

    // ==================== 公共方法 ====================

    /**
     * 从文件加载歌词
     */
    public boolean loadLyric(String lrcPath) {
        boolean success = lrcParser.parseFromFile(lrcPath);
        if (success) {
            lrcLines = lrcParser.getLrcLines();
            currentLineIndex = -1;
            scrollY = 0;
            invalidate();
        }
        return success;
    }

    /**
     * 设置歌词字体
     */
    public void setTypeface(Typeface typeface) {
        normalPaint.setTypeface(typeface);
        // 如果需要高亮行也用同样字体（加粗版）
        highlightPaint.setTypeface(Typeface.create(typeface, Typeface.NORMAL));
        invalidate();
    }
    /**
     * 从字符串加载歌词
     */
    public boolean loadLyricFromString(String lrcContent) {
        boolean success = lrcParser.parseFromString(lrcContent);
        if (success) {
            lrcLines = lrcParser.getLrcLines();
            currentLineIndex = -1;
            scrollY = 0;
            invalidate();
        }
        return success;
    }

    /**
     * 根据音乐路径自动查找并加载歌词
     */
    public boolean loadLyricForMusic(String musicPath) {
        String lrcPath = LrcParser.getLrcPathFromMusicPath(musicPath);
        return loadLyric(lrcPath);
    }

    /**
     * 更新播放进度 - 核心同步方法
     * @param currentTime 当前播放时间（毫秒）
     */
    public void updateTime(long currentTime) {
        if (lrcLines == null || lrcLines.isEmpty()) return;

        int newIndex = lrcParser.getCurrentLineIndex(currentTime);

        if (newIndex != currentLineIndex) {
            currentLineIndex = newIndex;

            // 如果用户没有在触摸，则自动滚动
            if (!isUserTouching && !isDragging) {
                scrollToLine(currentLineIndex);
            }

            invalidate();
        }
    }

    /**
     * 滚动到指定行
     */
    private void scrollToLine(int lineIndex) {
        if (lineIndex < 0) {
            targetScrollY = 0;
        } else {
            // 将目标行滚动到视图中央
            targetScrollY = lineIndex * lineHeight;
        }

        startScrollAnimation();
    }

    /**
     * 启动滚动动画
     */
    private void startScrollAnimation() {
        if (scrollAnimator != null && scrollAnimator.isRunning()) {
            scrollAnimator.cancel();
        }

        scrollAnimator = ValueAnimator.ofFloat(scrollY, targetScrollY);
        scrollAnimator.setDuration(300);
        scrollAnimator.setInterpolator(new DecelerateInterpolator());
        scrollAnimator.addUpdateListener(animation -> {
            scrollY = (float) animation.getAnimatedValue();
            invalidate();
        });
        scrollAnimator.start();
    }

    /**
     * 设置跳转监听器
     */
    public void setOnSeekListener(OnSeekListener listener) {
        this.onSeekListener = listener;
    }

    /**
     * 清空歌词
     */
    public void clear() {
        lrcParser.clear();
        lrcLines = null;
        currentLineIndex = -1;
        scrollY = 0;
        invalidate();
    }

    /**
     * 是否有歌词
     */
    public boolean hasLyrics() {
        return lrcLines != null && !lrcLines.isEmpty();
    }

    // ==================== 绘制 ====================

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (lrcLines == null || lrcLines.isEmpty()) {
            // 绘制无歌词提示
            drawNoLyricHint(canvas);
            return;
        }

        int centerX = getWidth() / 2;
        int centerY = getHeight() / 2;

        // 绘制每一行歌词
        for (int i = 0; i < lrcLines.size(); i++) {
            float y = centerY + (i * lineHeight) - scrollY;

            // 只绘制可见区域的歌词
            if (y < -lineHeight || y > getHeight() + lineHeight) {
                continue;
            }

            LrcLine line = lrcLines.get(i);
            Paint paint;

            if (i == currentLineIndex) {
                paint = highlightPaint;
                // 高亮行可以添加缩放效果
                float scale = 1.0f + 0.1f * (1 - Math.abs(centerY - y) / centerY);
                canvas.save();
                canvas.scale(scale, scale, centerX, y);
                canvas.drawText(line.getText(), centerX, y, paint);
                canvas.restore();
            } else {
                paint = normalPaint;
                // 根据距离中心的距离调整透明度
                float distance = Math.abs(y - centerY);
                float alpha = Math.max(0.3f, 1 - distance / (getHeight() / 2f));
                paint.setAlpha((int) (alpha * 128));
                canvas.drawText(line.getText(), centerX, y, paint);
            }
        }

        // 如果正在拖动，显示时间指示器
        if (isDragging) {
            drawTimeIndicator(canvas);
        }
    }

    private void drawNoLyricHint(Canvas canvas) {
        normalPaint.setAlpha(128);
        canvas.drawText("", getWidth() / 2f, getHeight() / 2f, normalPaint);
    }

    private void drawTimeIndicator(Canvas canvas) {
        // 计算当前滚动位置对应的歌词行
        int lineIndex = (int) (scrollY / lineHeight);
        if (lineIndex >= 0 && lineIndex < lrcLines.size()) {
            long time = lrcLines.get(lineIndex).getTime();
            String timeStr = formatTime(time);

            int centerY = getHeight() / 2;

            // 绘制时间线
            timePaint.setAlpha(150);
            canvas.drawLine(20, centerY, getWidth() - 20, centerY, timePaint);

            // 绘制时间文字
            canvas.drawText(timeStr, 60, centerY - 10, timePaint);
        }
    }

    private String formatTime(long millis) {
        int seconds = (int) (millis / 1000);
        int minutes = seconds / 60;
        seconds = seconds % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }

    // ==================== 触摸事件 ====================

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        boolean handled = gestureDetector.onTouchEvent(event);

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                isUserTouching = true;
                if (scrollAnimator != null) {
                    scrollAnimator.cancel();
                }
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                isUserTouching = false;
                lastTouchTime = System.currentTimeMillis();

                if (isDragging && onSeekListener != null) {
                    // 用户拖动结束，触发跳转
                    int lineIndex = (int) (scrollY / lineHeight);
                    if (lineIndex >= 0 && lineIndex < lrcLines.size()) {
                        onSeekListener.onSeek(lrcLines.get(lineIndex).getTime());
                    }
                }

                isDragging = false;
                invalidate();

                // 延迟后恢复自动滚动
                postDelayed(() -> {
                    if (System.currentTimeMillis() - lastTouchTime >= TOUCH_TIMEOUT) {
                        scrollToLine(currentLineIndex);
                    }
                }, TOUCH_TIMEOUT);
                break;
        }

        return handled || super.onTouchEvent(event);
    }

    private class LyricGestureListener extends GestureDetector.SimpleOnGestureListener {

        @Override
        public boolean onDown(MotionEvent e) {
            return true;
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2,
                                float distanceX, float distanceY) {
            isDragging = true;

            // 更新滚动位置
            scrollY += distanceY;

            // 限制滚动范围
            if (lrcLines != null && !lrcLines.isEmpty()) {
                float maxScroll = (lrcLines.size() - 1) * lineHeight;
                scrollY = Math.max(-getHeight() / 4f, Math.min(scrollY, maxScroll + getHeight() / 4f));
            }

            invalidate();
            return true;
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2,
                               float velocityX, float velocityY) {
            // 可以添加惯性滚动效果
            return super.onFling(e1, e2, velocityX, velocityY);
        }

        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {
            if (lrcLines == null || onSeekListener == null) {
                return true;
            }

            float x = e.getX();
            float y = e.getY();
            int centerX = getWidth() / 2;
            int centerY = getHeight() / 2;

            // 计算点击位置对应的歌词行索引
            float clickedScrollY = scrollY + (y - centerY);
            int lineIndex = Math.round(clickedScrollY / lineHeight);

            if (lineIndex >= 0 && lineIndex < lrcLines.size()) {
                LrcLine line = lrcLines.get(lineIndex);

                // 选择对应的画笔来测量文字宽度
                Paint paint = (lineIndex == currentLineIndex) ? highlightPaint : normalPaint;
                float textWidth = paint.measureText(line.getText());

                // 计算文字左右边界（文字居中绘制）
                float textLeft = centerX - textWidth / 2;
                float textRight = centerX + textWidth / 2;

                // 计算该行的 Y 坐标范围
                float lineY = centerY + (lineIndex * lineHeight) - scrollY;
                float lineTop = lineY - lineHeight / 2;
                float lineBottom = lineY + lineHeight / 2;

                // 检测点击是否在文字范围内
                if (x >= textLeft && x <= textRight && y >= lineTop && y <= lineBottom) {
                    onSeekListener.onSeek(line.getTime());
                }
            }
            return true;
        }
    }

    // ==================== 配置方法 ====================

    public void setNormalColor(int color) {
        this.normalColor = color;
        normalPaint.setColor(color);
        invalidate();
    }

    public void setHighlightColor(int color) {
        this.highlightColor = color;
        highlightPaint.setColor(color);
        invalidate();
    }

    public void setLineHeight(float height) {
        this.lineHeight = height;
        invalidate();
    }

    public void setTextSize(float normalSize, float highlightSize) {
        this.normalTextSize = normalSize;
        this.highlightTextSize = highlightSize;
        normalPaint.setTextSize(normalSize);
        highlightPaint.setTextSize(highlightSize);
        invalidate();
    }
}