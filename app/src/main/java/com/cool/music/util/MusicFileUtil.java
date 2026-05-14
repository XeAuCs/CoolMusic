package com.cool.music.util;

import android.content.Context;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RawRes;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

/**
 * 音乐文件工具类
 * 使用 ExoPlayer 播放音乐，无需格式转换
 * 文件命名格式: 音乐名称 + 4位UUID + 原始后缀
 * 例如: 晴天_a1b2.flac
 *
 * 改进: 通过文件魔数(Magic Bytes)精确识别音频格式，优先保留FLAC等无损格式
 */
public class MusicFileUtil {

    private static final String TAG = "MusicFileUtil";
    private static final int UUID_LENGTH = 4;

    // 音频文件魔数定义
    private static final byte[] MAGIC_FLAC = {0x66, 0x4C, 0x61, 0x43}; // "fLaC"
    private static final byte[] MAGIC_OGG = {0x4F, 0x67, 0x67, 0x53};  // "OggS"
    private static final byte[] MAGIC_RIFF = {0x52, 0x49, 0x46, 0x46}; // "RIFF" (WAV)
    private static final byte[] MAGIC_ID3 = {0x49, 0x44, 0x33};        // "ID3" (MP3 with ID3 tag)
    private static final byte[] MAGIC_MP3_FF = {(byte) 0xFF, (byte) 0xFB}; // MP3 frame sync
    private static final byte[] MAGIC_MP3_FF_F3 = {(byte) 0xFF, (byte) 0xF3}; // MP3 frame sync variant
    private static final byte[] MAGIC_MP3_FF_F2 = {(byte) 0xFF, (byte) 0xF2}; // MP3 frame sync variant
    private static final byte[] MAGIC_M4A = {0x66, 0x74, 0x79, 0x70};  // "ftyp" (在偏移4处)

    public interface SaveCallback {
        void onSuccess(String path);
        void onFail(Exception e);
    }

    // ==================== 同步方法（适用于数据库初始化）====================

    /**
     * 同步保存 Raw 资源音乐文件（保留原始格式）
     * @return 保存成功返回路径，失败返回 null
     */
    public static String saveMusicSync(Context context, @RawRes int rawResId) {
        try {
            // 先复制到临时文件
            String tempExtension = ".tmp";
            String tempPath = copyRawToTempFile(context, rawResId, tempExtension);

            // 通过魔数检测真实格式
            String realExtension = detectAudioFormat(tempPath);
            Log.d(TAG, "检测到音频格式: " + realExtension);

            // 生成输出路径（使用检测到的真实扩展名）
            String outputPath = generateOutputPath(context, tempPath, null, realExtension);

            // 移动文件到最终位置
            moveFile(tempPath, outputPath);
            Log.d(TAG, "音乐保存成功: " + outputPath);
            return outputPath;

        } catch (Exception e) {
            Log.e(TAG, "同步保存音乐失败", e);
            return null;
        }
    }

    /**
     * 同步保存文件路径音乐（保留原始格式）
     */
    public static String saveMusicSync(Context context, String inputPath) {
        try {
            // 优先通过魔数检测格式，如果检测失败则使用路径扩展名
            String extension = detectAudioFormat(inputPath);
            if (extension == null) {
                extension = getExtensionFromPath(inputPath);
            }

            String outputPath = generateOutputPath(context, inputPath, null, extension);

            // 复制文件到目标位置
            copyFile(inputPath, outputPath);
            Log.d(TAG, "音乐保存成功: " + outputPath);
            return outputPath;

        } catch (Exception e) {
            Log.e(TAG, "同步保存音乐失败", e);
            return null;
        }
    }

    /**
     * 同步保存 Uri 音乐（保留原始格式）
     */
    public static String saveMusicSync(Context context, Uri uri) {
        try {
            // 先用临时扩展名复制文件
            String tempExtension = ".tmp";
            String tempPath = copyUriToTempFile(context, uri, tempExtension);

            // 通过魔数检测真实格式
            String extension = detectAudioFormat(tempPath);
            if (extension == null) {
                // 如果魔数检测失败，尝试从Uri获取
                extension = getExtensionFromUri(context, uri);
            }

            String outputPath = generateOutputPath(context, tempPath, uri, extension);

            // 移动临时文件到最终位置
            moveFile(tempPath, outputPath);
            Log.d(TAG, "音乐保存成功: " + outputPath);
            return outputPath;

        } catch (Exception e) {
            Log.e(TAG, "同步保存音乐失败", e);
            return null;
        }
    }

