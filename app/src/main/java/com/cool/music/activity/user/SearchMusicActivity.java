package com.cool.music.activity.user;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.cool.music.R;
import com.cool.music.adapter.user.SheetMusicAdapter;
import com.cool.music.bean.MusicBean;
import com.cool.music.dao.MusicDao;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 音乐搜索页面 - 支持实时搜索和热门音乐展示
 */
public class SearchMusicActivity extends AppCompatActivity {

    private RecyclerView rv;
    private final List<MusicBean> musicList = new ArrayList<>();
    private SheetMusicAdapter adapter;

    // 防抖处理
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable searchTask;

    // 异步加载
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_search_music);

        initRecyclerView();
        initSearchView();
        loadMusic(null); // 初始加载热门音乐
    }

    private void initRecyclerView() {
        rv = findViewById(R.id.rv_search_playlist);
        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setAlpha(0f); // 初始隐藏
        adapter = new SheetMusicAdapter(musicList);
        adapter.setEnableAnimation(true); // 启用逐项动画
        rv.setAdapter(adapter);
    }

    private void initSearchView() {
        SearchView sv = findViewById(R.id.sv_search);
        sv.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                loadMusic(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                // 防抖：取消上次任务，延迟300ms执行
                if (searchTask != null) handler.removeCallbacks(searchTask);
                searchTask = () -> loadMusic(newText);
                handler.postDelayed(searchTask, 300);
                return true;
            }
        });
    }

    /** 加载音乐：keyword为空显示热门，否则搜索 */
    private void loadMusic(String keyword) {
        executor.execute(() -> {
            List<MusicBean> result = (keyword == null || keyword.trim().isEmpty())
                    ? MusicDao.getAllMusicByPlayDuration()
                    : MusicDao.searchMusic(keyword);

            runOnUiThread(() -> {
                musicList.clear();
                if (result != null) musicList.addAll(result);

                // 重置动画状态并刷新
                adapter.resetAnimation();
                adapter.notifyDataSetChanged();

                // 淡入列表
                rv.animate()
                        .alpha(1f)
                        .setDuration(300)
                        .start();
            });
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdownNow();
        if (searchTask != null) handler.removeCallbacks(searchTask);
    }
}