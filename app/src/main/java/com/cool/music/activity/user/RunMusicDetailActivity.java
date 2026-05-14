package com.cool.music.activity.user;

import static com.cool.music.util.ThemeColorExtractor.extractThemeColors;
import static com.cool.music.util.VinylRecordGenerator.generateVinylRecord;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;

import com.cool.music.R;
import com.cool.music.bean.MusicBean;
import com.cool.music.dao.MusicDao;
import com.cool.music.event.MusicPlayEvent;
import com.cool.music.event.MusicProgressEvent;
import com.cool.music.event.PlayControlEvent;
import com.cool.music.event.SeekEvent;
import com.cool.music.lyric.LrcParser;
import com.cool.music.player.MusicPlayerManager;
import com.cool.music.util.MusicMetadataUtil;
import com.cool.music.util.ThemeColorExtractor;
import com.cool.music.util.Tools;
import com.cool.music.view.AudioVisualizerView;
import com.cool.music.view.LyricView;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Random;

public class RunMusicDetailActivity extends AppCompatActivity {

    private ImageView ivMusicCover;
    private ImageView ivPlayPause;
    private ImageView ivPrevious;
    private ImageView ivNext;
    private SeekBar seekBar;
    private TextView tvCurrentTime;
    private TextView tvTotalTime;
    private TextView tvMusicTitle;
    private TextView tvMusicArtist;
    private ImageView ivLike;
    private View mainBackground;

    private CardView cardMusicCover;

    private LinearLayout controlArea;

    private ObjectAnimator rotateAnimator;
    private boolean isUserSeeking = false;
    private String musicId;
    private String musicPath;

    private boolean isLiked = false;

    // 保存当前背景颜色用于渐变
    private int currentBgColor1 = Color.BLACK;
    private int currentBgColor2 = Color.BLACK;

    // 动画时长
    private static final int TRANSITION_DURATION = 500;

    private Handler autoHideHandler = new Handler(Looper.getMainLooper());
    private boolean isContentAreaVisible = true;
    private static final int AUTO_HIDE_DELAY = 10000; // 10秒

    // ==================== 音频可视化相关 ====================
    private AudioVisualizerView audioVisualizer;
    private static final int PERMISSION_REQUEST_RECORD_AUDIO = 100;
    private boolean visualizerSetupPending = false;



    private LyricView lyricView;
    private LrcParser lrcParser;
    // ========================================================

    private Runnable autoHideRunnable = new Runnable() {
        @Override
        public void run() {
            hideContentArea();
        }
    };

    // 隐藏动画
    private void hideContentArea() {
        if (!isContentAreaVisible || controlArea == null) return;

        controlArea.animate()
                .translationY(controlArea.getHeight())
                .alpha(0f)
                .setDuration(1000)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .withEndAction(() -> isContentAreaVisible = false)
                .start();
    }

    private void showContentArea() {
        if (isContentAreaVisible || controlArea == null) return;

        controlArea.animate()
                .translationY(0)
                .alpha(1f)
                .setDuration(500)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .withEndAction(() -> isContentAreaVisible = true)
                .start();

        resetAutoHideTimer();
    }

    private void resetAutoHideTimer() {
        autoHideHandler.removeCallbacks(autoHideRunnable);
        autoHideHandler.postDelayed(autoHideRunnable, AUTO_HIDE_DELAY);
    }

