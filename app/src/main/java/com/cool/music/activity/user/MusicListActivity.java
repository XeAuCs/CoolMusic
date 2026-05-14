package com.cool.music.activity.user;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.graphics.ColorUtils;
import androidx.palette.graphics.Palette;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.cool.music.R;
import com.cool.music.adapter.user.SheetMusicAdapter;
import com.cool.music.bean.MusicBean;
import com.cool.music.bean.SheetBean;
import com.cool.music.bean.UserBean;
import com.cool.music.dao.SheetDao;
import com.cool.music.dao.UserDao;
import com.cool.music.util.Tools;
import com.cool.music.player.MusicPlayerManager;

import java.util.List;

public class MusicListActivity extends AppCompatActivity {

    private ImageView ivSheetCover;
    private TextView tvSheetName;
    private TextView tvSheetDescription;
    private TextView tvSongCount;
    private TextView tvPlayCount;
    private RecyclerView rvMusicPlaylist;

    private SheetBean sheetBean;
    private List<MusicBean> musicList;
    private SheetMusicAdapter adapter;
    private ImageView ivOwnerAvatar;
    private TextView tvOwnerName;
    private TextView tvPrivateLabel;  // 公开状态标签
    private ImageView ivEdit;         // 编辑按钮
    private CardView cvPlayAll;
    UserBean owner;
    String user_id;

