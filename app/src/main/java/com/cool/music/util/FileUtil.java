package com.cool.music.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Environment;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.UUID;

public class FileUtil {

    public interface SaveCallback {
        void onSuccess(String path);
        void onFail(Exception e);
    }

    // 保存 Uri 图片
    public static void saveImage(Context context, Uri uri, SaveCallback callback) {
        String path = getRandomFileName(context);
        Glide.with(context)
                .asBitmap()
                .load(uri)
                .into(createTarget(path, callback));
    }

    // 保存 Drawable 资源图片
    public static void saveImage(Context context, int drawableResId, SaveCallback callback) {
        String path = getRandomFileName(context);
        Glide.with(context)
                .asBitmap()
                .load(drawableResId)
                .into(createTarget(path, callback));
    }

    // 生成随机文件路径
    private static String getRandomFileName(Context context) {
        File dir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        if (dir == null) dir = context.getFilesDir();
        return new File(dir, UUID.randomUUID().toString().replace("-", "") + ".png").getAbsolutePath();
    }

    // 创建 Glide 回调 Target
    private static CustomTarget<Bitmap> createTarget(String path, SaveCallback callback) {
        return new CustomTarget<Bitmap>() {
            @Override
            public void onResourceReady(@NonNull Bitmap resource,
                                        @Nullable Transition<? super Bitmap> transition) {
                writeBitmapAsync(resource, path, callback);
            }

            @Override
            public void onLoadCleared(@Nullable Drawable placeholder) {}

            @Override
            public void onLoadFailed(@Nullable Drawable errorDrawable) {
                callback.onFail(new RuntimeException("图片加载失败"));
            }
        };
    }

    // 异步写入 Bitmap
    private static void writeBitmapAsync(Bitmap bitmap, String path, SaveCallback callback) {
        new Thread(() -> {
            try {
                writeBitmap(bitmap, path);
                callback.onSuccess(path);
            } catch (Exception e) {
                callback.onFail(e);
            }
        }).start();
    }

    // 同步写入 Bitmap 到文件
    private static void writeBitmap(Bitmap bitmap, String path) throws IOException {
        if (bitmap == null || path == null) {
            throw new IOException("Bitmap 或路径为空");
        }

        File file = new File(path);
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }

        try (FileOutputStream fos = new FileOutputStream(file)) {
            boolean ok = bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.flush();
            if (!ok) {
                throw new IOException("Bitmap 压缩失败");
            }
        }
    }

    // 在你现有的 FileUtil 类中添加这个同步方法

    /**
     * 同步保存 Drawable 资源图片（不使用 Glide，直接写入）
     */
    public static String saveImageSync(Context context, int drawableResId) {
        try {
            String path = getRandomFileName(context);

            // 使用 BitmapFactory 直接加载资源
            Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), drawableResId);
            if (bitmap == null) {
                return null;
            }

            writeBitmap(bitmap, path);
            return path;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}