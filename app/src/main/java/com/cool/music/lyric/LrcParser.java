package com.cool.music.lyric;

import android.text.TextUtils;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * LRC 歌词解析器
 * 支持标准 LRC 格式：[mm:ss.xx] 歌词内容
 * 支持自动检测编码（UTF-8、GBK、GB2312）
 */
public class LrcParser {

    /**
     * 歌词行数据类
     */
    public static class LrcLine implements Comparable<LrcLine> {
        private long time;      // 时间戳（毫秒）
        private String text;    // 歌词文本

        public LrcLine(long time, String text) {
            this.time = time;
            this.text = text;
        }

        public long getTime() { return time; }
        public String getText() { return text; }

        @Override
        public int compareTo(LrcLine other) {
            return Long.compare(this.time, other.time);
        }
    }

    /**
     * 歌词元信息
     */
    public static class LrcMetadata {
        public String title;    // [ti:标题]
        public String artist;   // [ar:艺术家]
        public String album;    // [al:专辑]
        public int offset;      // [offset:偏移量] 毫秒
    }

    // 时间标签正则：[mm:ss.xx] 或 [mm:ss:xx] 或 [mm:ss]
    private static final Pattern TIME_PATTERN =
            Pattern.compile("\\[(\\d{1,2}):(\\d{1,2})([.:]?(\\d{1,3}))?\\]");

    // 元信息标签正则
    private static final Pattern META_PATTERN =
            Pattern.compile("\\[(ti|ar|al|by|offset):(.*)\\]");

    private List<LrcLine> lrcLines = new ArrayList<>();
    private LrcMetadata metadata = new LrcMetadata();

    /**
     * 从文件路径解析歌词
     * @param lrcPath LRC 文件路径
     * @return 是否解析成功
     */
    public boolean parseFromFile(String lrcPath) {
        if (TextUtils.isEmpty(lrcPath)) return false;

        File file = new File(lrcPath);
        if (!file.exists()) return false;

        try {
            // 关键修复：自动检测文件编码
            String charset = detectCharset(file);

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(new FileInputStream(file), charset))) {

                lrcLines.clear();
                String line;

                while ((line = reader.readLine()) != null) {
                    parseLine(line);
                }

                // 按时间排序
                Collections.sort(lrcLines);
                return !lrcLines.isEmpty();
            }

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 检测文件编码
     * 通过读取文件头的BOM或分析字节特征来判断编码
     *
     * @param file 文件对象
     * @return 检测到的编码名称
     */
    private String detectCharset(File file) {
        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] bytes = new byte[Math.min((int) file.length(), 8192)];
            int len = fis.read(bytes);

            if (len < 3) return "UTF-8";

            // 检查 UTF-8 BOM
            if (bytes[0] == (byte) 0xEF && bytes[1] == (byte) 0xBB && bytes[2] == (byte) 0xBF) {
                return "UTF-8";
            }

            // 检查 UTF-16 LE BOM
            if (bytes[0] == (byte) 0xFF && bytes[1] == (byte) 0xFE) {
                return "UTF-16LE";
            }

            // 检查 UTF-16 BE BOM
            if (bytes[0] == (byte) 0xFE && bytes[1] == (byte) 0xFF) {
                return "UTF-16BE";
            }

            // 无BOM，分析内容判断是 UTF-8 还是 GBK
            if (isValidUtf8(bytes, len)) {
                // 进一步验证：如果能正确解析为UTF-8且包含中文，就是UTF-8
                String testStr = new String(bytes, 0, len, "UTF-8");
                if (containsChinese(testStr) && !testStr.contains("�")) {
                    return "UTF-8";
                }
            }

