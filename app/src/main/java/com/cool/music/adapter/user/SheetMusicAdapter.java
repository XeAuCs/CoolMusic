package com.cool.music.adapter.user;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.cool.music.R;
import com.cool.music.activity.user.RunMusicDetailActivity;
import com.cool.music.bean.MusicBean;
import com.cool.music.dialog.AddToSheetBottomSheet;
import com.cool.music.player.MusicPlayerManager;
import com.cool.music.util.MusicMetadataUtil;

import java.util.List;

public class SheetMusicAdapter extends RecyclerView.Adapter<SheetMusicAdapter.SheetMusicViewHolder> {

    private final List<MusicBean> list;
    private int lastAnimatedPosition = -1;
    private boolean enableAnimation = false;
    private boolean isEditMode = false;
    private OnMusicDeleteListener deleteListener;

    public interface OnMusicDeleteListener {
        void onMusicDelete(int position, MusicBean music);
    }

    public SheetMusicAdapter(List<MusicBean> list) {
        this.list = list;
    }

    public void setOnMusicDeleteListener(OnMusicDeleteListener listener) {
        this.deleteListener = listener;
    }

    public void setEditMode(boolean editMode) {
        this.isEditMode = editMode;
        notifyDataSetChanged();
    }

    public boolean isEditMode() {
        return isEditMode;
    }

    public void toggleEditMode() {
        setEditMode(!isEditMode);
    }

    public void removeItem(int position) {
        if (position >= 0 && position < list.size()) {
            list.remove(position);
            notifyItemRemoved(position);
            notifyItemRangeChanged(position, list.size() - position);
        }
    }

    public void setEnableAnimation(boolean enable) {
        this.enableAnimation = enable;
    }

    public void resetAnimation() {
        lastAnimatedPosition = -1;
    }

    @NonNull
    @Override
    public SheetMusicViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View view = inflater.inflate(R.layout.list_musics_in_sheet, parent, false);
        return new SheetMusicViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SheetMusicViewHolder holder, int position) {
        MusicBean item = list.get(position);
        String path = item.getPath();

        MusicMetadataUtil.MusicInfo info = MusicMetadataUtil.getMusicInfo(path);

        holder.index.setText(String.valueOf(position + 1));
        holder.music_name.setText(info.getTitle() != null ? info.getTitle() : item.getName());
        holder.artist_name.setText(info.getArtist() != null ? info.getArtist() : item.getSinger());

        if (info.getCoverBitmap() != null) {
            holder.cover.setImageBitmap(info.getCoverBitmap());
        } else {
            holder.cover.setImageResource(R.drawable.music_cover);
        }

        // 设置 interaction 按钮
        setupInteractionButton(holder, position, item);

        holder.cover.setOnClickListener(v -> {
            if (!isEditMode) {
                Intent intent = new Intent(holder.itemView.getContext(), RunMusicDetailActivity.class);
                intent.putExtra("musicPath", path);
                intent.putExtra("musicId", item.getId());
                holder.itemView.getContext().startActivity(intent);

                MusicBean music = new MusicBean();
                music.setId(item.getId());
                music.setPath(path);
                MusicPlayerManager.getInstance().playSingle(music);
            }
        });

        holder.music_name.setOnClickListener(v -> {
            if (!isEditMode) {
                MusicBean music = new MusicBean();
                music.setId(item.getId());
                music.setPath(path);
                MusicPlayerManager.getInstance().playSingle(music);
            }
        });

        if (enableAnimation && position > lastAnimatedPosition) {
            lastAnimatedPosition = position;
            animateItem(holder.itemView, position);
        }
    }

    /**
     * 设置 interaction 按钮 - 核心修改部分
     */
    private void setupInteractionButton(SheetMusicViewHolder holder, int position, MusicBean item) {
        if (holder.interaction == null) return;

        if (isEditMode) {
            // 编辑模式：旋转45度，点击删除
            holder.interaction.setVisibility(View.VISIBLE);
            holder.interaction.animate()
                    .rotation(45f)
                    .setDuration(300)
                    .start();

            holder.interaction.setOnClickListener(v -> {
                if (deleteListener != null) {
                    deleteListener.onMusicDelete(position, item);
                }
            });
        } else {
            // 非编辑模式：恢复原状，点击显示添加到歌单弹窗
            holder.interaction.setVisibility(View.VISIBLE);
            holder.interaction.animate()
                    .rotation(0f)
                    .setDuration(300)
                    .start();

            holder.interaction.setOnClickListener(v -> {
                // ★ 关键代码：显示添加到歌单的弹窗
                showAddToSheetDialog(holder.itemView, item);
            });
        }
    }

    /**
     * 显示添加到歌单的底部弹窗
     * 自动从 View 的 Context 获取 FragmentActivity
     */
    private void showAddToSheetDialog(View view, MusicBean music) {
        // 从 View 的 Context 获取 FragmentActivity
        if (view.getContext() instanceof FragmentActivity) {
            FragmentActivity activity = (FragmentActivity) view.getContext();

            // 使用静态方法显示弹窗
            AddToSheetBottomSheet.show(activity, music);
        }
    }

    private void animateItem(View view, int position) {
        view.setAlpha(0f);
        view.setTranslationY(40f);
        view.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(250)
                .setStartDelay(position * 40L)
                .start();
    }

    @Override
    public int getItemCount() {
        return list == null ? 0 : list.size();
    }

    static class SheetMusicViewHolder extends RecyclerView.ViewHolder {
        TextView index;
        TextView music_name;
        TextView artist_name;
        ImageView cover;
        ImageView interaction;

        public SheetMusicViewHolder(@NonNull View itemView) {
            super(itemView);
            index = itemView.findViewById(R.id.tv_index);
            music_name = itemView.findViewById(R.id.tv_song_name);
            artist_name = itemView.findViewById(R.id.tv_artist);
            cover = itemView.findViewById(R.id.iv_cover);
            interaction = itemView.findViewById(R.id.iv_interaction);
        }
    }
}