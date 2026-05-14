package com.cool.music.activity.user;

import android.app.AlertDialog;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.appcompat.widget.Toolbar;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;
import com.cool.music.R;
import com.cool.music.bean.SheetBean;
import com.cool.music.bean.UserBean;
import com.cool.music.dao.SheetDao;
import com.cool.music.dao.UserDao;
import com.cool.music.util.FileUtil;
import com.cool.music.util.Tools;

public class EditSheetActivity extends AppCompatActivity {

    // 顶部歌单信息视图
    private ImageView ivSheetCover;
    private TextView tvSheetName;
    private TextView tvSheetDescription;
    private TextView tvSongCount;
    private TextView tvPlayCount;
    private ImageView ivOwnerAvatar;
    private TextView tvOwnerName;
    private TextView tvPrivateLabel;
    private Toolbar toolbar;

    // 编辑区域视图
    private EditText etSheetName;
    private EditText etSheetDescription;
    private TextView tvChangeCover;
    private SwitchCompat switchPublic;
    private TextView tvDeleteSheet;
    private TextView tvSaveChanges;

    // 数据
    private String sheetId;
    private SheetBean sheetBean;
    private UserBean owner;

    // 封面图片选择
    private Uri coverUri = null;
    private String newCoverPath = null;
    private ActivityResultLauncher<String> coverLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_edit_sheet);

        // 初始化视图
        initViews();

        // 获取传递的数据
        loadData();

        // 设置歌单信息
        setupSheetInfo();

        // 初始化封面选择器
        initCoverLauncher();

        // 设置点击事件
        setupClickListeners();
    }

    /**
     * 初始化视图
     */
    private void initViews() {
        // 顶部区域
        ivSheetCover = findViewById(R.id.iv_sheet_cover);
        tvSheetName = findViewById(R.id.tv_sheet_name);
        tvSheetDescription = findViewById(R.id.tv_sheet_description);
        tvSongCount = findViewById(R.id.tv_song_count);
        tvPlayCount = findViewById(R.id.tv_play_count);
        ivOwnerAvatar = findViewById(R.id.iv_owner_avatar);
        tvOwnerName = findViewById(R.id.tv_owner_name);
        tvPrivateLabel = findViewById(R.id.tv_private_label);
        toolbar = findViewById(R.id.toolbar);

        // 编辑区域
        etSheetName = findViewById(R.id.et_sheet_name);
        etSheetDescription = findViewById(R.id.et_sheet_description);
        tvChangeCover = findViewById(R.id.tv_change_cover);
        switchPublic = findViewById(R.id.switch_public);
        tvDeleteSheet = findViewById(R.id.tv_delete_sheet);
        tvSaveChanges = findViewById(R.id.tv_save_changes);

        // 设置Toolbar
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                getSupportActionBar().setTitle("");
            }
            toolbar.setNavigationOnClickListener(v -> onBackPressed());
        }
    }

    /**
     * 加载数据
     */
    private void loadData() {
        sheetId = getIntent().getStringExtra("sheetId");

        if (!TextUtils.isEmpty(sheetId)) {
            sheetBean = SheetDao.getSheetBySheetId(sheetId);
            owner = UserDao.getUserBySheet(sheetId);
        }

        if (sheetBean == null) {
            Tools.toast(this, "获取歌单信息失败");
            finish();
            return;
        }

        // 初始化封面路径
        newCoverPath = sheetBean.getCover_image();
    }

    /**
     * 设置歌单信息
     */
    private void setupSheetInfo() {
        if (sheetBean == null) return;

        // 设置顶部显示区域
        if (tvSheetName != null && !TextUtils.isEmpty(sheetBean.getName())) {
            tvSheetName.setText(sheetBean.getName());
        }

        if (tvSheetDescription != null && !TextUtils.isEmpty(sheetBean.getDescription())) {
            tvSheetDescription.setText(sheetBean.getDescription());
        }

        if (tvSongCount != null && !TextUtils.isEmpty(sheetBean.getSong_count())) {
            tvSongCount.setText(sheetBean.getSong_count() + "首");
        }

        if (tvPlayCount != null && !TextUtils.isEmpty(sheetBean.getPlay_count())) {
            tvPlayCount.setText("播放 " + sheetBean.getPlay_count() + "次");
        }

        // 加载封面图片
        loadCoverImage();

        // 设置创建者信息
        if (owner != null) {
            if (tvOwnerName != null) {
                tvOwnerName.setText(owner.getNickname());
            }
            if (ivOwnerAvatar != null && !TextUtils.isEmpty(owner.getAvatar_path())) {
                Glide.with(this)
                        .load(owner.getAvatar_path())
                        .placeholder(R.drawable.default_avatar)
                        .into(ivOwnerAvatar);
            }
        }

        // 设置公开状态标签
        setupPrivateLabel();

        // 设置编辑区域的初始值
        if (etSheetName != null) {
            etSheetName.setText(sheetBean.getName());
        }

        if (etSheetDescription != null) {
            etSheetDescription.setText(sheetBean.getDescription());
        }

        if (switchPublic != null) {
            switchPublic.setChecked("1".equals(sheetBean.getIs_public()));
        }
    }

    /**
     * 加载封面图片
     */
    private void loadCoverImage() {
        if (ivSheetCover == null) return;

        String coverUrl = sheetBean.getCover_image();

        if (!TextUtils.isEmpty(coverUrl)) {
            Glide.with(this)
                    .load(coverUrl)
                    .apply(new RequestOptions()
                            .placeholder(R.drawable.default_sheet_background)
                            .error(R.drawable.default_sheet_background)
                            .transform(new RoundedCorners(16)))
                    .into(ivSheetCover);
        } else {
            ivSheetCover.setImageResource(R.drawable.default_sheet_background);
        }
    }

    /**
     * 设置公开状态标签
     */
    private void setupPrivateLabel() {
        if (tvPrivateLabel == null || sheetBean == null) return;

        String isPublic = sheetBean.getIs_public();

        if ("1".equals(isPublic)) {
            tvPrivateLabel.setVisibility(android.view.View.GONE);
        } else {
            tvPrivateLabel.setVisibility(android.view.View.VISIBLE);
            tvPrivateLabel.setText("未公开");
        }
    }

    /**
     * 初始化封面选择器
     */
    private void initCoverLauncher() {
        coverLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        coverUri = uri;
                        // 显示选择的图片
                        Glide.with(EditSheetActivity.this)
                                .load(uri)
                                .apply(new RequestOptions()
                                        .transform(new RoundedCorners(16)))
                                .into(ivSheetCover);
                    } else {
                        Tools.toast(EditSheetActivity.this, "请选择封面图片");
                    }
                }
        );
    }

    /**
     * 设置点击事件
     */
    private void setupClickListeners() {
        // 更换封面
        if (tvChangeCover != null) {
            tvChangeCover.setOnClickListener(v -> coverLauncher.launch("image/*"));
        }

        // 点击封面也可以更换
        if (ivSheetCover != null) {
            ivSheetCover.setOnClickListener(v -> coverLauncher.launch("image/*"));
        }

        // 公开开关状态变化时更新标签显示
        if (switchPublic != null) {
            switchPublic.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (tvPrivateLabel != null) {
                    if (isChecked) {
                        tvPrivateLabel.setVisibility(android.view.View.GONE);
                    } else {
                        tvPrivateLabel.setVisibility(android.view.View.VISIBLE);
                        tvPrivateLabel.setText("未公开");
                    }
                }
            });
        }

        // 删除歌单
        if (tvDeleteSheet != null) {
            tvDeleteSheet.setOnClickListener(v -> showDeleteConfirmDialog());
        }

        // 保存修改
        if (tvSaveChanges != null) {
            tvSaveChanges.setOnClickListener(v -> saveChanges());
        }
    }

    /**
     * 显示删除确认对话框
     */
    private void showDeleteConfirmDialog() {
        new AlertDialog.Builder(this)
                .setTitle("删除歌单")
                .setMessage("确定要删除这个歌单吗？此操作不可恢复。")
                .setPositiveButton("删除", (dialog, which) -> deleteSheet())
                .setNegativeButton("取消", null)
                .show();
    }

    /**
     * 删除歌单
     */
    private void deleteSheet() {
        if (TextUtils.isEmpty(sheetId)) {
            Tools.toast(this, "歌单ID无效");
            return;
        }

        try {
            SheetDao.deleteSheet(sheetId);
            Tools.toast(this, "歌单已删除");
            setResult(RESULT_OK);
            finish();
        } catch (Exception e) {
            Tools.toast(this, "删除失败：" + e.getMessage());
        }
    }

    /**
     * 保存修改
     */
    private void saveChanges() {
        String name = etSheetName.getText().toString().trim();
        String description = etSheetDescription.getText().toString().trim();
        boolean isPublic = switchPublic.isChecked();

        // 验证歌单名称
        if (TextUtils.isEmpty(name)) {
            Tools.toast(this, "歌单名称不能为空");
            return;
        }

        tvSaveChanges.setEnabled(false);

        // 如果有新封面，先保存封面
        if (coverUri != null) {
            saveCoverAndUpdate(name, description, isPublic);
        } else {
            // 直接更新歌单信息
            updateSheetInfo(name, description, isPublic);
        }
    }

    /**
     * 保存封面并更新歌单信息
     */
    private void saveCoverAndUpdate(String name, String description, boolean isPublic) {
        FileUtil.saveImage(this, coverUri, new FileUtil.SaveCallback() {
            @Override
            public void onSuccess(String coverPath) {
                newCoverPath = coverPath;
                runOnUiThread(() -> updateSheetInfo(name, description, isPublic));
            }

            @Override
            public void onFail(Exception e) {
                runOnUiThread(() -> {
                    Tools.toast(EditSheetActivity.this, "封面保存失败：" + e.getMessage());
                    tvSaveChanges.setEnabled(true);
                });
            }
        });
    }

    /**
     * 更新歌单信息到数据库
     */
    private void updateSheetInfo(String name, String description, boolean isPublic) {
        try {
            SheetDao.updateSheetInfo(sheetId, name, description, newCoverPath, isPublic ? "1" : "0");

            // 更新顶部显示区域
            if (tvSheetName != null) {
                tvSheetName.setText(name);
            }
            if (tvSheetDescription != null) {
                tvSheetDescription.setText(description);
            }

            Tools.toast(this, "保存成功");
            setResult(RESULT_OK);
            finish();
        } catch (Exception e) {
            Tools.toast(this, "保存失败：" + e.getMessage());
            tvSaveChanges.setEnabled(true);
        }
    }

    @Override
    public void onBackPressed() {
        // 检查是否有未保存的修改
        if (hasUnsavedChanges()) {
            new AlertDialog.Builder(this)
                    .setTitle("放弃修改")
                    .setMessage("您有未保存的修改，确定要放弃吗？")
                    .setPositiveButton("放弃", (dialog, which) -> super.onBackPressed())
                    .setNegativeButton("继续编辑", null)
                    .show();
        } else {
            super.onBackPressed();
        }
    }

    /**
     * 检查是否有未保存的修改
     */
    private boolean hasUnsavedChanges() {
        if (sheetBean == null) return false;

        String currentName = etSheetName.getText().toString().trim();
        String currentDescription = etSheetDescription.getText().toString().trim();
        boolean currentIsPublic = switchPublic.isChecked();

        String originalName = sheetBean.getName() != null ? sheetBean.getName() : "";
        String originalDescription = sheetBean.getDescription() != null ? sheetBean.getDescription() : "";
        boolean originalIsPublic = "1".equals(sheetBean.getIs_public());

        // 检查是否有任何字段被修改
        return !currentName.equals(originalName)
                || !currentDescription.equals(originalDescription)
                || currentIsPublic != originalIsPublic
                || coverUri != null;
    }
}