    // ==================== 异步方法 ====================

    /**
     * 异步保存 Raw 资源音乐文件
     */
    public static void saveMusic(Context context, @RawRes int rawResId, SaveCallback callback) {
        new Thread(() -> {
            String result = saveMusicSync(context, rawResId);
            if (result != null) {
                callback.onSuccess(result);
            } else {
                callback.onFail(new RuntimeException("保存音乐失败"));
            }
        }).start();
    }

    /**
     * 异步保存 Uri 音乐文件
     */
    public static void saveMusic(Context context, Uri uri, SaveCallback callback) {
        new Thread(() -> {
            String result = saveMusicSync(context, uri);
            if (result != null) {
                callback.onSuccess(result);
            } else {
                callback.onFail(new RuntimeException("保存音乐失败"));
            }
        }).start();
    }

    /**
     * 异步保存文件路径音乐
     */
    public static void saveMusic(Context context, String inputPath, SaveCallback callback) {
        new Thread(() -> {
            String result = saveMusicSync(context, inputPath);
            if (result != null) {
                callback.onSuccess(result);
            } else {
                callback.onFail(new RuntimeException("保存音乐失败"));
            }
        }).start();
    }

    // ==================== 音频格式检测（核心改进）====================

    /**
     * 通过读取文件头魔数检测音频格式
     * 这是最可靠的格式识别方式，不依赖文件扩展名或MIME类型
     *
     * @param filePath 文件路径
     * @return 检测到的扩展名（如 ".flac", ".mp3"），检测失败返回 null
     */
    @Nullable
    public static String detectAudioFormat(String filePath) {
        if (filePath == null) return null;

        File file = new File(filePath);
        if (!file.exists() || !file.canRead()) return null;

        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] header = new byte[12]; // 读取前12字节足以识别大多数格式
            int bytesRead = fis.read(header);

            if (bytesRead < 4) return null;