            // 默认尝试 GBK（大多数中文歌词文件使用此编码）
            return "GBK";

        } catch (Exception e) {
            return "UTF-8";
        }
    }

    /**
     * 检查字节数组是否为有效的 UTF-8 编码
     */
    private boolean isValidUtf8(byte[] bytes, int len) {
        int i = 0;
        while (i < len) {
            int b = bytes[i] & 0xFF;

            if (b < 0x80) {
                // ASCII 字符
                i++;
            } else if ((b & 0xE0) == 0xC0) {
                // 2字节 UTF-8
                if (i + 1 >= len) return false;
                if ((bytes[i + 1] & 0xC0) != 0x80) return false;
                i += 2;
            } else if ((b & 0xF0) == 0xE0) {
                // 3字节 UTF-8（中文通常在这里）
                if (i + 2 >= len) return false;
                if ((bytes[i + 1] & 0xC0) != 0x80) return false;
                if ((bytes[i + 2] & 0xC0) != 0x80) return false;
                i += 3;
            } else if ((b & 0xF8) == 0xF0) {
                // 4字节 UTF-8
                if (i + 3 >= len) return false;
                if ((bytes[i + 1] & 0xC0) != 0x80) return false;
                if ((bytes[i + 2] & 0xC0) != 0x80) return false;
                if ((bytes[i + 3] & 0xC0) != 0x80) return false;
                i += 4;
            } else {
                // 无效的 UTF-8 起始字节
                return false;
            }
        }
        return true;
    }

    /**
     * 检查字符串是否包含中文字符
     */
    private boolean containsChinese(String str) {
        if (str == null) return false;
        for (char c : str.toCharArray()) {
            if (c >= 0x4E00 && c <= 0x9FFF) {
                return true;
            }
        }
        return false;
    }

    /**
     * 从字符串解析歌词
     * @param lrcContent LRC 内容字符串
     */
    public boolean parseFromString(String lrcContent) {
        if (TextUtils.isEmpty(lrcContent)) return false;

        lrcLines.clear();
        String[] lines = lrcContent.split("\n");

        for (String line : lines) {
            parseLine(line);
        }

        Collections.sort(lrcLines);
        return !lrcLines.isEmpty();
    }

    /**
     * 解析单行歌词
     */
    private void parseLine(String line) {
        if (TextUtils.isEmpty(line)) return;

        line = line.trim();

        // 尝试解析元信息
        Matcher metaMatcher = META_PATTERN.matcher(line);
        if (metaMatcher.find()) {
            String tag = metaMatcher.group(1);
            String value = metaMatcher.group(2).trim();

            switch (tag) {
                case "ti": metadata.title = value; break;
                case "ar": metadata.artist = value; break;
                case "al": metadata.album = value; break;
                case "offset":
                    try { metadata.offset = Integer.parseInt(value); }
                    catch (NumberFormatException ignored) {}
                    break;
            }
            return;
        }

        // 解析时间标签和歌词
        Matcher timeMatcher = TIME_PATTERN.matcher(line);
        List<Long> times = new ArrayList<>();
        int lastEnd = 0;

        // 一行可能有多个时间标签：[00:01.00][00:15.00]歌词
        while (timeMatcher.find()) {
            times.add(parseTime(timeMatcher));
            lastEnd = timeMatcher.end();
        }

        if (!times.isEmpty() && lastEnd < line.length()) {
            String text = line.substring(lastEnd).trim();

            // 为每个时间标签创建歌词行
            for (Long time : times) {
                // 应用偏移量
                long adjustedTime = time + metadata.offset;
                if (adjustedTime >= 0) {
                    lrcLines.add(new LrcLine(adjustedTime, text));
                }
            }
        }
    }

    /**
     * 解析时间标签为毫秒
     */
    private long parseTime(Matcher matcher) {
        int minutes = Integer.parseInt(matcher.group(1));
        int seconds = Integer.parseInt(matcher.group(2));
        int millis = 0;

        String msGroup = matcher.group(4);
        if (msGroup != null) {
            // 处理不同精度：.xx (百分秒) 或 .xxx (毫秒)
            if (msGroup.length() == 2) {
                millis = Integer.parseInt(msGroup) * 10;
            } else if (msGroup.length() == 3) {
                millis = Integer.parseInt(msGroup);
            } else if (msGroup.length() == 1) {
                millis = Integer.parseInt(msGroup) * 100;
            }
        }

        return minutes * 60 * 1000L + seconds * 1000L + millis;
    }

    /**
     * 根据播放时间获取当前歌词索引
     * @param currentTime 当前播放时间（毫秒）
     * @return 当前歌词索引，-1 表示还没到第一句
     */
    public int getCurrentLineIndex(long currentTime) {
        if (lrcLines.isEmpty()) return -1;

        // 二分查找
        int left = 0, right = lrcLines.size() - 1;

        while (left <= right) {
            int mid = (left + right) / 2;
            long midTime = lrcLines.get(mid).getTime();

            if (midTime <= currentTime) {
                // 检查是否是最后一个或下一个时间还没到
                if (mid == lrcLines.size() - 1 ||
                        lrcLines.get(mid + 1).getTime() > currentTime) {
                    return mid;
                }
                left = mid + 1;
            } else {
                right = mid - 1;
            }
        }

        return -1; // 还没到第一句歌词
    }

    /**
     * 获取当前歌词行
     */
    public LrcLine getCurrentLine(long currentTime) {
        int index = getCurrentLineIndex(currentTime);
        return index >= 0 ? lrcLines.get(index) : null;
    }

    /**
     * 获取所有歌词行
     */
    public List<LrcLine> getLrcLines() {
        return lrcLines;
    }

    /**
     * 获取元信息
     */
    public LrcMetadata getMetadata() {
        return metadata;
    }

    /**
     * 是否有歌词
     */
    public boolean hasLyrics() {
        return !lrcLines.isEmpty();
    }

    /**
     * 清空歌词
     */
    public void clear() {
        lrcLines.clear();
        metadata = new LrcMetadata();
    }

    /**
     * 根据音乐路径推断歌词路径
     * 例如：/music/song.mp3 -> /music/song.lrc
     */
    public static String getLrcPathFromMusicPath(String musicPath) {
        if (TextUtils.isEmpty(musicPath)) return null;

        int dotIndex = musicPath.lastIndexOf('.');
        if (dotIndex > 0) {
            return musicPath.substring(0, dotIndex) + ".lrc";
        }
        return musicPath + ".lrc";
    }

    /**
     * 直接加载歌词文件
     * @param lyricPath 歌词文件路径
     * @return 解析成功返回 true
     */
    public boolean loadLyric(String lyricPath) {
        return parseFromFile(lyricPath);
    }
}