package com.cool.music.player;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.cool.music.bean.MusicBean;
import com.cool.music.util.SQLiteDbUtils;

/**
 * 播放记录追踪器
 * 负责追踪用户播放进度，当播放达到80%时标记为已完成
 *
 * 改进：支持单曲循环场景，每次完整播放都会创建新的记录
 */
public class ListeningRecordTracker {

    private static final String TAG = "ListeningRecordTracker";

    // 完成阈值：80%
    private static final float COMPLETION_THRESHOLD = 0.8f;

    public String getCurrentMusicId() {
        return currentMusicId;
    }

    // 当前追踪的歌曲信息
    private String currentMusicId;
    private String currentUserId;
    private long totalDuration;           // 总时长(毫秒)
    private long accumulatedPlayTime;     // 累计播放时长(毫秒)
    private long lastUpdateTime;          // 上次更新时间戳
    private boolean isCompleted;          // 是否已标记为完成
    private boolean isPlaying;            // 是否正在播放

    // 记录ID（用于更新记录）
    private String currentRecordId;

    // ★ 新增：用于检测单曲循环的播放位置
    private long lastKnownPosition;       // 上次已知的播放位置
    private boolean loopDetectionEnabled; // 是否启用循环检测

    private Handler handler;
    private Runnable trackingRunnable;
    private static final long TRACKING_INTERVAL = 1000; // 每秒检查一次

    // ★ 新增：循环检测阈值（播放位置回到开头附近）
    private static final long LOOP_POSITION_THRESHOLD = 3000; // 3秒内认为是从头开始

    private static volatile ListeningRecordTracker instance;

    private ListeningRecordTracker() {
        handler = new Handler(Looper.getMainLooper());
    }

    public static ListeningRecordTracker getInstance() {
        if (instance == null) {
            synchronized (ListeningRecordTracker.class) {
                if (instance == null) {
                    instance = new ListeningRecordTracker();
                }
            }
        }
        return instance;
    }

    /**
     * 开始追踪新歌曲的播放
     * 在切换歌曲时调用
     *
     * @param forceNewRecord 是否强制创建新记录（用于单曲循环场景）
     */
    public synchronized void startTracking(String userId, MusicBean music, long duration, boolean forceNewRecord) {
        // 如果不是强制创建，且是同一首歌且已有记录，则跳过
        if (!forceNewRecord && music.getId().equals(this.currentMusicId) && this.currentRecordId != null) {
            Log.d(TAG, "同一首歌，跳过重复创建");
            return;
        }

        // 先保存上一首歌的记录
        saveCurrentRecord();

        // 重置状态
        this.currentUserId = userId;
        this.currentMusicId = music.getId();
        this.totalDuration = duration;
        this.accumulatedPlayTime = 0;
        this.lastUpdateTime = System.currentTimeMillis();
        this.isCompleted = false;
        this.isPlaying = true;

        // ★ 重置循环检测状态
        this.lastKnownPosition = 0;
        this.loopDetectionEnabled = true;

        // 创建新的播放记录（初始 is_completed = 0）
        this.currentRecordId = createNewRecord();

        Log.d(TAG, "开始追踪: musicId=" + currentMusicId + ", duration=" + duration +
                ", forceNew=" + forceNewRecord);

        startTrackingTimer();
    }

    /**
     * 开始追踪新歌曲的播放（默认不强制创建新记录）
     */
    public synchronized void startTracking(String userId, MusicBean music, long duration) {
        startTracking(userId, music, duration, false);
    }

