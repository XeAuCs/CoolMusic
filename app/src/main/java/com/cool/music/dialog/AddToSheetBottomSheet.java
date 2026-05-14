package com.cool.music.dialog;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.cool.music.R;
import com.cool.music.bean.MusicBean;
import com.cool.music.bean.SheetBean;
import com.cool.music.dao.SheetDao;
import com.cool.music.util.Tools;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 添加音乐到歌单的底部弹窗
 * 可在任意 Activity/Fragment 中使用
 */
public class AddToSheetBottomSheet extends BottomSheetDialogFragment {

    private static final String ARG_MUSIC = "music";
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private MusicBean music;
    private RecyclerView rvSheets;
    private TextView tvTitle;
    private View progressBar;

    private OnAddToSheetListener listener;

    public interface OnAddToSheetListener {
        void onAddSuccess(SheetBean sheet, MusicBean music);
        void onAddFailed(String message);
    }

    /**
     * 创建实例的静态方法
     */
    public static AddToSheetBottomSheet newInstance(MusicBean music) {
        AddToSheetBottomSheet fragment = new AddToSheetBottomSheet();
        Bundle args = new Bundle();
        args.putSerializable(ARG_MUSIC, music);
        fragment.setArguments(args);
        return fragment;
    }

    /**
     * 便捷显示方法 - 在任意 Activity 中调用
     */
    public static void show(androidx.fragment.app.FragmentActivity activity, MusicBean music) {
        show(activity, music, null);
    }

    /**
     * 便捷显示方法 - 在任意 Activity 中调用，带回调
     */
    public static void show(androidx.fragment.app.FragmentActivity activity, MusicBean music,
                            OnAddToSheetListener listener) {
        AddToSheetBottomSheet sheet = newInstance(music);
        sheet.setOnAddToSheetListener(listener);
        sheet.show(activity.getSupportFragmentManager(), "AddToSheetBottomSheet");
    }

    /**
     * 便捷显示方法 - 在任意 Fragment 中调用
     */
    public static void show(androidx.fragment.app.Fragment fragment, MusicBean music) {
        show(fragment, music, null);
    }

    /**
     * 便捷显示方法 - 在任意 Fragment 中调用，带回调
     */
    public static void show(androidx.fragment.app.Fragment fragment, MusicBean music,
                            OnAddToSheetListener listener) {
        AddToSheetBottomSheet sheet = newInstance(music);
        sheet.setOnAddToSheetListener(listener);
        sheet.show(fragment.getChildFragmentManager(), "AddToSheetBottomSheet");
    }

    public void setOnAddToSheetListener(OnAddToSheetListener listener) {
        this.listener = listener;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            music = (MusicBean) getArguments().getSerializable(ARG_MUSIC);
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        BottomSheetDialog dialog = (BottomSheetDialog) super.onCreateDialog(savedInstanceState);
        dialog.setOnShowListener(dialogInterface -> {
            BottomSheetDialog d = (BottomSheetDialog) dialogInterface;
            View bottomSheet = d.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (bottomSheet != null) {
                BottomSheetBehavior<View> behavior = BottomSheetBehavior.from(bottomSheet);
                behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                behavior.setSkipCollapsed(true);
            }
        });
        return dialog;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_add_to_sheet, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tvTitle = view.findViewById(R.id.tv_title);
        rvSheets = view.findViewById(R.id.rv_sheets);
        progressBar = view.findViewById(R.id.progress_bar);

        tvTitle.setText("添加到歌单");

        rvSheets.setLayoutManager(new LinearLayoutManager(requireContext()));

        // 加载歌单列表
        loadSheets();
    }

