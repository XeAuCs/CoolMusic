package com.cool.music.activity.user;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.cool.music.R;
import com.cool.music.activity.user.fragment.HomeFragment;
import com.cool.music.activity.user.fragment.LikeFragment;
import com.cool.music.activity.user.fragment.MyFragment;
import com.cool.music.bean.MusicBean;
import com.cool.music.dao.SheetDao;
import com.cool.music.event.MusicPlayEvent;
import com.cool.music.event.MusicProgressEvent;
import com.cool.music.event.PlayControlEvent;
import com.cool.music.event.SeekEvent;
import com.cool.music.player.MusicPlayerManager;
import com.cool.music.util.MusicMetadataUtil;
import com.cool.music.util.Tools;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

public class UserManagerActivity extends AppCompatActivity {

    private CardView cv;
    private ImageView iv;
    private TextView tv_song_name;
    private TextView tv_singer_name;
    private ImageView iv_play;
    private ImageView iv_next;
    private TextView tv_duration;
    private TextView tv_current;
    private SeekBar sb;

    private BottomNavigationView bottomNavigationView;

    // 用户正在拖动进度条的标志
    private boolean isUserSeeking = false;

    // 旋转动画
    private ObjectAnimator rotateAnimator;

    // 当前歌曲的唯一标识，用于检测切歌
    private String currentMusicId = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_user_manager);

        // Fragment 设置
        FragmentManager fragment_container = getSupportFragmentManager();
        FragmentTransaction transaction = fragment_container.beginTransaction();
        transaction.replace(R.id.user_fl_frame, new HomeFragment());
        transaction.commit();

        // 初始化控件
        initViews();

        // 初始化旋转动画
        initRotateAnimator();

        // 初始化播放卡片状态
        loadCurrentMusic();


        iv.setOnClickListener(v -> {
            MusicBean music = MusicPlayerManager.getInstance().getCurrentMusic();
            if (music != null) {
                // 先触发播放（如果当前是暂停状态）
                if (!MusicPlayerManager.getInstance().isPlaying()) {
                    EventBus.getDefault().post(new PlayControlEvent(PlayControlEvent.ACTION_TOGGLE));
                }

                // 再跳转到详情页
                Intent intent = new Intent(UserManagerActivity.this, RunMusicDetailActivity.class);
                intent.putExtra("musicPath", music.getPath());
                intent.putExtra("musicId", music.getId());
                intent.putExtra("rotation", iv.getRotation());
                startActivity(intent);
            }
        });
        // 播放按钮点击事件
        iv_play.setOnClickListener(v -> {
            EventBus.getDefault().post(new PlayControlEvent(PlayControlEvent.ACTION_TOGGLE));
        });

        if (iv_next != null) {
            iv_next.setOnClickListener(v -> {
                EventBus.getDefault().post(new PlayControlEvent(PlayControlEvent.ACTION_NEXT));
            });
        }

        // SeekBar 拖动事件
        sb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && tv_current != null) {
                    // 用户拖动时实时更新时间显示
                    tv_current.setText(formatTime(progress));
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                isUserSeeking = true;
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                int seekPosition = seekBar.getProgress();

                // 发送跳转事件
                EventBus.getDefault().post(new SeekEvent(seekPosition));

                // 延迟恢复，确保 seek 事件已被处理
                sb.postDelayed(() -> {
                    isUserSeeking = false;
                }, 100);
            }
        });


        bottomNavigationView.setOnItemSelectedListener(item -> {
            FragmentManager fm = getSupportFragmentManager();
            FragmentTransaction ft = fm.beginTransaction();

            int id = item.getItemId();
            if (id == R.id.user_bn_menu_home) {
                ft.replace(R.id.user_fl_frame, new HomeFragment());
            } else if (id == R.id.user_bn_menu_list) {
                ft.replace(R.id.user_fl_frame, new LikeFragment());
            } else if (id == R.id.user_bn_menu_my) {
                ft.replace(R.id.user_fl_frame, new MyFragment());
            }

            ft.commit();
            return true;  // ✅ 返回 true 表示事件已处理
        });

    }

    private void initViews() {
        cv = findViewById(R.id.user_card_player);
        iv = cv.findViewById(R.id.user_iv_album);
        tv_song_name = cv.findViewById(R.id.user_tv_song_title);
        tv_singer_name = cv.findViewById(R.id.user_tv_artist);
        iv_play = cv.findViewById(R.id.user_iv_play);
        tv_duration = cv.findViewById(R.id.user_tv_total_time);
        tv_current = cv.findViewById(R.id.user_tv_current_time);
        sb = cv.findViewById(R.id.user_seekbar_progress);
        iv_next = cv.findViewById(R.id.user_iv_next);
        bottomNavigationView = findViewById(R.id.user_bn_menu);
    }

    /**
     * 初始化旋转动画
     */
    private void initRotateAnimator() {
        rotateAnimator = ObjectAnimator.ofFloat(iv, "rotation", 0f, -360f);
        rotateAnimator.setDuration(12000);
        rotateAnimator.setRepeatCount(ValueAnimator.INFINITE);
        rotateAnimator.setRepeatMode(ValueAnimator.RESTART);
        rotateAnimator.setInterpolator(new LinearInterpolator());
    }

    /**
     * 开始/恢复旋转动画
     */
    private void startRotation() {
        if (rotateAnimator != null) {
            if (rotateAnimator.isPaused()) {
                rotateAnimator.resume();
            } else if (!rotateAnimator.isRunning()) {
                rotateAnimator.start();
            }
        }
    }

    /**
     * 暂停旋转动画（保持当前角度）
     */
    private void pauseRotation() {
        if (rotateAnimator != null && rotateAnimator.isRunning()) {
            rotateAnimator.pause();
        }
    }

    /**
     * 停止旋转动画并重置角度
     */
    private void stopRotation() {
        if (rotateAnimator != null) {
            rotateAnimator.cancel();
            iv.setRotation(0f);
        }
    }

    private void loadCurrentMusic() {
        MusicPlayerManager player = MusicPlayerManager.getInstance();
        MusicBean music = player.getCurrentMusic();

        if (music != null) {
            // ✅ 优先使用 MusicPlayerManager 的状态
            updatePlayerCard(music, player.isPlaying());
        } else {
            // ⚠️ MusicPlayerManager 没有音乐，尝试从数据库恢复
            music = SheetDao.getCurrentUserPlayMusic(Tools.getOnAccount(this), "1");
            if (music == null) {
                cv.setVisibility(CardView.GONE);
            } else {
                // 从数据库恢复时，默认是暂停状态（因为播放器还没开始播放）
                updatePlayerCard(music, false);
            }
        }
    }

    /**
     * 更新播放卡片 UI
     */
    private void updatePlayerCard(MusicBean music, boolean isPlaying) {
        if (music == null) {
            cv.setVisibility(CardView.GONE);
            stopRotation();
            currentMusicId = null;
            return;
        }

        if (cv.getVisibility() != CardView.VISIBLE) {
            cv.setAlpha(0f);
            cv.setTranslationY(100f);
            cv.setVisibility(CardView.VISIBLE);
            cv.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(500)
                    .setInterpolator(new DecelerateInterpolator())
                    .start();
        }

        // 检测是否切换了歌曲
        String newMusicId = music.getId();
        boolean isSongChanged = !java.util.Objects.equals(currentMusicId, newMusicId);
        currentMusicId = newMusicId;

        String path = music.getPath();
        MusicMetadataUtil.MusicInfo info = MusicMetadataUtil.getMusicInfo(path);

        if (info != null) {
            tv_song_name.setText(info.getTitle());
            tv_singer_name.setText(info.getArtist());

            Bitmap cover = info.getCoverBitmap();
            if (cover != null) {
                iv.setImageBitmap(cover);
            } else {
                iv.setImageResource(R.drawable.icon_default_cover_image);
            }

            tv_duration.setText(info.getFormattedDuration());
            sb.setMax(info.getDuration());

            // 切换歌曲时重置进度条
            if (isSongChanged) {
                sb.setProgress(0);
                if (tv_current != null) {
                    tv_current.setText("00:00");
                }
            }
        }

        // 更新播放按钮图标和旋转状态
        if (isPlaying) {
            iv_play.setImageResource(R.drawable.ic_play_pause);
            startRotation();
        } else {
            iv_play.setImageResource(R.drawable.ic_play_arrow);
            pauseRotation();
        }
    }

    // ==================== EventBus 注册/注销 ====================

    @Override
    protected void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);

        // 返回时主动同步当前播放状态
        syncCurrentPlayingState();
    }

    /**
     * 同步当前播放状态（从其他Activity返回时调用）
     */
    private void syncCurrentPlayingState() {
        MusicPlayerManager player = MusicPlayerManager.getInstance();
        MusicBean currentMusic = player.getCurrentMusic();

        if (currentMusic != null) {
            // ✅ 始终以 MusicPlayerManager 为准
            updatePlayerCard(currentMusic, player.isPlaying());

            // 同步进度条
            if (sb != null && player.getDuration() > 0) {
                sb.setMax((int) player.getDuration());
                sb.setProgress((int) player.getCurrentPosition());
                if (tv_current != null) {
                    tv_current.setText(formatTime((int) player.getCurrentPosition()));
                }
            }
        } else {
            // 播放器为空时，尝试数据库恢复
            loadCurrentMusic();
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
        if (rotateAnimator != null) {
            rotateAnimator.cancel();
            rotateAnimator = null;
        }
    }

    // ==================== 接收事件 ====================

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMusicPlayEvent(MusicPlayEvent event) {
        updatePlayerCard(event.getMusic(), event.isPlaying());
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMusicProgressEvent(MusicProgressEvent event) {
        // 用户正在拖动时不更新
        if (isUserSeeking) {
            return;
        }

        if (sb == null) {
            return;
        }

        int newProgress = event.getCurrentPosition();
        int duration = event.getDuration();

        // 基本的数据有效性检查
        if (newProgress < 0 || duration <= 0) {
            return;
        }

        // 防止进度超过总时长
        if (newProgress > duration) {
            newProgress = duration;
        }

        // 更新进度条
        sb.setProgress(newProgress);

        // 更新时间显示
        if (tv_current != null) {
            tv_current.setText(formatTime(newProgress));
        }
    }

    private String formatTime(int milliseconds) {
        int seconds = milliseconds / 1000;
        int minutes = seconds / 60;
        seconds = seconds % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }
}