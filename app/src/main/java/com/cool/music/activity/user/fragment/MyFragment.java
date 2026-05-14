package com.cool.music.activity.user.fragment;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Rect;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.cool.music.R;
import com.cool.music.activity.user.MusicListActivity;
import com.cool.music.activity.user.UserChangeInfoActivity;
import com.cool.music.adapter.user.PlayMusicAdapter;
import com.cool.music.bean.SheetBean;
import com.cool.music.bean.UserBean;
import com.cool.music.dao.SheetDao;
import com.cool.music.dao.UserDao;
import com.cool.music.util.Tools;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * 我喜欢Fragment - 展示用户个人信息
 */
public class MyFragment extends Fragment {

    // 视图控件
    private ImageView ivAvatar;
    private TextView tvNickname;
    private ImageView ivSex;
    private ImageView ivChangeSettings;
    private TextView tvLocation;
    private TextView tvSongCount;
    private TextView tvListenDuration;
    private TextView tvRegisterDays;

    private ImageView ivBackground;

    private RecyclerView rvMySheets;

    // 歌单适配器（用于删除操作）
    private PlayMusicAdapter mySheetsAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.frame_user_my, container, false);

        // 初始化视图
        initViews(root);

        // 加载用户数据
        loadUserData();

        return root;
    }

    /**
     * 初始化视图控件
     */
    private void initViews(View root) {
        ivAvatar = root.findViewById(R.id.iv_avatar);
        tvNickname = root.findViewById(R.id.tv_nickname);
        ivSex = root.findViewById(R.id.iv_sex);
        tvLocation = root.findViewById(R.id.tv_location);
        tvSongCount = root.findViewById(R.id.tv_song_count);
        tvListenDuration = root.findViewById(R.id.tv_listen_duration);
        tvRegisterDays = root.findViewById(R.id.tv_register_days);
        ivChangeSettings = root.findViewById(R.id.iv_change_settings);
        ivBackground = root.findViewById(R.id.iv_profile_background);
        rvMySheets = root.findViewById(R.id.rv_my_sheets);



        rvMySheets.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        rvMySheets.addItemDecoration(new HorizontalSpaceDecoration(dpToPx(12)));
        ivChangeSettings.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), UserChangeInfoActivity.class);
            startActivity(intent);
        });


    }
    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }


    private static class HorizontalSpaceDecoration extends RecyclerView.ItemDecoration {
        private final int space;

        HorizontalSpaceDecoration(int space) {
            this.space = space;
        }

        @Override
        public void getItemOffsets(@NonNull Rect outRect, @NonNull View view,
                                   @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
            RecyclerView.Adapter<?> adapter = parent.getAdapter();
            if (adapter == null) {
                return;
            }
            int position = parent.getChildAdapterPosition(view);
            if (position != RecyclerView.NO_POSITION && position != adapter.getItemCount() - 1) {
                outRect.right = space;
            }
        }
    }
    /**
     * 加载用户数据并显示
     */
    private void loadUserData() {
        String account = Tools.getOnAccount(getContext());
        UserBean user = UserDao.getUserByAccount(account);

        if (user == null) {
            return;
        }

        // 加载头像
        loadAvatar(user.getAvatar_path());
        loadBackground(user.getBackground_path());


// 设置昵称
        tvNickname.setText(user.getNickname() != null ? user.getNickname() : "未设置昵称");

// 设置性别

        // 设置性别
        String sex = user.getSex();
        if (sex != null && sex.equals("男")) {
            ivSex.setImageResource(R.drawable.ic_sex_male);
        } else if (sex != null && sex.equals("女")) {
            ivSex.setImageResource(R.drawable.ic_sex_female);
        } else {
            ivSex.setImageResource(R.drawable.ic_sex_secret);
        }

// 设置地址
        tvLocation.setText(user.getAddress() != null ? user.getAddress() : "未设置地址");

// 设置听歌数量
        tvSongCount.setText(user.getSong_count() != null ? user.getSong_count() : "0");

// 设置听歌时长
        tvListenDuration.setText(formatListeningTime(user.getListening_time()));

// 设置注册天数
        tvRegisterDays.setText(calculateRegisterDays(user.getRegistration_time()));

        loadMySheets(account);
    }

    private void loadMySheets(String account) {
        if (getContext() == null) {
            return;
        }

        // 获取用户创建的歌单
        List<SheetBean> mySheets = SheetDao.getSheetByUserId(account);

        // 即使没有歌单也显示添加按钮
        if (mySheets == null) {
            mySheets = new java.util.ArrayList<>();
        }

        mySheetsAdapter = new PlayMusicAdapter(mySheets);

        // 启用添加歌单按钮
        mySheetsAdapter.setShowAddButton(true);

        // 设置长按监听器，实现删除歌单功能
        mySheetsAdapter.setOnSheetLongClickListener((sheet, position) -> {
            showDeleteSheetDialog(sheet, position);
        });

        // 设置添加歌单点击监听器
        mySheetsAdapter.setOnAddSheetClickListener(() -> {
            openCreateSheetActivity();
        });

        rvMySheets.setAdapter(mySheetsAdapter);
    }

    /**
     * 打开创建歌单界面
     */
    private void openCreateSheetActivity() {

        String sheetId=SheetDao.createNewSheet(Tools.getOnAccount(getContext()));
        Intent intent = new Intent(getContext(), MusicListActivity.class);
        // 传递一个标记表示是新建歌单
        intent.putExtra("isCreateNew", true);
        intent.putExtra("sheetId",sheetId);
        startActivity(intent);
    }

    /**
     * 显示删除歌单确认对话框
     * @param sheet 要删除的歌单
     * @param position 歌单在列表中的位置
     */
    private void showDeleteSheetDialog(SheetBean sheet, int position) {
        if (getContext() == null) {
            return;
        }

        new AlertDialog.Builder(getContext())
                .setTitle("删除歌单")
                .setMessage("确定要删除歌单「" + sheet.getName() + "」吗？\n删除后无法恢复。")
                .setPositiveButton("删除", (dialog, which) -> {
                    deleteSheet(sheet, position);
                })
                .setNegativeButton("取消", null)
                .show();
    }

    /**
     * 删除歌单
     * @param sheet 要删除的歌单
     * @param position 歌单在列表中的位置
     */
    private void deleteSheet(SheetBean sheet, int position) {
        if (getContext() == null) {
            return;
        }

        // 从数据库中删除歌单
        SheetDao.deleteSheet(sheet.getId());
        mySheetsAdapter.removeItem(position);

    }

    /**
     * 加载用户头像
     */
    private void loadAvatar(String avatarPath) {
        if (getContext() == null) {
            return;
        }

        if (avatarPath != null && !avatarPath.isEmpty()) {
            File avatarFile = new File(avatarPath);
            if (avatarFile.exists()) {
                Glide.with(getContext())
                        .load(avatarFile)
                        .placeholder(R.drawable.default_avatar)
                        .error(R.drawable.default_avatar)
                        .into(ivAvatar);
            } else {
                ivAvatar.setImageResource(R.drawable.default_avatar);
            }
        } else {
            ivAvatar.setImageResource(R.drawable.default_avatar);
        }
    }

    private void loadBackground(String backgroundPath) {
        if (getContext() == null) {
            return;
        }

        if (backgroundPath != null && !backgroundPath.isEmpty()) {
            File backgroundFile = new File(backgroundPath);
            if (backgroundFile.exists()) {
                Glide.with(getContext())
                        .load(backgroundFile)
                        .placeholder(R.drawable.default_background)  // 换成背景默认图
                        .error(R.drawable.default_background)
                        .into(ivBackground);  // 改为 ivBackground
            } else {
                ivBackground.setImageResource(R.drawable.default_background);
            }
        } else {
            ivBackground.setImageResource(R.drawable.default_background);
        }
    }

    /**
     * 格式化听歌时长
     * @param listeningTime 听歌时长（秒数）
     * @return 格式化后的时长字符串
     */
    private String formatListeningTime(String listeningTime) {
        if (listeningTime == null || listeningTime.isEmpty()) {
            return "0h";
        }

        try {
            long seconds = Long.parseLong(listeningTime);
            long minutes = seconds / 60;
            long hours = minutes / 60;

            if (hours > 0) {
                return hours + "h";
            } else if (minutes > 0) {
                return minutes + "min";
            } else {
                return seconds + "s";
            }
        } catch (NumberFormatException e) {
            return listeningTime;
        }
    }

    /**
     * 计算注册天数
     * @param registrationTime 注册时间字符串 (格式: yyyy-MM-dd HH:mm:ss)
     * @return 注册天数
     */
    private String calculateRegisterDays(String registrationTime) {
        if (registrationTime == null || registrationTime.isEmpty()) {
            return "0";
        }

        try {
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            Date registerDate = format.parse(registrationTime);

            if (registerDate != null) {
                long diffInMillis = System.currentTimeMillis() - registerDate.getTime();
                long days = TimeUnit.MILLISECONDS.toDays(diffInMillis);
                return String.valueOf(Math.max(days, 1)); // 至少显示1天
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }

        return "0";
    }

    @Override
    public void onResume() {
        super.onResume();
        // 每次页面可见时刷新数据
        loadUserData();
    }
}