    private static final int REQUEST_CODE_EDIT_SHEET = 1001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_music_list);
        user_id = Tools.getOnAccount(this);

        // 初始化视图
        initViews();

        // 获取传递的数据
        loadData();

        // 设置歌单信息
        setupSheetInfo();

        // 设置RecyclerView
        setupRecyclerView();

        // 设置公开状态显示
        setupPrivateLabel();

        // 设置编辑按钮显示
        setupEditButton();

        // 设置播放全部按钮
        setupPlayAllButton();
    }

    /**
     * 设置播放全部按钮点击事件
     */
    private void setupPlayAllButton() {
        if (cvPlayAll == null) {
            return;
        }

        cvPlayAll.setOnClickListener(v -> {
            if (musicList == null || musicList.isEmpty()) {
                Toast.makeText(this, "歌单暂无歌曲", Toast.LENGTH_SHORT).show();
                return;
            }

            // 调用播放管理器播放整个歌单（会清空当前列表并播放新列表）
            MusicPlayerManager.getInstance().playList(musicList, 0);
            Toast.makeText(this, "开始播放全部歌曲", Toast.LENGTH_SHORT).show();
        });
    }

    /**
     * 初始化视图
     */
    private void initViews() {
        ivSheetCover = findViewById(R.id.iv_sheet_cover);
        tvSheetName = findViewById(R.id.tv_sheet_name);
        tvSheetDescription = findViewById(R.id.tv_sheet_description);
        tvSongCount = findViewById(R.id.tv_song_count);
        tvPlayCount = findViewById(R.id.tv_play_count);
        rvMusicPlaylist = findViewById(R.id.rv_music_playlist);
        ivOwnerAvatar = findViewById(R.id.iv_owner_avatar);
        tvOwnerName = findViewById(R.id.tv_owner_name);
        tvPrivateLabel = findViewById(R.id.tv_private_label);  // 初始化公开状态标签
        ivEdit = findViewById(R.id.iv_edit);                   // 初始化编辑按钮
        cvPlayAll = findViewById(R.id.cv_play_all);
    }

    /**
     * 加载数据
     */
    private void loadData() {
        Intent intent = getIntent();
        String sheetId = intent.getStringExtra("sheetId");
        boolean isCreateNew = intent.getBooleanExtra("isCreateNew", false);

        if (!TextUtils.isEmpty(sheetId)) {
            sheetBean = SheetDao.getSheetBySheetId(sheetId);
            musicList = SheetDao.getPlayMusicOnSheet(sheetId);
            owner = UserDao.getUserBySheet(sheetId);
        }
    }

    /**
     * 设置歌单信息
     */
    private void setupSheetInfo() {
        if (sheetBean != null) {
            // 设置歌单名称
            if (tvSheetName != null && !TextUtils.isEmpty(sheetBean.getName())) {
                tvSheetName.setText(sheetBean.getName());

                // 点击歌单名称跳转到编辑界面（仅歌单创建者可编辑）
                tvSheetName.setOnClickListener(v -> {
                    if (owner != null && !TextUtils.isEmpty(user_id) && user_id.equals(owner.getAccount())) {
                        openSheetEditPage();
                    }
                });
            }

            // 设置歌单描述
            if (tvSheetDescription != null && !TextUtils.isEmpty(sheetBean.getDescription())) {
                tvSheetDescription.setText(sheetBean.getDescription());
            }

            // 设置歌曲数量
            if (tvSongCount != null && !TextUtils.isEmpty(sheetBean.getSong_count())) {
                tvSongCount.setText(sheetBean.getSong_count() + "首");
            }

            // 设置播放次数
            if (tvPlayCount != null && !TextUtils.isEmpty(sheetBean.getPlay_count())) {
                tvPlayCount.setText("播放 " + sheetBean.getPlay_count() + "次");
            }

            // 设置封面图片
            loadCoverImage();

            // 设置创建者信息
            if (owner != null) {
                if (tvOwnerName != null) {
                    tvOwnerName.setText(owner.getNickname());
                }
                if (ivOwnerAvatar != null && !TextUtils.isEmpty(owner.getAvatar_path())) {
                    Glide.with(this).load(owner.getAvatar_path()).into(ivOwnerAvatar);
                }
            }
        } else {
            // 如果没有歌单数据，设置默认图片
            if (ivSheetCover != null) {
                ivSheetCover.setImageResource(R.drawable.default_sheet_background);
            }
            if (owner != null) {
                tvOwnerName.setText(owner.getNickname());
                Glide.with(this)
                        .load(owner.getAvatar_path())
                        .placeholder(R.drawable.default_avatar)
                        .into(ivOwnerAvatar);
            }
        }
    }

    /**
     * 根据 is_public 设置公开状态显示
     */
    private void setupPrivateLabel() {
        if (tvPrivateLabel == null || sheetBean == null) {
            return;
        }

        // 获取 is_public 字段值
        String isPublic = sheetBean.getIs_public();

        if ("1".equals(isPublic)) {
            // 公开歌单，隐藏标签
            tvPrivateLabel.setVisibility(View.GONE);
        } else {
            // 未公开歌单，显示"未公开"标签
            tvPrivateLabel.setVisibility(View.VISIBLE);
            tvPrivateLabel.setText("未公开");
        }
    }

    /**
     * 根据 user_id 和 owner 设置编辑按钮显示
     * 只有当前用户是歌单创建者时才显示编辑按钮
     */
    private void setupEditButton() {
        if (ivEdit == null) {
            return;
        }

        // 判断当前用户是否为歌单创建者
        if (owner != null && !TextUtils.isEmpty(user_id) && user_id.equals(owner.getAccount())) {
            // 当前用户是歌单创建者，显示编辑按钮
            ivEdit.setVisibility(View.VISIBLE);

            // 设置编辑按钮点击事件
            ivEdit.setOnClickListener(v -> {
                // 如果有歌曲列表，切换编辑模式
                if (adapter != null) {
                    adapter.toggleEditMode();
                    // 旋转编辑按钮图标作为视觉反馈
                    if (adapter.isEditMode()) {
                        ivEdit.animate()
                                .rotation(45f)
                                .setDuration(500)
                                .start();
                    } else {
                        ivEdit.animate()
                                .rotation(0f)
                                .setDuration(500)
                                .start();
                    }
                } else {
                    // 没有歌曲时，直接跳转到歌单编辑页面
                    openSheetEditPage();
                }
            });
        } else {
            // 当前用户不是歌单创建者，隐藏编辑按钮
            ivEdit.setVisibility(View.GONE);
        }
    }

    /**
     * 打开歌单编辑页面
     */
    private void openSheetEditPage() {
        // 跳转到歌单信息编辑页面
        Intent intent = new Intent(this, EditSheetActivity.class); // 替换为你的编辑页面
        intent.putExtra("sheetId", sheetBean.getId());
        intent.putExtra("sheetName", sheetBean.getName());
        intent.putExtra("sheetDescription", sheetBean.getDescription());
        intent.putExtra("isPublic", sheetBean.getIs_public());
        startActivityForResult(intent, REQUEST_CODE_EDIT_SHEET);
    }

    /**
     * 加载封面图片
     */
    private void loadCoverImage() {
        if (ivSheetCover == null) return;

        String coverUrl = sheetBean.getCover_image();

        if (!TextUtils.isEmpty(coverUrl)) {
            Glide.with(this)
                    .asBitmap()
                    .load(coverUrl)
                    .apply(new RequestOptions()
                            .placeholder(R.drawable.default_sheet_background)
                            .error(R.drawable.default_sheet_background))
                    .into(new CustomTarget<Bitmap>() {
                        @Override
                        public void onResourceReady(@NonNull Bitmap bitmap, @Nullable Transition<? super Bitmap> transition) {
                            // 设置封面图片
                            ivSheetCover.setImageBitmap(bitmap);

                            // 根据图片计算合适的文字颜色
                            adjustTextColorByImage(bitmap);
                        }

                        @Override
                        public void onLoadCleared(@Nullable Drawable placeholder) {
                            ivSheetCover.setImageResource(R.drawable.default_sheet_background);
                        }

                        @Override
                        public void onLoadFailed(@Nullable Drawable errorDrawable) {
                            ivSheetCover.setImageResource(R.drawable.default_sheet_background);
                            // 加载失败时使用默认白色文字
                            setTextColor(Color.WHITE);
                        }
                    });
        } else {
            ivSheetCover.setImageResource(R.drawable.default_sheet_background);
            setTextColor(Color.WHITE);
        }
    }
    private void adjustTextColorByImage(Bitmap bitmap) {
        Palette.from(bitmap).generate(palette -> {
            if (palette == null) {
                setTextColor(Color.WHITE);
                return;
            }

            // 获取主色调，如果没有则计算平均亮度
            Palette.Swatch dominantSwatch = palette.getDominantSwatch();

            int textColor;
            if (dominantSwatch != null) {
                // 根据主色调的亮度决定文字颜色
                float[] hsl = dominantSwatch.getHsl();
                float lightness = hsl[2]; // HSL中的亮度值 0-1

                // 亮度 > 0.5 用深色文字，否则用浅色文字
                textColor = lightness > 0.5f ? Color.BLACK : Color.WHITE;
            } else {
                // 备用方案：计算图片平均亮度
                textColor = calculateAverageBrightness(bitmap) > 128 ? Color.BLACK : Color.WHITE;
            }

            setTextColor(textColor);
        });
    }

    /**
     * 计算图片平均亮度（备用方案）
     */
    private int calculateAverageBrightness(Bitmap bitmap) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        // 采样计算，避免性能问题
        int sampleSize = 10;
        long totalBrightness = 0;
        int count = 0;

        for (int x = 0; x < width; x += sampleSize) {
            for (int y = 0; y < height; y += sampleSize) {
                int pixel = bitmap.getPixel(x, y);
                int r = Color.red(pixel);
                int g = Color.green(pixel);
                int b = Color.blue(pixel);
                // 使用感知亮度公式
                totalBrightness += (int) (0.299 * r + 0.587 * g + 0.114 * b);
                count++;
            }
        }

        return count > 0 ? (int) (totalBrightness / count) : 128;
    }

    /**
     * 设置文字颜色（带透明度）
     */
    private void setTextColor(int baseColor) {
        // 设置70%透明度（alpha = 180）
        int alphaTextColor = ColorUtils.setAlphaComponent(baseColor, 180);
        // 全不透明版本用于标题
        int solidTextColor = baseColor;

        // 应用到各个文字控件
        if (tvSheetName != null) {
            tvSheetName.setTextColor(alphaTextColor);
        }
        if (tvSheetDescription != null) {
            tvSheetDescription.setTextColor(alphaTextColor);
        }
        if (tvSongCount != null) {
            tvSongCount.setTextColor(alphaTextColor);
        }
        if (tvPlayCount != null) {
            tvPlayCount.setTextColor(alphaTextColor);
        }
        if (tvOwnerName != null) {
            tvOwnerName.setTextColor(alphaTextColor);
        }
    }
    /**
     * 设置RecyclerView
     */
    private void setupRecyclerView() {
        if (rvMusicPlaylist == null) return;

        rvMusicPlaylist.setLayoutManager(new LinearLayoutManager(this));

        if (musicList != null && !musicList.isEmpty()) {
            adapter = new SheetMusicAdapter(musicList);

            // 设置删除监听器
            adapter.setOnMusicDeleteListener((position, music) -> {
                showDeleteConfirmDialog(position, music);
            });

            rvMusicPlaylist.setAdapter(adapter);
        }
    }

    /**
     * 显示删除确认对话框
     */
    private void showDeleteConfirmDialog(int position, MusicBean music) {
        deleteMusicFromSheet(position, music);
//        new AlertDialog.Builder(this)
//                .setTitle("删除歌曲")
//                .setMessage("确定要从歌单中删除这首歌吗？")
//                .setPositiveButton("删除", (dialog, which) -> {
//                    deleteMusicFromSheet(position, music);
//                })
//                .setNegativeButton("取消", null)
//                .show();
    }

    /**
     * 从歌单中删除歌曲
     */
    private void deleteMusicFromSheet(int position, MusicBean music) {
        // 从数据库中删除
        String sheetId = getIntent().getStringExtra("sheetId");
        if (!TextUtils.isEmpty(sheetId) && music != null) {

            SheetDao.removeMusicFromSheet(sheetId, music.getId());

            // 从列表中删除并更新UI
            adapter.removeItem(position);

            // 更新歌曲数量显示
            updateSongCount();

//            Toast.makeText(this, "已删除", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 更新歌曲数量显示
     */
    private void updateSongCount() {
        if (tvSongCount != null && musicList != null) {
            tvSongCount.setText(musicList.size() + "首");
        }
    }

    /**
     * 播放音乐
     * @param position 音乐在列表中的位置
     */
    private void playMusic(int position) {
        if (musicList == null || position < 0 || position >= musicList.size()) {
            return;
        }

        // TODO: 跳转到播放页面或启动播放服务

    }

    @Override
    protected void onResume() {
        super.onResume();
        // 可以在这里刷新数据
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 清理资源
    }

    @Override
    public void onBackPressed() {
        // 如果在编辑模式，先退出编辑模式
        if (adapter != null && adapter.isEditMode()) {
            adapter.setEditMode(false);
            ivEdit.animate()
                    .rotation(0f)
                    .setDuration(200)
                    .start();
        } else {
            super.onBackPressed();
        }
    }
}