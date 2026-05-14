package com.cool.music;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.media3.ui.PlayerView;

import com.cool.music.activity.SignUpActivity;
import com.cool.music.activity.user.RunMusicDetailActivity;
import com.cool.music.activity.user.UserManagerActivity;
import com.cool.music.dao.UserDao;
import com.cool.music.player.MusicPlayerManager;
import com.cool.music.util.DBUtil;
import com.cool.music.util.Tools;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this); // 启用边缘到边缘显示，实现全屏沉浸式体验
        setContentView(R.layout.activity_main); // 加载主界面布局

        DBUtil.getInstance(this).init();


        Tools.addPrePreferenceData(this,"account","root");

        MusicPlayerManager.getInstance().init(this,Tools.getOnAccount(this));





        Intent intent = new Intent(this, UserManagerActivity.class);
        startActivity(intent);


        EditText accountEditText = findViewById(R.id.login_et_account); // 获取账号输入框
        EditText passwordEditText = findViewById(R.id.login_et_password); // 获取密码输入框
        Button signInButton = findViewById(R.id.login_btn_sign_in); // 获取登录按钮
        Button signUpButton = findViewById(R.id.login_btn_sign_up);

        signInButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String account = accountEditText.getText().toString().trim(); // 获取并去除首尾空格的账号
                String password = passwordEditText.getText().toString().trim(); // 获取并去除首尾空格的密码

                // 验证账号是否为空
                if (account == null || account.isEmpty()) {
                    Tools.toast(MainActivity.this, "请输入账号");
                }
                // 验证密码是否为空
                else if (password == null || password.isEmpty()) {
                    Tools.toast(MainActivity.this, "请输入密码");
                }
                // 账号密码均有效，进行登录验证
                else {
                    int sta = UserDao.isLogin(account, password); // 调用UserDao进行登录验证
                    if (sta == -1) {
                        Tools.toast(MainActivity.this, "账号密码输入错误");
                    } else if (sta == 0) {
                        Tools.toast(MainActivity.this, "登录用户");
                    } else if (sta == 1) {
                        Tools.toast(MainActivity.this, "登录管理员");
                    } else {
                        Tools.toast(MainActivity.this, "数据格式错误。");
                    }
                }
            }
        });

        signUpButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, SignUpActivity.class);
                startActivity(intent);
            }
        });
    }


}