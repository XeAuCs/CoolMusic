package com.cool.music.adapter.user;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.ColorUtils;
import androidx.palette.graphics.Palette;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.cool.music.R;
import com.cool.music.activity.user.MusicListActivity;
import com.cool.music.bean.SheetBean;
import com.cool.music.dao.SheetDao;

import java.util.List;

public class PlayMusicAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_NORMAL = 0;
    private static final int TYPE_ADD = 1;

    private final List<SheetBean> list;
    private OnSheetLongClickListener longClickListener;
    private OnAddSheetClickListener addSheetClickListener;
    private boolean showAddButton = false;

    public interface OnSheetLongClickListener {
        void onSheetLongClick(SheetBean sheet, int position);
    }

    public interface OnAddSheetClickListener {
        void onAddSheetClick();
    }

    public PlayMusicAdapter(List<SheetBean> list) {
        this.list = list;
    }

    public void setShowAddButton(boolean show) {
        this.showAddButton = show;
        notifyDataSetChanged();
    }

    public void setOnSheetLongClickListener(OnSheetLongClickListener listener) {
        this.longClickListener = listener;
    }

    public void setOnAddSheetClickListener(OnAddSheetClickListener listener) {
        this.addSheetClickListener = listener;
    }

    public void removeItem(int position) {
        if (position >= 0 && position < list.size()) {
            list.remove(position);
            notifyItemRemoved(position);
            notifyItemRangeChanged(position, getItemCount());
        }
    }

    @Override
    public int getItemViewType(int position) {
        if (showAddButton && position == list.size()) {
            return TYPE_ADD;
        }
        return TYPE_NORMAL;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == TYPE_ADD) {
            View view = inflater.inflate(R.layout.list_user_add_sheet, parent, false);
            return new AddSheetViewHolder(view);
        } else {
            View view = inflater.inflate(R.layout.list_user_play_music, parent, false);
            return new PlayMusicViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof AddSheetViewHolder) {
            holder.itemView.setOnClickListener(v -> {
                if (addSheetClickListener != null) {
                    addSheetClickListener.onAddSheetClick();
                }
            });
        } else if (holder instanceof PlayMusicViewHolder) {
            PlayMusicViewHolder musicHolder = (PlayMusicViewHolder) holder;
            SheetBean item = list.get(position);

            musicHolder.sheet_name.setText(item.getName());
            musicHolder.favorite_count.setText(item.getCollect_count());
            musicHolder.play_count.setText(item.getPlay_count());
            String songCount = SheetDao.getSongCount(item.getId());
            musicHolder.song_count.setText(songCount);

            // 使用 Glide 加载图片并提取颜色
            Glide.with(musicHolder.view.getContext())
                    .asBitmap()
                    .load(item.getCover_image())
                    .into(new CustomTarget<Bitmap>() {
                        @Override
                        public void onResourceReady(@NonNull Bitmap bitmap,
                                                    @Nullable Transition<? super Bitmap> transition) {
                            // 设置图片
                            musicHolder.cover.setImageBitmap(bitmap);

                            // 提取颜色并调整文字颜色
                            extractColorAndSetTextColor(bitmap, musicHolder);
                        }

                        @Override
                        public void onLoadCleared(@Nullable Drawable placeholder) {
                            // 加载清除时恢复默认颜色
                            setTextColor(musicHolder, Color.BLACK);
                        }

                        @Override
                        public void onLoadFailed(@Nullable Drawable errorDrawable) {
                            // 加载失败时使用默认黑色
                            setTextColor(musicHolder, Color.BLACK);
                        }
                    });

            musicHolder.itemView.setOnLongClickListener(v -> {
                if (longClickListener != null) {
                    longClickListener.onSheetLongClick(item, musicHolder.getAdapterPosition());
                    return true;
                }
                return false;
            });

            musicHolder.itemView.setOnClickListener(v -> {
                String sheetId = item.getId();
                Intent intent = new Intent(musicHolder.view.getContext(), MusicListActivity.class);
                intent.putExtra("sheetId", sheetId);
                musicHolder.view.getContext().startActivity(intent);
            });
        }
    }

    /**
     * 提取图片颜色并设置文字颜色
     */
    private void extractColorAndSetTextColor(Bitmap bitmap, PlayMusicViewHolder holder) {
        Palette.from(bitmap).generate(palette -> {
            if (palette != null) {
                int dominantColor = palette.getDominantColor(Color.WHITE);
                int textColor = isColorDark(dominantColor) ? Color.WHITE : Color.BLACK;

                // 设置半透明（透明度约70%）
                int alphaTextColor = ColorUtils.setAlphaComponent(textColor, 180);
                setTextColor(holder, alphaTextColor);
            }
        });
    }

    /**
     * 判断颜色是否为深色
     * 使用相对亮度公式：L = 0.299*R + 0.587*G + 0.114*B
     */
    private boolean isColorDark(int color) {
        double luminance = (0.299 * Color.red(color)
                + 0.587 * Color.green(color)
                + 0.114 * Color.blue(color)) / 255;
        return luminance < 0.5;
    }

    /**
     * 设置所有文字的颜色
     */
    private void setTextColor(PlayMusicViewHolder holder, int color) {
        holder.sheet_name.setTextColor(color);
        holder.favorite_count.setTextColor(color);
        holder.play_count.setTextColor(color);
        holder.song_count.setTextColor(color);
    }

    @Override
    public int getItemCount() {
        int count = list.size();
        if (showAddButton) {
            count += 1;
        }
        return count;
    }

    static class PlayMusicViewHolder extends RecyclerView.ViewHolder {
        TextView sheet_name;
        TextView favorite_count;
        TextView play_count;
        TextView song_count;
        ImageView cover;
        View view;

        public PlayMusicViewHolder(@NonNull View itemView) {
            super(itemView);
            view = itemView;
            sheet_name = itemView.findViewById(R.id.tv_sheet_name);
            favorite_count = itemView.findViewById(R.id.tv_favorite_count);
            cover = itemView.findViewById(R.id.iv_cover);
            play_count = itemView.findViewById(R.id.tv_play_count);
            song_count = itemView.findViewById(R.id.tv_song_count);
        }
    }

    static class AddSheetViewHolder extends RecyclerView.ViewHolder {
        public AddSheetViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }
}