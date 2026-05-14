package com.cool.music.activity.user.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.cool.music.R;
import com.cool.music.adapter.user.SheetMusicAdapter;
import com.cool.music.bean.MusicBean;
import com.cool.music.dao.MusicDao;
import com.cool.music.util.Tools;

import java.util.ArrayList;
import java.util.List;

/**
 * 我喜欢Fragment - 展示用户收藏/常听的音乐
 */
public class LikeFragment extends Fragment {

    private RecyclerView rvPlaylist;
    private TextView tvEmpty;
    private SheetMusicAdapter adapter;
    private final List<MusicBean> musicList = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.frame_user_like, container, false);

        initViews(root);
        setupRecyclerView();

        return root;
    }

    @Override
    public void onResume() {
        super.onResume();
        // 每次可见时刷新数据，确保数据最新
        loadLikedMusic();
    }

    /** 初始化视图 */
    private void initViews(View root) {
        rvPlaylist = root.findViewById(R.id.rv_like_playlist);
        // 如果布局中有空状态TextView，取消注释下一行
        // tvEmpty = root.findViewById(R.id.tv_empty);
    }

    /** 初始化RecyclerView */
    private void setupRecyclerView() {
        rvPlaylist.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new SheetMusicAdapter(musicList);
        rvPlaylist.setAdapter(adapter);
    }

    /** 加载用户喜欢的音乐 */
    private void loadLikedMusic() {
        String account = Tools.getOnAccount(requireContext());
        List<MusicBean> result = MusicDao.getLikedMusicByUserPlayDuration(account);

        musicList.clear();
        if (result != null && !result.isEmpty()) {
            musicList.addAll(result);
        }
        adapter.notifyDataSetChanged();

        // 更新空状态显示
        updateEmptyState();
    }

    /** 更新空状态视图 */
    private void updateEmptyState() {
        if (tvEmpty != null) {
            tvEmpty.setVisibility(musicList.isEmpty() ? View.VISIBLE : View.GONE);
            rvPlaylist.setVisibility(musicList.isEmpty() ? View.GONE : View.VISIBLE);
        }
    }
}