    private double getLuminance(String hexColor) {
        String hex = hexColor.startsWith("#") ? hexColor.substring(1) : hexColor;

        int r = Integer.parseInt(hex.substring(0, 2), 16);
        int g = Integer.parseInt(hex.substring(2, 4), 16);
        int b = Integer.parseInt(hex.substring(4, 6), 16);

        return 0.299 * r + 0.587 * g + 0.114 * b;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        getWindow().setFormat(PixelFormat.RGBA_8888);
        setContentView(R.layout.activity_run_music_detail);

        musicPath = getIntent().getStringExtra("musicPath");
        musicId = getIntent().getStringExtra("musicId");
        float initialRotation = getIntent().getFloatExtra("rotation", 0f);
        if (musicPath == null || musicId == null) {
            finish();
            return;
        }

        // ==================== 初始化音频可视化 ====================
        audioVisualizer = findViewById(R.id.audio_visualizer);
        if (audioVisualizer != null) {
            audioVisualizer.setColor(Color.WHITE);
            audioVisualizer.setVisualizerType(AudioVisualizerView.VisualizerType.BAR);
        }
        // =========================================================

        MusicMetadataUtil.MusicInfo info = MusicMetadataUtil.getMusicInfo(musicPath);
        String[] colors = extractThemeColors(info.getCoverBitmap(), 20);

        // ===== 临时测试代码，跑完数据后删掉 =====
        Bitmap testBitmap = info.getCoverBitmap();
        new Thread(() -> {
            // 测试原始尺寸
            ThemeColorExtractor.BenchmarkResult r1 =
                    ThemeColorExtractor.benchmark(testBitmap, 5, 10);

            // 测试放大到 1000×1000
            Bitmap m = Bitmap.createScaledBitmap(testBitmap, 1000, 1000, true);
            ThemeColorExtractor.BenchmarkResult r2 =
                    ThemeColorExtractor.benchmark(m, 5, 10);

            // 测试放大到 2000×2000
            Bitmap l = Bitmap.createScaledBitmap(testBitmap, 2000, 2000, true);
            ThemeColorExtractor.BenchmarkResult r3 =
                    ThemeColorExtractor.benchmark(l, 5, 10);

            android.util.Log.i("BENCHMARK", "原图(" + testBitmap.getWidth() + "×" + testBitmap.getHeight() + "): " + r1);
            android.util.Log.i("BENCHMARK", "1000px: " + r2);
            android.util.Log.i("BENCHMARK", "2000px: " + r3);
        }).start();
// ===== 测试代码结束 =====

        // 按亮度排序
        Arrays.sort(colors, Comparator.comparingDouble(this::getLuminance));

        // 初始化控件引用
        mainBackground = findViewById(R.id.main_run_music);
        ivMusicCover = findViewById(R.id.iv_music_cover);
        tvMusicTitle = findViewById(R.id.tv_music_title);
        tvMusicArtist = findViewById(R.id.tv_artist);
        cardMusicCover = findViewById(R.id.card_music_cover);
        ivLike = findViewById(R.id.iv_like);
        controlArea = findViewById(R.id.bottom_panel);

        // 方案1: 最深色 + 最浅色（最大对比）
        currentBgColor1 = Color.parseColor(colors[0]);
        currentBgColor2 = Color.parseColor(colors[colors.length - 1]);

        // 唱片使用中等亮度的颜色
        String vinylColor = colors[colors.length / 2];
        ivMusicCover.setImageBitmap(generateVinylRecord(info.getCoverBitmap(), vinylColor, 2000));

        int[] gradientColors = {currentBgColor1, currentBgColor2};
        mainBackground.setBackground(
                new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, gradientColors)
        );

        tvMusicTitle.setText(info.getTitle());
        tvMusicArtist.setText(info.getArtist());

        // ========== 初始化播放控件 ==========
        initPlayerControls();

        // ========== 开始播放当前歌曲 ==========
        startPlayingCurrentMusic();
        initRotateAnimator();

        // ========== 喜欢按钮 ==========
        setupLikeButton();

        resetAutoHideTimer();

        // 点击屏幕任意位置时显示/重置计时
        mainBackground.setOnClickListener(v -> {
            if (!isContentAreaVisible) {
                showContentArea();
            } else {
                resetAutoHideTimer();
            }
        });

        // ==================== 请求音频权限并设置可视化 ====================
        requestAudioPermissionAndSetupVisualizer();
        // ================================================================

