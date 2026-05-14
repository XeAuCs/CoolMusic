package com.cool.music.activity.user.fragment;

import android.content.Intent;
import android.graphics.Rect;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.cool.music.R;
import com.cool.music.activity.user.SearchMusicActivity;
import com.cool.music.adapter.user.PlayMusicAdapter;
import com.cool.music.adapter.user.SheetMusicAdapter;
import com.cool.music.bean.MusicBean;
import com.cool.music.bean.SheetBean;
import com.cool.music.dao.MusicDao;
import com.cool.music.dao.SheetDao;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HomeFragment extends Fragment {

    private final ExecutorService executor = Executors.newFixedThreadPool(2);

    private RecyclerView rvPlaylist;
    private RecyclerView rvMusicChart;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.frame_user_home, container, false);

        initViews(root);
        loadDataAsync();
        setupFakeSearchBar(root);

        return root;
    }

    private void initViews(View root) {
        // 推荐歌单 - 初始隐藏
        rvPlaylist = root.findViewById(R.id.rv_recommended_playlist);
        rvPlaylist.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        rvPlaylist.addItemDecoration(new HorizontalSpaceDecoration(dpToPx(12)));
        rvPlaylist.setAlpha(0f);

        // 音乐榜 - 初始隐藏
        rvMusicChart = root.findViewById(R.id.rv_music_chart);
        rvMusicChart.setLayoutManager(new LinearLayoutManager(getContext()));
        rvMusicChart.setAlpha(0f);
    }

    /** 异步加载数据，交错显示 */
    private void loadDataAsync() {
        // 第一步：加载推荐歌单
        executor.execute(() -> {
            List<SheetBean> sheets = SheetDao.getRandomSheets(10);
            if (sheets != null && !sheets.isEmpty() && isAdded()) {
                requireActivity().runOnUiThread(() -> {
                    rvPlaylist.setAdapter(new PlayMusicAdapter(sheets));
                    fadeIn(rvPlaylist, 400);
                });
            }
        });

        // 第二步：延迟加载音乐榜，形成交错效果
        executor.execute(() -> {
            try {
                Thread.sleep(200); // 延迟200ms，形成交错加载感
            } catch (InterruptedException ignored) {}

            List<MusicBean> musicList = MusicDao.getAllMusicByPlayDuration();
            if (musicList != null && !musicList.isEmpty() && isAdded()) {
                requireActivity().runOnUiThread(() -> {
                    SheetMusicAdapter adapter = new SheetMusicAdapter(musicList);
                    adapter.setEnableAnimation(true); // 启用逐项动画
                    rvMusicChart.setAdapter(adapter);
                    fadeIn(rvMusicChart, 400);
                });
            }
        });
    }

    /** 淡入动画 */
    private void fadeIn(View view, int duration) {
        view.setAlpha(0f);
        view.animate()
                .alpha(1f)
                .setDuration(duration)
                .start();
    }

    private void setupFakeSearchBar(View root) {
        CardView cvSearch = root.findViewById(R.id.cv_fake_search);
        cvSearch.setOnClickListener(v ->
                startActivity(new Intent(getContext(), SearchMusicActivity.class)));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        executor.shutdownNow();
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
            if (parent.getChildAdapterPosition(view) != parent.getAdapter().getItemCount() - 1) {
                outRect.right = space;
            }
        }
    }
}