    /**
     * ★ 新增：检测单曲循环并处理
     * 当播放位置从接近结尾跳回开头时，认为是单曲循环
     *
     * @param currentPosition 当前播放位置
     * @return true 如果检测到循环并创建了新记录
     */
    public synchronized boolean checkAndHandleLoop(long currentPosition) {
        if (!loopDetectionEnabled || currentMusicId == null || totalDuration <= 0) {
            return false;
        }

        // 检测条件：上次位置在后半段（>50%），当前位置在开头附近（<3秒）
        boolean wasNearEnd = lastKnownPosition > totalDuration * 0.5;
        boolean isNearStart = currentPosition < LOOP_POSITION_THRESHOLD;

        if (wasNearEnd && isNearStart) {
            Log.d(TAG, "🔁 检测到单曲循环: lastPos=" + lastKnownPosition +
                    "ms, currentPos=" + currentPosition + "ms");

            // 保存当前记录并创建新记录
            handleLoopRestart();
            return true;
        }

        lastKnownPosition = currentPosition;
        return false;
    }

    /**
     * ★ 新增：处理循环重播
     * 保存当前记录，并为新一轮播放创建新记录
     */
    private void handleLoopRestart() {
        // 临时禁用循环检测，避免重复触发
        loopDetectionEnabled = false;

        // 保存当前记录
        saveCurrentRecord();

        // 重置追踪状态（保留 musicId 和 userId）
        this.accumulatedPlayTime = 0;
        this.lastUpdateTime = System.currentTimeMillis();
        this.isCompleted = false;
        this.lastKnownPosition = 0;

        // 创建新记录
        this.currentRecordId = createNewRecord();

        Log.d(TAG, "循环重播：创建新记录 " + currentRecordId);

        // 延迟重新启用循环检测，避免误判
        handler.postDelayed(() -> {
            loopDetectionEnabled = true;
            Log.d(TAG, "循环检测已重新启用");
        }, 5000); // 5秒后重新启用
    }

    /**
     * 暂停追踪（用户暂停播放）
     */
    public void pauseTracking() {
        if (isPlaying) {
            // 累加已播放时间
            long now = System.currentTimeMillis();
            accumulatedPlayTime += (now - lastUpdateTime);
            isPlaying = false;

            stopTrackingTimer();

            Log.d(TAG, "暂停追踪: 累计播放=" + accumulatedPlayTime + "ms");
        }
    }

    /**
     * 恢复追踪（用户继续播放）
     */
    public void resumeTracking() {
        if (!isPlaying && currentMusicId != null) {
            lastUpdateTime = System.currentTimeMillis();
            isPlaying = true;

            startTrackingTimer();

            Log.d(TAG, "恢复追踪");
        }
    }

    /**
     * 停止追踪并保存记录（切歌或退出时调用）
     */
    public void stopTracking() {
        if (isPlaying) {
            long now = System.currentTimeMillis();
            accumulatedPlayTime += (now - lastUpdateTime);
        }
        isPlaying = false;

        stopTrackingTimer();
        saveCurrentRecord();

        // 重置状态
        currentMusicId = null;
        currentRecordId = null;
        loopDetectionEnabled = false;
    }

    /**
     * 处理用户拖动进度条
     * 注意：拖动进度条不应该直接增加播放时长
     */
    public void onSeek(long newPosition) {
        // 先保存当前累计时间
        if (isPlaying) {
            long now = System.currentTimeMillis();
            accumulatedPlayTime += (now - lastUpdateTime);
            lastUpdateTime = now;
        }

        // ★ 更新已知位置，避免 seek 导致误判为循环
        lastKnownPosition = newPosition;

        // 检查是否达到80%（基于累计时间，不是当前位置）
        checkCompletion();

        Log.d(TAG, "Seek到: " + newPosition + "ms, 累计播放: " + accumulatedPlayTime + "ms");
    }

    /**
     * ★ 新增：更新当前播放位置（由 MusicPlayerManager 定期调用）
     * 用于检测单曲循环
     */
    public void updatePlaybackPosition(long position) {
        checkAndHandleLoop(position);
    }

    // ==================== 私有方法 ====================