    private void loadSheets() {
        progressBar.setVisibility(View.VISIBLE);
        rvSheets.setVisibility(View.GONE);

        executor.execute(() -> {
            // 获取用户的所有歌单
            List<SheetBean> sheets = SheetDao.getSheetByUserId(Tools.getOnAccount(requireContext()));

            if (isAdded()) {
                requireActivity().runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    rvSheets.setVisibility(View.VISIBLE);

                    if (sheets != null && !sheets.isEmpty()) {
                        SheetListAdapter adapter = new SheetListAdapter(sheets, this::onSheetSelected);
                        rvSheets.setAdapter(adapter);
                    } else {
                        tvTitle.setText("暂无歌单");
                    }
                });
            }
        });
    }

    private void onSheetSelected(SheetBean sheet) {
        if (music == null || sheet == null) return;

        // 禁用点击，防止重复提交
        rvSheets.setEnabled(false);

        executor.execute(() -> {
            // 检查歌曲是否已在歌单中
            boolean alreadyExists = SheetDao.isMusicInSheet(sheet.getId(), music.getId());

            if (alreadyExists) {
                if (isAdded()) {
                    requireActivity().runOnUiThread(() -> {
                        Toast.makeText(requireContext(),
                                "该歌曲已在「" + sheet.getName() + "」中", Toast.LENGTH_SHORT).show();
                        rvSheets.setEnabled(true);
                    });
                }
                return;
            }

            // 执行添加操作
            boolean success = SheetDao.addMusicToSheet(sheet.getId(), music.getId());

            if (isAdded()) {
                requireActivity().runOnUiThread(() -> {
                    if (success) {
                        Toast.makeText(requireContext(),
                                "已添加到「" + sheet.getName() + "」", Toast.LENGTH_SHORT).show();
                        if (listener != null) {
                            listener.onAddSuccess(sheet, music);
                        }
                    } else {
                        Toast.makeText(requireContext(), "添加失败", Toast.LENGTH_SHORT).show();
                        if (listener != null) {
                            listener.onAddFailed("添加失败");
                        }
                    }
                    dismiss();
                });
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (!executor.isShutdown()) {
            executor.shutdownNow();
        }
    }

    /**
     * 歌单列表适配器
     */
    private static class SheetListAdapter extends RecyclerView.Adapter<SheetListAdapter.ViewHolder> {

        private final List<SheetBean> sheets;
        private final OnSheetClickListener clickListener;

        interface OnSheetClickListener {
            void onClick(SheetBean sheet);
        }

        SheetListAdapter(List<SheetBean> sheets, OnSheetClickListener listener) {
            this.sheets = sheets;
            this.clickListener = listener;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_sheet_simple, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            SheetBean sheet = sheets.get(position);

            // 设置序号
            holder.tvIndex.setText(String.valueOf(position + 1));

            // 设置歌单名称
            holder.tvName.setText(sheet.getName());

            // 设置歌曲数量
            holder.tvCount.setText(sheet.getSong_count() + " 首");

            // 加载封面图片
            if (sheet.getCover_image() != null && !sheet.getCover_image().isEmpty()) {
                Glide.with(holder.itemView.getContext())
                        .load(sheet.getCover_image())
                        .placeholder(R.drawable.music_cover)
                        .error(R.drawable.music_cover)
                        .centerCrop()
                        .into(holder.ivCover);
            } else {
                holder.ivCover.setImageResource(R.drawable.music_cover);
            }

            // 设置点击事件
            holder.itemView.setOnClickListener(v -> {
                if (clickListener != null) {
                    clickListener.onClick(sheet);
                }
            });

            // 设置添加按钮点击事件（与整个item相同）
            holder.ivInteraction.setOnClickListener(v -> {
                if (clickListener != null) {
                    clickListener.onClick(sheet);
                }
            });
        }

        @Override
        public int getItemCount() {
            return sheets == null ? 0 : sheets.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvIndex;
            ImageView ivCover;
            TextView tvName;
            TextView tvCount;
            ImageView ivInteraction;

            ViewHolder(View itemView) {
                super(itemView);
                tvIndex = itemView.findViewById(R.id.tv_index);
                ivCover = itemView.findViewById(R.id.iv_cover);
                tvName = itemView.findViewById(R.id.tv_sheet_name);
                tvCount = itemView.findViewById(R.id.tv_music_count);
                ivInteraction = itemView.findViewById(R.id.iv_interaction);
            }
        }
    }
}