package com.cool.music.view;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.media.audiofx.Visualizer;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.animation.LinearInterpolator;

/**
 * 音频可视化View - 丝滑版
 * 支持三种展示模式：柱状图、波形图、圆形
 * 优化：使用动画插值实现丝滑过渡效果
 */
public class AudioVisualizerView extends View {

    private static final String TAG = "AudioVisualizerView";

    // ==================== 可调参数 ====================
    private int barCount = 32;              // 柱状条数量
    private float sensitivity = 1.1f;       // 灵敏度
    private float smoothFactor = 0.15f;     // 平滑因子 (降低以更平滑)
    private float minBarHeight = 0.02f;     // 最小柱子高度比例
    private boolean useLogScale = true;     // 使用对数刻度

    // ==================== 动画参数 ====================
    private float animationSpeed = 0.1f;   // 动画速度 (0.05-0.3，越小越丝滑)
    private float springDamping = 0.7f;     // 弹性阻尼 (0.5-0.9)
    private float velocityDecay = 0.92f;    // 速度衰减 (0.85-0.95)
    private static final int ANIMATION_FPS = 120;  // 动画帧率
    // ==================================================

    private Paint paint;
    private Paint wavePaint;
    private Visualizer visualizer;
    private byte[] waveformData;
    private byte[] fftData;

    // 动画相关数组
    private float[] targetHeights;      // 目标高度
    private float[] currentHeights;     // 当前显示高度
    private float[] velocities;         // 速度（用于弹性动画）
    private float[] smoothBarHeights;   // FFT平滑处理后的高度

    private int visualizerColor = Color.WHITE;
    private VisualizerType type = VisualizerType.BAR;
    private boolean isLinked = false;

    // 动画器
    private ValueAnimator animator;
    private boolean isAnimating = false;

    public enum VisualizerType {
        WAVEFORM,  // 波形图
        BAR,       // 柱状图
        CIRCLE     // 圆形可视化
    }

    public AudioVisualizerView(Context context) {
        super(context);
        init();
    }

    public AudioVisualizerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public AudioVisualizerView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(visualizerColor);
        paint.setStyle(Paint.Style.FILL);

        wavePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        wavePaint.setColor(visualizerColor);
        wavePaint.setStrokeWidth(3f);
        wavePaint.setStyle(Paint.Style.STROKE);

