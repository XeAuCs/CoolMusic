package com.cool.music.activity;

import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioGroup;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.cool.music.R;
import com.cool.music.dao.UserDao;
import com.cool.music.util.FileUtil;
import com.cool.music.util.Tools;

public class SignUpActivity extends AppCompatActivity {
    private Uri result = null;
    private ActivityResultLauncher<String> getContentLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_sign_up);

        Toolbar goback = findViewById(R.id.signup_tb_back);
        setSupportActionBar(goback);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
        goback.setNavigationOnClickListener(v -> finish());

        ImageView avatar = findViewById(R.id.signup_iv_avatar);
        EditText nickname = findViewById(R.id.signup_et_nickname);
        EditText account = findViewById(R.id.signup_et_account);
        EditText password = findViewById(R.id.signup_et_password);
        Button submit = findViewById(R.id.signup_btn_submit);
        RadioGroup genderGroup = findViewById(R.id.signup_rg_gender);

        getContentLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        avatar.setImageURI(uri);
                        result = uri;
                    } else {
                        Tools.toast(SignUpActivity.this, "请选择头像");
                    }
                }
        );

        avatar.setOnClickListener(v -> getContentLauncher.launch("image/*"));

        submit.setOnClickListener(v -> {
            String nicknameText = nickname.getText().toString().trim();
            String accountText = account.getText().toString().trim();
            String passwordText = password.getText().toString().trim();

            int selectedId = genderGroup.getCheckedRadioButtonId();
            String sexText;
            if (selectedId == R.id.signup_rb_male) sexText = "男";
            else if (selectedId == R.id.signup_rb_female) sexText = "女";
            else sexText = "保密";

            if (nicknameText.isEmpty() || accountText.isEmpty() || passwordText.isEmpty()) {
                Tools.toast(SignUpActivity.this, "请填写完整信息");
                return;
            }

            submit.setEnabled(false);

            FileUtil.SaveCallback callback = new FileUtil.SaveCallback() {
                @Override
                public void onSuccess(String avatarPath) {
                    runOnUiThread(() -> {
                        int status = UserDao.register(accountText, nicknameText, passwordText, avatarPath, sexText);
                        Tools.toast(SignUpActivity.this, status == 1 ? "注册成功" : "注册失败");
                        if (status == 1) finish();
                        else submit.setEnabled(true);
                    });
                }

                @Override
                public void onFail(Exception e) {
                    runOnUiThread(() -> {
                        Tools.toast(SignUpActivity.this, "头像保存失败：" + e.getMessage());
                        submit.setEnabled(true);
                    });
                }
            };

            if (result != null) {
                FileUtil.saveImage(this, result, callback);
            } else {
                FileUtil.saveImage(this, R.drawable.default_avatar, callback);
            }
        });
    }
}