            return detectFormatFromHeader(header, bytesRead);

        } catch (IOException e) {
            Log.w(TAG, "读取文件头失败: " + e.getMessage());
            return null;
        }
    }

    /**
     * 通过 InputStream 检测音频格式
     */
    @Nullable
    public static String detectAudioFormat(InputStream is) {
        if (is == null) return null;

        try {
            byte[] header = new byte[12];
            is.mark(12);
            int bytesRead = is.read(header);
            is.reset();

            if (bytesRead < 4) return null;

            return detectFormatFromHeader(header, bytesRead);

        } catch (IOException e) {
            Log.w(TAG, "从流读取文件头失败: " + e.getMessage());
            return null;
        }
    }

    /**
     * 根据文件头字节判断格式
     */
    private static String detectFormatFromHeader(byte[] header, int length) {
        // FLAC: 以 "fLaC" 开头
        if (length >= 4 && matchesMagic(header, 0, MAGIC_FLAC)) {
            return ".flac";
        }

        // OGG: 以 "OggS" 开头 (可能是 OGG Vorbis 或 OGG FLAC)
        if (length >= 4 && matchesMagic(header, 0, MAGIC_OGG)) {
            return ".ogg";
        }

        // WAV: 以 "RIFF" 开头
        if (length >= 4 && matchesMagic(header, 0, MAGIC_RIFF)) {
            // 进一步检查是否为 WAVE 格式
            if (length >= 12) {
                byte[] wave = {0x57, 0x41, 0x56, 0x45}; // "WAVE"
                if (matchesMagic(header, 8, wave)) {
                    return ".wav";
                }
            }
            return ".wav"; // 假设 RIFF 就是 WAV
        }

        // MP3 with ID3 tag: 以 "ID3" 开头
        if (length >= 3 && matchesMagic(header, 0, MAGIC_ID3)) {
            return ".mp3";
        }

        // MP3 frame sync: 以 0xFF 0xFB/F3/F2 开头
        if (length >= 2) {
            if (matchesMagic(header, 0, MAGIC_MP3_FF) ||
                    matchesMagic(header, 0, MAGIC_MP3_FF_F3) ||
                    matchesMagic(header, 0, MAGIC_MP3_FF_F2)) {
                return ".mp3";
            }
        }

        // M4A/AAC/MP4: 在偏移4处有 "ftyp"
        if (length >= 8 && matchesMagic(header, 4, MAGIC_M4A)) {
            // 检查具体的 ftyp 类型
            if (length >= 12) {
                // M4A 常见的 ftyp: M4A , M4B , mp42, isom
                byte[] m4a = {0x4D, 0x34, 0x41, 0x20}; // "M4A "
                byte[] m4b = {0x4D, 0x34, 0x42, 0x20}; // "M4B "
                if (matchesMagic(header, 8, m4a) || matchesMagic(header, 8, m4b)) {
                    return ".m4a";
                }
            }
            // 通用的 AAC/M4A 容器
            return ".m4a";
        }

        // AIFF: 以 "FORM" 开头，然后是 "AIFF"
        byte[] formMagic = {0x46, 0x4F, 0x52, 0x4D}; // "FORM"
        if (length >= 4 && matchesMagic(header, 0, formMagic)) {
            if (length >= 12) {
                byte[] aiff = {0x41, 0x49, 0x46, 0x46}; // "AIFF"
                if (matchesMagic(header, 8, aiff)) {
                    return ".aiff";
                }
            }
        }

        // WMA: ASF 格式，以特定GUID开头
        // ASF header GUID: 30 26 B2 75 8E 66 CF 11
        if (length >= 4 && header[0] == 0x30 && header[1] == 0x26 &&
                header[2] == (byte) 0xB2 && header[3] == 0x75) {
            return ".wma";
        }

        Log.d(TAG, "无法识别的音频格式，文件头: " + bytesToHex(header, Math.min(length, 8)));
        return null;
    }

    /**
     * 检查字节数组是否匹配魔数
     */
    private static boolean matchesMagic(byte[] data, int offset, byte[] magic) {
        if (data.length < offset + magic.length) return false;
        for (int i = 0; i < magic.length; i++) {
            if (data[offset + i] != magic[i]) return false;
        }
        return true;
    }

    /**
     * 字节数组转十六进制字符串（用于调试）
     */
    private static String bytesToHex(byte[] bytes, int length) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length && i < bytes.length; i++) {
            sb.append(String.format("%02X ", bytes[i]));
        }
        return sb.toString().trim();
    }

    // ==================== 私有工具方法 ====================

    /**
     * 复制 Raw 资源到临时文件
     */
    private static String copyRawToTempFile(Context context, @RawRes int rawResId, String extension) throws IOException {
        File tempFile = new File(context.getCacheDir(),
                "temp_" + UUID.randomUUID().toString().substring(0, 8) + extension);

        try (InputStream is = context.getResources().openRawResource(rawResId);
             FileOutputStream fos = new FileOutputStream(tempFile)) {
            byte[] buffer = new byte[8192];
            int len;
            while ((len = is.read(buffer)) != -1) {
                fos.write(buffer, 0, len);
            }
            fos.flush();
        }

        return tempFile.getAbsolutePath();
    }

    /**
     * 复制 Uri 到临时文件
     */
    private static String copyUriToTempFile(Context context, Uri uri, String extension) throws IOException {
        File tempFile = new File(context.getCacheDir(),
                "temp_" + UUID.randomUUID().toString().substring(0, 8) + extension);

        try (InputStream is = context.getContentResolver().openInputStream(uri);
             FileOutputStream fos = new FileOutputStream(tempFile)) {
            if (is == null) {
                throw new IOException("无法打开 Uri: " + uri);
            }
            byte[] buffer = new byte[8192];
            int len;
            while ((len = is.read(buffer)) != -1) {
                fos.write(buffer, 0, len);
            }
            fos.flush();
        }

        return tempFile.getAbsolutePath();
    }

    /**
     * 生成输出文件路径
     * 格式: 音乐名称_4位UUID.原始后缀 (例如: 晴天_a1b2.flac)
     *
     * @param context   上下文
     * @param inputPath 输入文件路径（用于提取元数据或文件名）
     * @param uri       输入 Uri（可选，优先用于提取元数据）
     * @param extension 文件扩展名（包含点，如 ".flac"）
     * @return 输出文件的完整路径
     */
    private static String generateOutputPath(Context context, @Nullable String inputPath,
                                             @Nullable Uri uri, String extension) {
        File dir = getMusicDir(context);
        String musicName = extractMusicName(context, inputPath, uri);

        // 使用4位 UUID
        String shortUuid = UUID.randomUUID().toString().substring(0, UUID_LENGTH);

        // 清理文件名中的非法字符
        String safeName = sanitizeFileName(musicName);

        String fileName = safeName + "_" + shortUuid + extension;
        Log.d(TAG, "生成文件名: " + fileName);

        return new File(dir, fileName).getAbsolutePath();
    }

    /**
     * 提取音乐名称
     * 优先级: 元数据标题 > 文件名 > "unknown"
     */
    private static String extractMusicName(Context context, @Nullable String inputPath, @Nullable Uri uri) {
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        String title = null;

        try {
            // 优先使用 Uri 设置数据源（元数据更完整）
            if (uri != null) {
                retriever.setDataSource(context, uri);
            } else if (inputPath != null) {
                retriever.setDataSource(inputPath);
            }

            // 获取标题元数据
            title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE);
            Log.d(TAG, "从元数据提取标题: " + title);

            // 如果没有标题，尝试获取艺术家
            if (title == null || title.trim().isEmpty()) {
                String artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST);
                String album = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM);
                if (artist != null && !artist.trim().isEmpty()) {
                    title = artist.trim();
                } else if (album != null && !album.trim().isEmpty()) {
                    title = album.trim();
                }
            }

        } catch (Exception e) {
            Log.w(TAG, "提取音乐元数据失败: " + e.getMessage());
        } finally {
            try {
                retriever.release();
            } catch (Exception ignored) {
            }
        }

        // 如果元数据中没有标题，从文件名获取
        if (title == null || title.trim().isEmpty()) {
            title = getFileNameWithoutExtension(inputPath, uri);
            Log.d(TAG, "从文件名提取: " + title);
        }

        // 最后兜底
        if (title == null || title.trim().isEmpty()) {
            title = "unknown";
        }

        return title.trim();
    }

    /**
     * 从路径或 Uri 获取不带扩展名的文件名
     */
    @Nullable
    private static String getFileNameWithoutExtension(@Nullable String inputPath, @Nullable Uri uri) {
        String fileName = null;

        // 优先从路径获取
        if (inputPath != null) {
            fileName = new File(inputPath).getName();
        }
        // 其次从 Uri 获取
        else if (uri != null) {
            String path = uri.getLastPathSegment();
            if (path != null) {
                int lastSlash = path.lastIndexOf('/');
                if (lastSlash >= 0) {
                    fileName = path.substring(lastSlash + 1);
                } else {
                    fileName = path;
                }
            }
        }

        // 移除扩展名
        if (fileName != null && !fileName.isEmpty()) {
            // 移除临时文件前缀
            if (fileName.startsWith("temp_")) {
                int dotIndex = fileName.indexOf('.', 5);
                if (dotIndex > 0 && dotIndex < fileName.length() - 1) {
                    return null;
                }
            }

            int dotIndex = fileName.lastIndexOf('.');
            if (dotIndex > 0) {
                return fileName.substring(0, dotIndex);
            }
            return fileName;
        }

        return null;
    }

    /**
     * 清理文件名中的非法字符
     */
    @NonNull
    private static String sanitizeFileName(@Nullable String name) {
        if (name == null || name.trim().isEmpty()) {
            return "unknown";
        }

        // 移除文件系统非法字符
        String sanitized = name.replaceAll("[\\\\/:*?\"<>|]", "_");

        // 移除控制字符
        sanitized = sanitized.replaceAll("[\\x00-\\x1F\\x7F]", "");

        // 将多个连续下划线替换为单个
        sanitized = sanitized.replaceAll("_+", "_");

        // 移除首尾空格和下划线
        sanitized = sanitized.trim();
        sanitized = sanitized.replaceAll("^_+|_+$", "");

        // 限制长度
        if (sanitized.length() > 100) {
            sanitized = sanitized.substring(0, 100);
        }

        if (sanitized.isEmpty()) {
            return "unknown";
        }

        return sanitized;
    }

    /**
     * 获取音乐存储目录
     */
    private static File getMusicDir(Context context) {
        File dir = context.getExternalFilesDir(Environment.DIRECTORY_MUSIC);
        if (dir == null) {
            dir = new File(context.getFilesDir(), "music");
        }
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return dir;
    }

    /**
     * 从文件路径获取扩展名（作为备选方案）
     */
    private static String getExtensionFromPath(String path) {
        if (path == null) return ".mp3";
        int dotIndex = path.lastIndexOf('.');
        if (dotIndex > 0 && dotIndex < path.length() - 1) {
            String ext = path.substring(dotIndex).toLowerCase();
            // 过滤临时扩展名
            if (!ext.equals(".tmp")) {
                return ext;
            }
        }
        return ".mp3";
    }

    /**
     * 从 Uri 获取扩展名（作为备选方案）
     */
    private static String getExtensionFromUri(Context context, Uri uri) {
        String mimeType = context.getContentResolver().getType(uri);
        if (mimeType != null) {
            // 优先检查无损格式
            if (mimeType.contains("flac")) return ".flac";
            if (mimeType.contains("wav") || mimeType.contains("wave")) return ".wav";
            if (mimeType.contains("aiff")) return ".aiff";
            // 有损格式
            if (mimeType.contains("mpeg") || mimeType.contains("mp3")) return ".mp3";
            if (mimeType.contains("ogg")) return ".ogg";
            if (mimeType.contains("aac")) return ".aac";
            if (mimeType.contains("m4a") || mimeType.contains("mp4")) return ".m4a";
            if (mimeType.contains("wma") || mimeType.contains("asf")) return ".wma";
        }

        // 尝试从 Uri 路径获取
        String path = uri.getLastPathSegment();
        if (path != null) {
            int dotIndex = path.lastIndexOf('.');
            if (dotIndex > 0 && dotIndex < path.length() - 1) {
                return path.substring(dotIndex).toLowerCase();
            }
        }

        return ".mp3";
    }

    /**
     * 复制文件
     */
    private static void copyFile(String srcPath, String destPath) throws IOException {
        File destFile = new File(destPath);
        File parent = destFile.getParentFile();
        if (parent != null && !parent.exists()) parent.mkdirs();

        try (InputStream is = new FileInputStream(srcPath);
             FileOutputStream fos = new FileOutputStream(destFile)) {
            byte[] buffer = new byte[8192];
            int len;
            while ((len = is.read(buffer)) != -1) {
                fos.write(buffer, 0, len);
            }
            fos.flush();
        }
    }

    /**
     * 移动文件
     */
    private static void moveFile(String srcPath, String destPath) throws IOException {
        File srcFile = new File(srcPath);
        File destFile = new File(destPath);

        File parent = destFile.getParentFile();
        if (parent != null && !parent.exists()) parent.mkdirs();

        if (!srcFile.renameTo(destFile)) {
            copyFile(srcPath, destPath);
            srcFile.delete();
        }
    }

    /**
     * 删除文件
     */
    public static void deleteFile(String path) {
        if (path != null) {
            File file = new File(path);
            if (file.exists()) {
                boolean deleted = file.delete();
                Log.d(TAG, "删除文件: " + path + ", 结果: " + deleted);
            }
        }
    }

    /**
     * 检查文件是否存在
     */
    public static boolean fileExists(String path) {
        if (path == null) return false;
        return new File(path).exists();
    }

    /**
     * 获取文件大小（字节）
     */
    public static long getFileSize(String path) {
        if (path == null) return 0;
        File file = new File(path);
        return file.exists() ? file.length() : 0;
    }

    /**
     * 保存 Raw 资源中的 LRC 歌词文件
     * @param context 上下文
     * @param rawResId raw 资源ID
     * @param musicName 关联的音乐名称（用于生成文件名）
     * @return 保存成功返回路径，失败返回 null
     */
    public static String saveLrcSync(Context context, @RawRes int rawResId, @Nullable String musicName) {
        try {
            File dir = getLrcDir(context);
            String name = (musicName != null && !musicName.trim().isEmpty())
                    ? sanitizeFileName(musicName)
                    : "lyric";
            String shortUuid = UUID.randomUUID().toString().substring(0, UUID_LENGTH);
            String fileName = name + "_" + shortUuid + ".lrc";
            File outputFile = new File(dir, fileName);

            try (InputStream is = context.getResources().openRawResource(rawResId);
                 FileOutputStream fos = new FileOutputStream(outputFile)) {
                byte[] buffer = new byte[4096];
                int len;
                while ((len = is.read(buffer)) != -1) {
                    fos.write(buffer, 0, len);
                }
                fos.flush();
            }

            Log.d(TAG, "LRC保存成功: " + outputFile.getAbsolutePath());
            return outputFile.getAbsolutePath();

        } catch (Exception e) {
            Log.e(TAG, "保存LRC失败", e);
            return null;
        }
    }

    /**
     * 获取歌词存储目录
     */
    private static File getLrcDir(Context context) {
        File dir = new File(context.getExternalFilesDir(null), "lyrics");
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return dir;
    }





}