    private void startTrackingTimer() {
        stopTrackingTimer();

        trackingRunnable = new Runnable() {
            @Override
            public void run() {
                if (isPlaying && currentMusicId != null) {
                    // 更新累计时间
                    long now = System.currentTimeMillis();
                    accumulatedPlayTime += (now - lastUpdateTime);
                    lastUpdateTime = now;

                    // 检查是否达到80%
                    checkCompletion();

                    // 继续定时检查
                    handler.postDelayed(this, TRACKING_INTERVAL);
                }
            }
        };
        handler.postDelayed(trackingRunnable, TRACKING_INTERVAL);
    }

    private void stopTrackingTimer() {
        if (handler != null && trackingRunnable != null) {
            handler.removeCallbacks(trackingRunnable);
            trackingRunnable = null;
        }
    }

    /**
     * 检查是否达到80%完成度
     */
    private void checkCompletion() {
        if (isCompleted || totalDuration <= 0) {
            return;
        }

        float progress = (float) accumulatedPlayTime / totalDuration;

        Log.d(TAG, "当前进度: " + (progress * 100) + "%, 累计: " + accumulatedPlayTime + "ms / " + totalDuration + "ms");

        if (progress >= COMPLETION_THRESHOLD) {
            isCompleted = true;
            markAsCompleted();
            Log.d(TAG, "🎉 达到80%完成度！");
        }
    }

    /**
     * 创建新的播放记录
     */
    private String createNewRecord() {
        if (currentUserId == null || currentMusicId == null) {
            return null;
        }

        String recordId = java.util.UUID.randomUUID().toString();

        // 初始记录：is_completed = 0
        new Thread(() -> {
            try {
                String sql = "INSERT INTO d_play_record (id, user_id, music_id, play_duration_seconds, is_completed) VALUES (?, ?, ?, ?, ?)";
                SQLiteDbUtils.executeUpdate(sql, recordId, currentUserId, currentMusicId, "0", "0");
                Log.d(TAG, "创建播放记录: " + recordId);
            } catch (Exception e) {
                Log.e(TAG, "创建记录失败", e);
            }
        }).start();

        return recordId;
    }

    /**
     * 标记为已完成（更新 is_completed = 1）
     */
    private void markAsCompleted() {
        if (currentRecordId == null) {
            return;
        }

        final String recordId = currentRecordId;
        final long playDurationSeconds = accumulatedPlayTime / 1000;

        new Thread(() -> {
            try {
                String sql = "UPDATE d_play_record SET is_completed = ?, play_duration_seconds = ? WHERE id = ?";
                SQLiteDbUtils.executeUpdate(sql, "1", String.valueOf(playDurationSeconds), recordId);
                Log.d(TAG, "标记完成: recordId=" + recordId + ", duration=" + playDurationSeconds + "s");
            } catch (Exception e) {
                Log.e(TAG, "标记完成失败", e);
            }
        }).start();
    }

    /**
     * 保存当前记录（更新播放时长）
     */
    private void saveCurrentRecord() {
        if (currentRecordId == null || accumulatedPlayTime <= 0) {
            return;
        }

        final String recordId = currentRecordId;
        final long playDurationSeconds = accumulatedPlayTime / 1000;
        final boolean completed = isCompleted;

        new Thread(() -> {
            try {
                String sql = "UPDATE d_play_record SET play_duration_seconds = ?, is_completed = ? WHERE id = ?";
                SQLiteDbUtils.executeUpdate(sql,
                        String.valueOf(playDurationSeconds),
                        completed ? "1" : "0",
                        recordId);
                Log.d(TAG, "保存记录: duration=" + playDurationSeconds + "s, completed=" + completed);
            } catch (Exception e) {
                Log.e(TAG, "保存记录失败", e);
            }
        }).start();
    }

    // ==================== Getter ====================

    public long getAccumulatedPlayTime() {
        if (isPlaying) {
            return accumulatedPlayTime + (System.currentTimeMillis() - lastUpdateTime);
        }
        return accumulatedPlayTime;
    }

    public float getCompletionProgress() {
        if (totalDuration <= 0) return 0;
        return (float) getAccumulatedPlayTime() / totalDuration;
    }

    public boolean isCompleted() {
        return isCompleted;
    }
}