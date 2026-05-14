package com.cool.music.activity.user;

import android.net.Uri;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.bumptech.glide.Glide;
import com.cool.music.R;
import com.cool.music.bean.UserBean;
import com.cool.music.dao.UserDao;
import com.cool.music.util.FileUtil;
import com.cool.music.util.Tools;

import java.io.File;

public class UserChangeInfoActivity extends AppCompatActivity {
    private Uri avatarUri = null;
    private Uri backgroundUri = null;
    private ActivityResultLauncher<String> avatarLauncher;
    private ActivityResultLauncher<String> backgroundLauncher;

    private String newAvatarPath = null;
    private String newBackgroundPath = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_user_change_info);



        // 初始化视图
        ImageView ivBackground = findViewById(R.id.iv_profile_background);
        ImageView ivAvatar = findViewById(R.id.iv_avatar);
        TextView tvNickname = findViewById(R.id.tv_nickname);
        TextView tvLocation = findViewById(R.id.tv_location);
        ImageView ivSex = findViewById(R.id.iv_sex);

        TextView tvAvatar = findViewById(R.id.tv_avatar);
        TextView tvBackground = findViewById(R.id.tv_background);

        TextView tvAccount = findViewById(R.id.tv_account);
        EditText etNickname = findViewById(R.id.et_nickname);
        EditText etGender = findViewById(R.id.et_gender);

        EditText etOldPassword = findViewById(R.id.et_old_password);
        EditText etNewPassword = findViewById(R.id.et_new_password);
        EditText etConfirmPassword = findViewById(R.id.et_confirm_password);

        EditText etRegion = findViewById(R.id.et_region);

        TextView tvSave = findViewById(R.id.tv_save);
        TextView tvLogout = findViewById(R.id.tv_logout);

        // 获取当前用户信息
        String account = Tools.getOnAccount(this);
        UserBean currentUserInfo = UserDao.getUserByAccount(account);

        if (currentUserInfo == null) {
            Tools.toast(this, "获取用户信息失败");
            finish();
            return;
        }

        // 初始化路径
        newAvatarPath = currentUserInfo.getAvatar_path();
        newBackgroundPath = currentUserInfo.getBackground_path();

        // 显示当前用户信息
        loadUserInfo(currentUserInfo, ivBackground, ivAvatar, tvNickname, tvLocation, ivSex,
                tvAccount, etNickname, etGender, etRegion);

        // 注册头像选择器
        avatarLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        avatarUri = uri;
                        ivAvatar.setImageURI(uri);
                    } else {
                        Tools.toast(UserChangeInfoActivity.this, "请选择头像");
                    }
                }
        );

        // 注册背景选择器
        backgroundLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        backgroundUri = uri;
                        ivBackground.setImageURI(uri);
                    } else {
                        Tools.toast(UserChangeInfoActivity.this, "请选择背景图片");
                    }
                }
        );

        // 点击更换头像
        tvAvatar.setOnClickListener(v -> avatarLauncher.launch("image/*"));
        ivAvatar.setOnClickListener(v -> avatarLauncher.launch("image/*"));

        // 点击更换背景
        tvBackground.setOnClickListener(v -> backgroundLauncher.launch("image/*"));
        ivBackground.setOnClickListener(v -> backgroundLauncher.launch("image/*"));

        // 保存按钮
        tvSave.setOnClickListener(v -> {
            String nicknameText = etNickname.getText().toString().trim();
            String genderText = etGender.getText().toString().trim();
            String regionText = etRegion.getText().toString().trim();

            String oldPassword = etOldPassword.getText().toString().trim();
            String newPassword = etNewPassword.getText().toString().trim();
            String confirmPassword = etConfirmPassword.getText().toString().trim();

            // 验证昵称
            if (nicknameText.isEmpty()) {
                Tools.toast(UserChangeInfoActivity.this, "昵称不能为空");
                return;
            }

            // 验证密码修改
            String finalPassword = currentUserInfo.getPassword();
            if (!oldPassword.isEmpty() || !newPassword.isEmpty() || !confirmPassword.isEmpty()) {
                // 用户想要修改密码
                if (!oldPassword.equals(currentUserInfo.getPassword())) {
                    Tools.toast(UserChangeInfoActivity.this, "原密码错误");
                    return;
                }
                if (newPassword.isEmpty()) {
                    Tools.toast(UserChangeInfoActivity.this, "请输入新密码");
                    return;
                }
                if (!newPassword.equals(confirmPassword)) {
                    Tools.toast(UserChangeInfoActivity.this, "两次输入的密码不一致");
                    return;
                }
                finalPassword = newPassword;
            }

            tvSave.setEnabled(false);

            final String passwordToSave = finalPassword;

            // 处理图片保存的回调链
            saveImagesAndUpdate(account, nicknameText, passwordToSave, genderText, regionText, tvSave);
        });


    }

    /**
     * 加载用户信息到视图
     */
    private void loadUserInfo(UserBean user, ImageView ivBackground, ImageView ivAvatar,
                              TextView tvNickname, TextView tvLocation, ImageView ivSex,
                              TextView tvAccount, EditText etNickname, EditText etGender, EditText etRegion) {
        // 加载头像
        if (user.getAvatar_path() != null && !user.getAvatar_path().isEmpty()) {
            File avatarFile = new File(user.getAvatar_path());
            if (avatarFile.exists()) {
                Glide.with(this).load(avatarFile).into(ivAvatar);
            } else {
                ivAvatar.setImageResource(R.drawable.default_avatar);
            }
        } else {
            ivAvatar.setImageResource(R.drawable.default_avatar);
        }

        // 加载背景
        if (user.getBackground_path() != null && !user.getBackground_path().isEmpty()) {
            File bgFile = new File(user.getBackground_path());
            if (bgFile.exists()) {
                Glide.with(this).load(bgFile).into(ivBackground);
            }else {
                ivBackground.setImageResource(R.drawable.default_background);
            }
        } else {
            ivBackground.setImageResource(R.drawable.default_background);
        }

        // 显示信息
        tvNickname.setText(user.getNickname());
        tvAccount.setText(user.getAccount());
        etNickname.setText(user.getNickname());

        if (user.getSex() != null) {
            etGender.setText(user.getSex());
            // 设置性别图标
            if ("男".equals(user.getSex())) {
                ivSex.setImageResource(R.drawable.ic_sex_male);
            } else if ("女".equals(user.getSex())) {
                ivSex.setImageResource(R.drawable.ic_sex_female);
            }
            else{
                ivSex.setImageResource(R.drawable.ic_sex_secret);
            }
        }

        if (user.getAddress() != null) {
            tvLocation.setText(user.getAddress());
            etRegion.setText(user.getAddress());
        }
    }

    /**
     * 保存图片并更新用户信息
     */
    private void saveImagesAndUpdate(String account, String nickname, String password,
                                     String gender, String region, TextView tvSave) {
        // 先处理头像
        if (avatarUri != null) {
            FileUtil.saveImage(this, avatarUri, new FileUtil.SaveCallback() {
                @Override
                public void onSuccess(String avatarPath) {
                    newAvatarPath = avatarPath;
                    // 头像保存成功后，处理背景
                    saveBackgroundAndUpdate(account, nickname, password, gender, region, tvSave);
                }

                @Override
                public void onFail(Exception e) {
                    runOnUiThread(() -> {
                        Tools.toast(UserChangeInfoActivity.this, "头像保存失败：" + e.getMessage());
                        tvSave.setEnabled(true);
                    });
                }
            });
        } else {
            // 没有新头像，直接处理背景
            saveBackgroundAndUpdate(account, nickname, password, gender, region, tvSave);
        }
    }

    /**
     * 保存背景图片并更新用户信息
     */
    private void saveBackgroundAndUpdate(String account, String nickname, String password,
                                         String gender, String region, TextView tvSave) {
        if (backgroundUri != null) {
            FileUtil.saveImage(this, backgroundUri, new FileUtil.SaveCallback() {
                @Override
                public void onSuccess(String backgroundPath) {
                    newBackgroundPath = backgroundPath;
                    // 背景保存成功后，更新数据库
                    updateUserInfo(account, nickname, password, gender, region, tvSave);
                }

                @Override
                public void onFail(Exception e) {
                    runOnUiThread(() -> {
                        Tools.toast(UserChangeInfoActivity.this, "背景保存失败：" + e.getMessage());
                        tvSave.setEnabled(true);
                    });
                }
            });
        } else {
            // 没有新背景，直接更新数据库
            updateUserInfo(account, nickname, password, gender, region, tvSave);
        }
    }

    /**
     * 更新用户信息到数据库
     */
    private void updateUserInfo(String account, String nickname, String password,
                                String gender, String region, TextView tvSave) {
        runOnUiThread(() -> {
            try {
                UserDao.updateUserInfo(account, nickname, password,
                        newAvatarPath, newBackgroundPath, gender, region);
                Tools.toast(UserChangeInfoActivity.this, "保存成功");
                setResult(RESULT_OK);
                finish();
            } catch (Exception e) {
                Tools.toast(UserChangeInfoActivity.this, "保存失败：" + e.getMessage());
                tvSave.setEnabled(true);
            }
        });
    }
}