        initBarArrays();
        setupAnimator();
    }

    private void initBarArrays() {
        targetHeights = new float[barCount];
        currentHeights = new float[barCount];
        velocities = new float[barCount];
        smoothBarHeights = new float[barCount];
    }

    /**
     * 设置动画器 - 核心丝滑动画实现
     */
    private void setupAnimator() {
        animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(1000 / ANIMATION_FPS);
        animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.setInterpolator(new LinearInterpolator());

        animator.addUpdateListener(animation -> {
            updateAnimation();
            invalidate();
        });
    }

    /**
     * 更新动画 - 使用弹性插值实现丝滑效果
     */
    private void updateAnimation() {
        boolean needsUpdate = false;

        for (int i = 0; i < barCount; i++) {
            float target = targetHeights[i];
            float current = currentHeights[i];
            float velocity = velocities[i];

            // 计算到目标的距离
            float distance = target - current;

            // 弹性动画：施加弹力
            float springForce = distance * animationSpeed;

            // 更新速度（带阻尼）
            velocity = (velocity + springForce) * velocityDecay;

            // 限制最大速度，防止抖动
            velocity = Math.max(-0.15f, Math.min(0.15f, velocity));

            // 更新当前位置
            current += velocity;

            // 接近目标时直接吸附，避免无限震荡
            if (Math.abs(distance) < 0.001f && Math.abs(velocity) < 0.001f) {
                current = target;
                velocity = 0;
            } else {
                needsUpdate = true;
            }

            // 确保在有效范围内
            currentHeights[i] = Math.max(minBarHeight, Math.min(1f, current));
            velocities[i] = velocity;
        }

        // 如果没有连接且不需要更新，可以降低刷新率
        if (!isLinked && !needsUpdate) {
            // 保持最小高度的静态显示
            for (int i = 0; i < barCount; i++) {
                currentHeights[i] = minBarHeight;
            }
        }
    }

    /**
     * 开始动画
     */
    private void startAnimation() {
        if (!isAnimating && animator != null) {
            animator.start();
            isAnimating = true;
        }
    }

    /**
     * 停止动画
     */
    private void stopAnimation() {
        if (isAnimating && animator != null) {
            animator.cancel();
            isAnimating = false;
        }
    }

    // ==================== 参数设置方法 ====================

    /**
     * 设置柱子数量
     */
    public void setBarCount(int count) {
        this.barCount = Math.max(8, Math.min(128, count));
        initBarArrays();
        invalidate();
    }

    /**
     * 设置灵敏度
     */
    public void setSensitivity(float sensitivity) {
        this.sensitivity = Math.max(0.1f, Math.min(5f, sensitivity));
    }

    /**
     * 设置平滑因子
     */
    public void setSmoothFactor(float factor) {
        this.smoothFactor = Math.max(0.05f, Math.min(0.5f, factor));
    }

    /**
     * 设置最小柱子高度
     */
    public void setMinBarHeight(float minHeight) {
        this.minBarHeight = Math.max(0f, Math.min(0.3f, minHeight));
    }

    /**
     * 设置动画速度 (越小越丝滑，推荐 0.08-0.15)
     */
    public void setAnimationSpeed(float speed) {
        this.animationSpeed = Math.max(0.03f, Math.min(0.3f, speed));
    }

    /**
     * 设置弹性阻尼 (越大弹性越小，推荐 0.6-0.85)
     */
    public void setSpringDamping(float damping) {
        this.springDamping = Math.max(0.5f, Math.min(0.95f, damping));
    }

    /**
     * 是否使用对数刻度
     */
    public void setUseLogScale(boolean useLog) {
        this.useLogScale = useLog;
    }

    /**
     * 设置可视化颜色
     */
    public void setColor(int color) {
        this.visualizerColor = color;
        paint.setColor(color);
        wavePaint.setColor(color);
        invalidate();
    }

    /**
     * 设置可视化类型
     */
    public void setVisualizerType(VisualizerType type) {
        this.type = type;
        invalidate();
    }

    // ==================== 核心方法 ====================

    /**
     * 绑定到音频会话
     */
    public void linkToAudioSession(int audioSessionId) {
        if (audioSessionId == 0) {
            Log.w(TAG, "Invalid audioSessionId: 0");
            return;
        }

        release();

        try {
            visualizer = new Visualizer(audioSessionId);

            int[] sizeRange = Visualizer.getCaptureSizeRange();
            int captureSize = sizeRange[1];
            visualizer.setCaptureSize(captureSize);

            Log.d(TAG, "Visualizer capture size: " + captureSize);

            visualizer.setDataCaptureListener(new Visualizer.OnDataCaptureListener() {
                @Override
                public void onWaveFormDataCapture(Visualizer visualizer, byte[] waveform, int samplingRate) {
                    waveformData = waveform.clone();
                    if (type == VisualizerType.WAVEFORM) {
                        postInvalidate();
                    }
                }

                @Override
                public void onFftDataCapture(Visualizer visualizer, byte[] fft, int samplingRate) {
                    fftData = fft.clone();
                    updateTargetHeights();
                }
            }, Visualizer.getMaxCaptureRate(), true, true);

            visualizer.setEnabled(true);
            isLinked = true;
            startAnimation();
            Log.d(TAG, "Visualizer linked successfully");

        } catch (Exception e) {
            Log.e(TAG, "Failed to link visualizer", e);
            isLinked = false;
        }
    }

    /**
     * 更新目标高度 - FFT处理后设置动画目标值
     */
    private void updateTargetHeights() {
        if (fftData == null || fftData.length < 4) return;

        int fftSize = fftData.length / 2;
        int minBin = 1;
        int maxBin = fftSize / 2;
        int availableBins = maxBin - minBin;

        for (int i = 0; i < barCount; i++) {
            float magnitude;

            if (useLogScale) {
                float logMin = (float) Math.log(minBin + 1);
                float logMax = (float) Math.log(maxBin);

                float startLog = logMin + (logMax - logMin) * i / barCount;
                float endLog = logMin + (logMax - logMin) * (i + 1) / barCount;

                int startIndex = (int) Math.exp(startLog);
                int endIndex = (int) Math.exp(endLog);

                startIndex = Math.max(minBin, startIndex);
                endIndex = Math.max(startIndex + 1, endIndex);

                if (i < barCount / 4 && endIndex - startIndex <= 1) {
                    int linearBins = Math.min(barCount / 4, availableBins / 4);
                    startIndex = minBin + i * linearBins / (barCount / 4);
                    endIndex = minBin + (i + 1) * linearBins / (barCount / 4);
                    endIndex = Math.max(startIndex + 1, endIndex);
                }

                magnitude = calculateMagnitudeWithBoost(startIndex, endIndex, i);
            } else {
                int startIndex = minBin + i * (maxBin - minBin) / barCount;
                int endIndex = minBin + (i + 1) * (maxBin - minBin) / barCount;
                magnitude = calculateMagnitudeWithBoost(startIndex, endIndex, i);
            }

            magnitude *= sensitivity;
            magnitude = Math.min(1f, magnitude);

            // 对FFT结果进行初步平滑
            smoothBarHeights[i] = smoothBarHeights[i] * (1 - smoothFactor) + magnitude * smoothFactor;

            // 设置动画目标高度
            targetHeights[i] = Math.max(minBarHeight, smoothBarHeights[i]);
        }
    }

    private float calculateMagnitudeWithBoost(int startIndex, int endIndex, int barIndex) {
        if (fftData == null) return 0;

        float sum = 0;
        int count = 0;

        for (int j = startIndex; j < endIndex && j * 2 + 1 < fftData.length; j++) {
            float real = fftData[j * 2];
            float imag = fftData[j * 2 + 1];
            float mag = (float) Math.hypot(real, imag);
            sum += mag;
            count++;
        }

        if (count == 0) return 0;

        float avg = sum / count;

        // 高频增益补偿
        float boostFactor = 1.0f + (float) barIndex / barCount * 2.0f;
        avg *= boostFactor;

        // 动态范围压缩
        avg = (float) Math.sqrt(avg);

        return avg / 12f;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (!isLinked) {
            drawStaticBars(canvas);
            return;
        }

        switch (type) {
            case WAVEFORM:
                drawWaveform(canvas);
                break;
            case BAR:
                drawBars(canvas);
                break;
            case CIRCLE:
                drawCircle(canvas);
                break;
        }
    }

    /**
     * 绘制静态柱状图
     */
    private void drawStaticBars(Canvas canvas) {
        int width = getWidth();
        int height = getHeight();
        if (width == 0 || height == 0) return;

        float totalBarWidth = width * 0.75f;
        float barWidth = totalBarWidth / barCount;
        float gap = (width - totalBarWidth) / (barCount + 1);

        paint.setAlpha(60);

        for (int i = 0; i < barCount; i++) {
            float left = gap + i * (barWidth + gap);
            float barHeight = height * minBarHeight;

            RectF rect = new RectF(left, height - barHeight, left + barWidth, height);
            canvas.drawRoundRect(rect, barWidth / 2, barWidth / 2, paint);
        }

        paint.setAlpha(255);
    }

    /**
     * 绘制波形图
     */
    private void drawWaveform(Canvas canvas) {
        if (waveformData == null || waveformData.length == 0) return;

        int width = getWidth();
        int height = getHeight();
        float centerY = height / 2f;

        Path path = new Path();
        path.moveTo(0, centerY);

        int step = Math.max(1, waveformData.length / width);

        for (int i = 0; i < waveformData.length; i += step) {
            float x = (float) i / waveformData.length * width;
            float amplitude = ((waveformData[i] & 0xFF) - 128) / 128f;
            float y = centerY - amplitude * centerY * 0.8f * sensitivity;
            path.lineTo(x, y);
        }

        canvas.drawPath(path, wavePaint);
    }

    /**
     * 绘制柱状图 - 使用动画插值后的高度
     */
    private void drawBars(Canvas canvas) {
        int width = getWidth();
        int height = getHeight();
        if (width == 0 || height == 0) return;

        float totalBarWidth = width * 0.75f;
        float barWidth = totalBarWidth / barCount;
        float gap = (width - totalBarWidth) / (barCount + 1);

        for (int i = 0; i < barCount; i++) {
            float left = gap + i * (barWidth + gap);
            // 使用动画插值后的当前高度
            float barHeight = currentHeights[i] * height * 0.95f;
            barHeight = Math.max(barHeight, barWidth);

            float top = height - barHeight;

            RectF rect = new RectF(left, top, left + barWidth, height);
            canvas.drawRoundRect(rect, barWidth / 2, barWidth / 2, paint);
        }
    }

    /**
     * 绘制圆形可视化 - 使用动画插值后的高度
     */
    private void drawCircle(Canvas canvas) {
        int width = getWidth();
        int height = getHeight();
        float centerX = width / 2f;
        float centerY = height / 2f;
        float baseRadius = Math.min(width, height) / 3f;

        Path path = new Path();

        for (int i = 0; i <= barCount; i++) {
            double angle = 2 * Math.PI * i / barCount - Math.PI / 2;

            // 使用动画插值后的当前高度
            float magnitude = currentHeights[i % barCount];
            float radius = baseRadius + magnitude * baseRadius * 0.8f;

            float x = centerX + (float) (radius * Math.cos(angle));
            float y = centerY + (float) (radius * Math.sin(angle));

            if (i == 0) {
                path.moveTo(x, y);
            } else {
                path.lineTo(x, y);
            }
        }
        path.close();

        canvas.drawPath(path, wavePaint);

        // 中心圆
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(2f);
        canvas.drawCircle(centerX, centerY, baseRadius * 0.3f, paint);
        paint.setStyle(Paint.Style.FILL);
    }

    public boolean isLinked() {
        return isLinked;
    }

    public void release() {
        stopAnimation();

        if (visualizer != null) {
            try {
                visualizer.setEnabled(false);
                visualizer.release();
            } catch (Exception e) {
                Log.e(TAG, "Error releasing visualizer", e);
            }
            visualizer = null;
        }
        isLinked = false;

        // 重置数据
        waveformData = null;
        fftData = null;
        if (targetHeights != null) {
            for (int i = 0; i < barCount; i++) {
                targetHeights[i] = 0;
                currentHeights[i] = 0;
                velocities[i] = 0;
                smoothBarHeights[i] = 0;
            }
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (isLinked) {
            startAnimation();
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        release();
    }

    @Override
    protected void onVisibilityChanged(View changedView, int visibility) {
        super.onVisibilityChanged(changedView, visibility);
        if (visibility == VISIBLE && isLinked) {
            startAnimation();
        } else {
            stopAnimation();
        }
    }
}