        initLyricView();
    }

    private void initLyricView() {
        lyricView = findViewById(R.id.lyric_view);
        lrcParser = new LrcParser();

        if (lyricView != null) {
            // 设置歌词点击跳转监听
            lyricView.setOnSeekListener(time -> {
                // 发送跳转事件
                EventBus.getDefault().post(new SeekEvent((int) time));
            });

            // 加载当前歌曲的歌词
            loadLyricForCurrentMusic();
        }
    }
    // ==================== 音频可视化权限和设置方法 ====================

    private void loadLyricForCurrentMusic() {
        if (lyricView == null || musicPath == null) return;



        boolean loaded = lyricView.loadLyric(MusicDao.findMusicById(musicId).getLyric_path());
        Typeface customFont = ResourcesCompat.getFont(this, R.font.genshin_zh_cn);
        lyricView.setTypeface(customFont);
        if (!loaded) {
            // 尝试其他路径或显示无歌词
            lyricView.clear();
        }
    }
    /**
     * 请求录音权限（Visualizer需要此权限）
     */
    private void requestAudioPermissionAndSetupVisualizer() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            // 需要请求权限
            visualizerSetupPending = true;
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.RECORD_AUDIO},
                    PERMISSION_REQUEST_RECORD_AUDIO);
        } else {
            // 已有权限，直接设置
            setupVisualizer();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSION_REQUEST_RECORD_AUDIO) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // 权限授予成功
                setupVisualizer();
            } else {
                // 权限被拒绝，可视化功能将不可用
                // 可以选择隐藏可视化View或显示提示
                if (audioVisualizer != null) {
                    audioVisualizer.setVisibility(View.GONE);
                }
            }
            visualizerSetupPending = false;
        }
    }

    /**
     * 设置音频可视化
     * 从ExoPlayer获取audioSessionId并绑定到Visualizer
     */
    private void setupVisualizer() {
        if (audioVisualizer == null) return;

        // 延迟执行，确保播放器已经准备好
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            try {
                // 从 MusicPlayerManager 获取 ExoPlayer
                MusicPlayerManager manager = MusicPlayerManager.getInstance();
                if (manager.getPlayer() != null) {
                    int audioSessionId = manager.getPlayer().getAudioSessionId();

                    if (audioSessionId != 0 && audioSessionId != -1) {
                        audioVisualizer.linkToAudioSession(audioSessionId);

                        // 根据背景色设置可视化颜色（使用较亮的颜色以便可见）
                        audioVisualizer.setColor(Color.argb(180, 255, 255, 255));

                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, 500); // 延迟500ms等待播放器准备
    }

    /**
     * 更新可视化颜色（可在切换歌曲时调用）
     */
    private void updateVisualizerColor(int color) {
        if (audioVisualizer != null) {
            // 使用半透明白色或根据背景色计算对比色
            audioVisualizer.setColor(Color.argb(180,
                    Color.red(color), Color.green(color), Color.blue(color)));
        }
    }

    // ================================================================

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (ev.getAction() == MotionEvent.ACTION_DOWN) {
            if (!isContentAreaVisible) {
                showContentArea();
            } else {
                resetAutoHideTimer();
            }
        }
        return super.dispatchTouchEvent(ev);
    }

    private void initPlayerControls() {
        ivPlayPause = findViewById(R.id.user_iv_play);
        ivPrevious = findViewById(R.id.user_iv_previous);
        ivNext = findViewById(R.id.user_iv_next);
        seekBar = findViewById(R.id.user_seekbar_progress);
        tvCurrentTime = findViewById(R.id.user_tv_current_time);
        tvTotalTime = findViewById(R.id.user_tv_total_time);

        if (ivPlayPause != null) {
            ivPlayPause.setOnClickListener(v -> {
                EventBus.getDefault().post(new PlayControlEvent(PlayControlEvent.ACTION_TOGGLE));
            });
        }

        if (ivPrevious != null) {
            ivPrevious.setOnClickListener(v -> {
                EventBus.getDefault().post(new PlayControlEvent(PlayControlEvent.ACTION_PREVIOUS));
            });
        }

        if (ivNext != null) {
            ivNext.setOnClickListener(v -> {
                EventBus.getDefault().post(new PlayControlEvent(PlayControlEvent.ACTION_NEXT));
            });
        }

        if (seekBar != null) {
            seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    if (fromUser && tvCurrentTime != null) {
                        tvCurrentTime.setText(formatTime(progress));
                    }
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {
                    isUserSeeking = true;
                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                    EventBus.getDefault().post(new SeekEvent(seekBar.getProgress()));
                    seekBar.postDelayed(() -> isUserSeeking = false, 100);
                }
            });
        }
    }

    private void initRotateAnimator() {
        float initialRotation = getIntent().getFloatExtra("rotation", 0f);

        ivMusicCover.setRotation(initialRotation);

        rotateAnimator = ObjectAnimator.ofFloat(ivMusicCover, "rotation",
                initialRotation, initialRotation - 360f);
        rotateAnimator.setDuration(12000);
        rotateAnimator.setRepeatCount(ValueAnimator.INFINITE);
        rotateAnimator.setRepeatMode(ValueAnimator.RESTART);
        rotateAnimator.setInterpolator(new LinearInterpolator());
    }

    private void startRotation() {
        if (rotateAnimator != null) {
            if (rotateAnimator.isPaused()) {
                rotateAnimator.resume();
            } else if (!rotateAnimator.isRunning()) {
                rotateAnimator.start();
            }
        }
    }

    private void pauseRotation() {
        if (rotateAnimator != null && rotateAnimator.isRunning()) {
            rotateAnimator.pause();
        }
    }

    private void stopRotation() {
        if (rotateAnimator != null) {
            rotateAnimator.cancel();
            ivMusicCover.setRotation(0f);
        }
    }

    private void startPlayingCurrentMusic() {
        MusicBean music = new MusicBean();
        music.setId(musicId);
        music.setPath(musicPath);
        MusicPlayerManager.getInstance().playSingle(music);
    }

    private void setupLikeButton() {
        isLiked = MusicDao.isLikedMusic(Tools.getOnAccount(this), musicId);
        ivLike.setImageResource(isLiked ? R.drawable.icon_like : R.drawable.icon_dislike);

        ivLike.setOnClickListener(v -> {
            if (isLiked) {
                ivLike.setImageResource(R.drawable.icon_dislike);
                MusicDao.setLikedMusic(Tools.getOnAccount(this), musicId, false);
                isLiked = false;
            } else {
                ivLike.setImageResource(R.drawable.icon_like);
                MusicDao.setLikedMusic(Tools.getOnAccount(this), musicId, true);
                isLiked = true;
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);

        if (MusicPlayerManager.getInstance().isPlaying()) {
            startRotation();
            if (ivPlayPause != null) {
                ivPlayPause.setImageResource(R.drawable.ic_play_pause);
            }
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        EventBus.getDefault().unregister(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        autoHideHandler.removeCallbacks(autoHideRunnable);

        // ==================== 释放音频可视化资源 ====================
        if (audioVisualizer != null) {
            audioVisualizer.release();
        }
        // ============================================================

        if (rotateAnimator != null) {
            rotateAnimator.cancel();
            rotateAnimator = null;
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMusicPlayEvent(MusicPlayEvent event) {
        MusicBean newMusic = event.getMusic();

        if (newMusic != null && !newMusic.getId().equals(musicId)) {
            musicId = newMusic.getId();
            musicPath = newMusic.getPath();

            // 使用渐变动画刷新界面
            refreshMusicUIWithAnimation();
            loadLyricForCurrentMusic();

            // ==================== 切换歌曲时重新绑定可视化 ====================
            // ExoPlayer的audioSessionId不会变化，所以不需要重新绑定
            // 但可以更新颜色
            // =================================================================
        }

        if (event.isPlaying()) {
            if (ivPlayPause != null) {
                ivPlayPause.setImageResource(R.drawable.ic_play_pause);
            }
            startRotation();
        } else {
            if (ivPlayPause != null) {
                ivPlayPause.setImageResource(R.drawable.ic_play_arrow);
            }
            pauseRotation();
        }

    }

    /**
     * 带渐变动画的UI刷新
     */
    private void refreshMusicUIWithAnimation() {
        MusicMetadataUtil.MusicInfo info = MusicMetadataUtil.getMusicInfo(musicPath);
        String[] colors = extractThemeColors(info.getCoverBitmap(), 5);
        Random random = new Random();

        // 准备新的封面图片
        Bitmap newCoverBitmap = generateVinylRecord(info.getCoverBitmap(),
                colors[random.nextInt(colors.length)], 1000);

        // 准备新的背景颜色
        int index1 = random.nextInt(colors.length);
        int index2 = (index1 + 1 + random.nextInt(colors.length - 1)) % colors.length;
        int newBgColor1 = Color.parseColor(colors[index1]);
        int newBgColor2 = Color.parseColor(colors[index2]);

        // 准备新的歌曲信息
        String newTitle = info.getTitle();
        String newArtist = info.getArtist();

        // 准备新的喜欢状态
        boolean newIsLiked = MusicDao.isLikedMusic(Tools.getOnAccount(this), musicId);

        // 1. 封面渐变动画（淡出 -> 换图 -> 淡入）
        animateCoverTransition(newCoverBitmap);

        // 2. 背景颜色渐变动画
        animateBackgroundTransition(newBgColor1, newBgColor2);

        // 3. 文字渐变动画
        animateTextTransition(tvMusicTitle, newTitle);
        animateTextTransition(tvMusicArtist, newArtist);

        // 4. 喜欢图标渐变动画
        animateLikeIconTransition(newIsLiked);

        // 重置进度条
        if (seekBar != null) {
            seekBar.setProgress(0);
        }
        if (tvCurrentTime != null) {
            tvCurrentTime.setText("00:00");
        }
    }

    /**
     * 封面图片渐变过渡
     */
    private void animateCoverTransition(Bitmap newBitmap) {
        float currentRotation = ivMusicCover.getRotation();

        cardMusicCover.animate()
                .alpha(0f)
                .setDuration(TRANSITION_DURATION)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .withEndAction(() -> {
                    ivMusicCover.setImageBitmap(newBitmap);
                    ivMusicCover.setRotation(0);

                    cardMusicCover.animate()
                            .alpha(1f)
                            .setDuration(TRANSITION_DURATION)
                            .setInterpolator(new AccelerateDecelerateInterpolator())
                            .withEndAction(() -> {
                                if (MusicPlayerManager.getInstance().isPlaying()) {
                                    startRotation();
                                }
                            })
                            .start();
                })
                .start();
    }

    /**
     * 背景颜色渐变过渡
     */
    private void animateBackgroundTransition(int newColor1, int newColor2) {
        ValueAnimator colorAnimator = ValueAnimator.ofFloat(0f, 1f);
        colorAnimator.setDuration(TRANSITION_DURATION);
        colorAnimator.setInterpolator(new AccelerateDecelerateInterpolator());

        final int startColor1 = currentBgColor1;
        final int startColor2 = currentBgColor2;
        final ArgbEvaluator evaluator = new ArgbEvaluator();

        colorAnimator.addUpdateListener(animation -> {
            float fraction = animation.getAnimatedFraction();

            int color1 = (int) evaluator.evaluate(fraction, startColor1, newColor1);
            int color2 = (int) evaluator.evaluate(fraction, startColor2, newColor2);

            GradientDrawable gradient = new GradientDrawable(
                    GradientDrawable.Orientation.TOP_BOTTOM,
                    new int[]{color1, color2}
            );
            mainBackground.setBackground(gradient);
        });

        colorAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                currentBgColor1 = newColor1;
                currentBgColor2 = newColor2;
            }
        });

        colorAnimator.start();
    }

    /**
     * 文字渐变过渡
     */
    private void animateTextTransition(TextView textView, String newText) {
        if (textView == null) return;

        textView.animate()
                .alpha(0f)
                .translationY(-20f)
                .setDuration(TRANSITION_DURATION)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .withEndAction(() -> {
                    textView.setText(newText);
                    textView.setTranslationY(20f);

                    textView.animate()
                            .alpha(1f)
                            .translationY(0f)
                            .setDuration(TRANSITION_DURATION)
                            .setInterpolator(new AccelerateDecelerateInterpolator())
                            .start();
                })
                .start();
    }

    /**
     * 喜欢图标渐变过渡
     */
    private void animateLikeIconTransition(boolean newIsLiked) {
        if (ivLike == null) return;

        ivLike.animate()
                .alpha(0f)
                .scaleX(0.5f)
                .scaleY(0.5f)
                .setDuration(TRANSITION_DURATION)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .withEndAction(() -> {
                    isLiked = newIsLiked;
                    ivLike.setImageResource(isLiked ? R.drawable.icon_like : R.drawable.icon_dislike);

                    ivLike.animate()
                            .alpha(1f)
                            .scaleX(1f)
                            .scaleY(1f)
                            .setDuration(TRANSITION_DURATION)
                            .setInterpolator(new AccelerateDecelerateInterpolator())
                            .start();
                })
                .start();
    }

    /**
     * 原有的刷新方法保留（用于非动画场景）
     */
    private void refreshMusicUI() {
        MusicMetadataUtil.MusicInfo info = MusicMetadataUtil.getMusicInfo(musicPath);

        String[] colors = extractThemeColors(info.getCoverBitmap(), 10);
        Random random = new Random();
        ivMusicCover.setImageBitmap(generateVinylRecord(info.getCoverBitmap(),
                colors[random.nextInt(colors.length)], 2000));

        stopRotation();
        if (MusicPlayerManager.getInstance().isPlaying()) {
            startRotation();
        }

        int index1 = random.nextInt(colors.length);
        int index2 = (index1 + 1 + random.nextInt(colors.length - 1)) % colors.length;
        int[] gradientColors = {
                Color.parseColor(colors[index1]),
                Color.parseColor(colors[index2])
        };
        GradientDrawable gradient = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                gradientColors
        );
        gradient.setDither(true);
        mainBackground.setBackground(gradient);

        tvMusicTitle.setText(info.getTitle());
        tvMusicArtist.setText(info.getArtist());

        updateLikeButton();

        if (seekBar != null) {
            seekBar.setProgress(0);
        }
        if (tvCurrentTime != null) {
            tvCurrentTime.setText("00:00");
        }
    }

    private void updateLikeButton() {
        isLiked = MusicDao.isLikedMusic(Tools.getOnAccount(this), musicId);
        ivLike.setImageResource(isLiked ? R.drawable.icon_like : R.drawable.icon_dislike);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMusicProgressEvent(MusicProgressEvent event) {
        if (isUserSeeking || seekBar == null) return;

        int current = event.getCurrentPosition();
        int duration = event.getDuration();

        if (current >= 0 && duration > 0) {
            seekBar.setMax(duration);
            seekBar.setProgress(current);

            if (tvCurrentTime != null) {
                tvCurrentTime.setText(formatTime(current));
            }
            if (tvTotalTime != null) {
                tvTotalTime.setText(formatTime(duration));
            }
            // ★★★ 关键：更新歌词显示 ★★★
            if (lyricView != null) {
                lyricView.updateTime(current);
            }
        }
    }

    private String formatTime(int milliseconds) {
        int seconds = milliseconds / 1000;
        int minutes = seconds / 60;
        seconds = seconds % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }
}