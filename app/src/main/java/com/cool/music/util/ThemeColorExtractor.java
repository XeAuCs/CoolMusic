package com.cool.music.util;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

/**
 * 主题色提取工具类
 * 使用 K-Means 聚类算法从 Bitmap 中提取主题色
 *
 * 并行化改造说明：
 * - 并行模型：共享内存模型（Shared Memory），基于 Java ForkJoinPool
 * - 并行粒度：数据并行（Data Parallelism），对像素集合进行分块处理
 * - 核心改进：K-Means 赋值步骤（最耗时）由串行遍历改为多线程并行计算
 * - 采样阶段：由串行逐像素读取改为批量读取 + 并行过滤
 */
public class ThemeColorExtractor {

    private static final String TAG = "ThemeColorExtractor";

    private static final int MAX_ITERATIONS = 20;   // 最大迭代次数
    private static final int SAMPLE_SIZE = 10000;   // 采样像素数量
    private static final int MIN_BRIGHTNESS = 20;   // 最小亮度阈值
    private static final int MAX_BRIGHTNESS = 235;  // 最大亮度阈值

    // ForkJoin 任务分割阈值：子任务像素数小于此值时直接串行处理
    private static final int FORK_THRESHOLD = 500;

    // ==================== 对外接口 ====================

    /**
     * 【串行版本】从 Bitmap 中提取 n 个主题色（原始实现，保留用于对比）
     */
    public static String[] extractThemeColors(Bitmap bitmap, int n) {
        if (bitmap == null || n <= 0) return new String[0];

        List<int[]> pixels = samplePixels(bitmap);
        if (pixels.isEmpty()) return new String[0];

        List<int[]> centroids = kMeansClustering(pixels, n);

        centroids.sort((a, b) -> {
            float ba = 0.299f * a[0] + 0.587f * a[1] + 0.114f * a[2];
            float bb = 0.299f * b[0] + 0.587f * b[1] + 0.114f * b[2];
            return Float.compare(bb, ba);
        });

        String[] result = new String[centroids.size()];
        for (int i = 0; i < centroids.size(); i++) {
            int[] rgb = centroids.get(i);
            result[i] = String.format("#%02X%02X%02X", rgb[0], rgb[1], rgb[2]);
        }
        return result;
    }

    /**
     * 【并行版本】从 Bitmap 中提取 n 个主题色
     *
     * 并行化改进点：
     * 1. samplePixelsParallel  - 批量读取像素 + 并行流过滤，减少 getPixel 调用开销
     * 2. kMeansClusteringParallel - 赋值步骤（Assignment Step）使用 ForkJoinPool 并行化
     */
    public static String[] extractThemeColorsParallel(Bitmap bitmap, int n) {
        if (bitmap == null || n <= 0) return new String[0];

        // 改进1：并行采样
        List<int[]> pixels = samplePixelsParallel(bitmap);
        if (pixels.isEmpty()) return new String[0];

        // 改进2：并行 K-Means 聚类
        List<int[]> centroids = kMeansClusteringParallel(pixels, n);

        centroids.sort((a, b) -> {
            float ba = 0.299f * a[0] + 0.587f * a[1] + 0.114f * a[2];
            float bb = 0.299f * b[0] + 0.587f * b[1] + 0.114f * b[2];
            return Float.compare(bb, ba);
        });

        String[] result = new String[centroids.size()];
        for (int i = 0; i < centroids.size(); i++) {
            int[] rgb = centroids.get(i);
            result[i] = String.format("#%02X%02X%02X", rgb[0], rgb[1], rgb[2]);
        }
        return result;
    }

    /**
     * 性能对比基准测试：自动运行串行和并行版本，打印耗时对比
     *
     * @param bitmap 测试图片
     * @param n      提取颜色数量
     * @param repeat 重复测试次数（取平均值）
     * @return BenchmarkResult 含串行/并行耗时及加速比
     */
    public static BenchmarkResult benchmark(Bitmap bitmap, int n, int repeat) {
        long serialTotal = 0;
        long parallelTotal = 0;

        // 预热（避免 JIT 编译影响第一次结果）
        extractThemeColors(bitmap, n);
        extractThemeColorsParallel(bitmap, n);

        for (int i = 0; i < repeat; i++) {
            long t0 = System.currentTimeMillis();
            extractThemeColors(bitmap, n);
            serialTotal += System.currentTimeMillis() - t0;

            long t1 = System.currentTimeMillis();
            extractThemeColorsParallel(bitmap, n);
            parallelTotal += System.currentTimeMillis() - t1;
        }

        long serialAvg = serialTotal / repeat;
        long parallelAvg = parallelTotal / repeat;
        double speedup = (double) serialAvg / parallelAvg;

        Log.i(TAG, String.format(
                "性能对比 | 图片:%dx%d | 采样:%d | 迭代:%d次\n" +
                "  串行均值: %d ms\n  并行均值: %d ms\n  加速比: %.2fx\n  线程数: %d",
                bitmap.getWidth(), bitmap.getHeight(), SAMPLE_SIZE, repeat,
                serialAvg, parallelAvg, speedup,
                Runtime.getRuntime().availableProcessors()
        ));

        return new BenchmarkResult(serialAvg, parallelAvg, speedup,
                Runtime.getRuntime().availableProcessors());
    }

    /**
     * 基准测试结果封装
     */
    public static class BenchmarkResult {
        public final long serialMs;     // 串行平均耗时(ms)
        public final long parallelMs;   // 并行平均耗时(ms)
        public final double speedup;    // 加速比
        public final int threadCount;   // 可用线程数

        public BenchmarkResult(long serialMs, long parallelMs,
                               double speedup, int threadCount) {
            this.serialMs = serialMs;
            this.parallelMs = parallelMs;
            this.speedup = speedup;
            this.threadCount = threadCount;
        }

        @Override
        public String toString() {
            return String.format("串行:%dms | 并行:%dms | 加速比:%.2fx | 线程:%d",
                    serialMs, parallelMs, speedup, threadCount);
        }
    }

    // ==================== 串行内部实现（原始版本） ====================

    /**
     * 【串行】逐像素采样
     */
    private static List<int[]> samplePixels(Bitmap bitmap) {
        List<int[]> pixels = new ArrayList<>();
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int totalPixels = width * height;
        int step = Math.max(1, (int) Math.sqrt((double) totalPixels / SAMPLE_SIZE));

        for (int y = 0; y < height; y += step) {
            for (int x = 0; x < width; x += step) {
                int pixel = bitmap.getPixel(x, y);    // 逐个读取，较慢
                int alpha = Color.alpha(pixel);
                if (alpha < 128) continue;

                int r = Color.red(pixel);
                int g = Color.green(pixel);
                int b = Color.blue(pixel);
                float brightness = 0.299f * r + 0.587f * g + 0.114f * b;
                if (brightness >= MIN_BRIGHTNESS && brightness <= MAX_BRIGHTNESS) {
                    pixels.add(new int[]{r, g, b});
                }
            }
        }
        return pixels;
    }

    /**
     * 【串行】K-Means 聚类主循环
     */
    private static List<int[]> kMeansClustering(List<int[]> pixels, int k) {
        if (pixels.size() < k) k = pixels.size();

        List<int[]> centroids = initializeCentroids(pixels, k);
        int[] assignments = new int[pixels.size()];

        for (int iteration = 0; iteration < MAX_ITERATIONS; iteration++) {
            boolean changed = false;

            // 赋值步骤（串行）：每个像素逐一找最近中心 —— 瓶颈所在
            for (int i = 0; i < pixels.size(); i++) {
                int nearest = findNearestCentroid(pixels.get(i), centroids);
                if (nearest != assignments[i]) {
                    assignments[i] = nearest;
                    changed = true;
                }
            }

            if (!changed) break;

            centroids = recalculateCentroids(pixels, assignments, k);
        }

        return centroids;
    }

    // ==================== 并行内部实现（改进版本） ====================

    /**
     * 【并行改进1】批量读取像素 + 并行流过滤
     *
     * 关键改进：
     * - 原版用 bitmap.getPixel(x,y) 逐个读取，JNI 调用开销大
     * - 改为 bitmap.getPixels() 一次性批量读入 int[]，再用并行流过滤
     * - 过滤计算（亮度判断）并行执行，多核利用率显著提升
     */
    private static List<int[]> samplePixelsParallel(Bitmap bitmap) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int totalPixels = width * height;
        int step = Math.max(1, (int) Math.sqrt((double) totalPixels / SAMPLE_SIZE));

        // 一次性批量读取所有像素到内存数组（比逐个 getPixel 快约 10x）
        int[] allPixels = new int[totalPixels];
        bitmap.getPixels(allPixels, 0, width, 0, 0, width, height);

        // 收集采样坐标
        List<Integer> indices = new ArrayList<>();
        for (int y = 0; y < height; y += step) {
            for (int x = 0; x < width; x += step) {
                indices.add(y * width + x);
            }
        }

        // 并行流过滤：每个线程独立处理一段坐标，无共享写入冲突
        return indices.parallelStream()
                .map(idx -> {
                    int pixel = allPixels[idx];
                    if (Color.alpha(pixel) < 128) return null;
                    int r = Color.red(pixel);
                    int g = Color.green(pixel);
                    int b = Color.blue(pixel);
                    float brightness = 0.299f * r + 0.587f * g + 0.114f * b;
                    if (brightness < MIN_BRIGHTNESS || brightness > MAX_BRIGHTNESS) return null;
                    return new int[]{r, g, b};
                })
                .filter(p -> p != null)
                .collect(Collectors.toList());
    }

    /**
     * 【并行改进2】K-Means 赋值步骤使用 Arrays.parallelSetAll 并行化
     *
     * 并行模型：共享内存 + 数据并行
     * - centroidsSnap 数组只读共享，各线程无竞争
     * - newAssignments 各线程写不同下标，无竞争，无需加锁
     * - Arrays.parallelSetAll 内部使用 commonPool，比手写 ForkJoin 更轻量
     *
     * 复杂度：
     * - 串行：O(N × K) per iteration
     * - 并行：O(N × K / P) per iteration，P = 可用核心数
     */
    private static List<int[]> kMeansClusteringParallel(List<int[]> pixels, int k) {
        if (pixels.size() < k) k = pixels.size();

        List<int[]> centroids = initializeCentroids(pixels, k);
        int[] assignments = new int[pixels.size()];
        final int[][] pixelArray = pixels.toArray(new int[0][]);

        for (int iteration = 0; iteration < MAX_ITERATIONS; iteration++) {
            final int[][] centroidsSnap = centroids.toArray(new int[0][]);
            final int[] newAssignments = new int[pixelArray.length];

            // 并行赋值：每个索引 i 独立计算最近聚类中心，互不依赖
            Arrays.parallelSetAll(newAssignments,
                    i -> findNearestCentroidArray(pixelArray[i], centroidsSnap));

            if (Arrays.equals(assignments, newAssignments)) {
                Log.d(TAG, "K-Means 提前收敛，迭代次数：" + (iteration + 1));
                break;
            }
            System.arraycopy(newAssignments, 0, assignments, 0, assignments.length);
            centroids = recalculateCentroids(pixels, assignments, k);
        }

        return centroids;
    }

    // ==================== 共用工具方法 ====================

    /**
     * K-Means++ 初始化聚类中心（串行，只调用一次，开销可忽略）
     */
    private static List<int[]> initializeCentroids(List<int[]> pixels, int k) {
        List<int[]> centroids = new ArrayList<>();
        Random random = new Random(42);

        int[] first = pixels.get(random.nextInt(pixels.size()));
        centroids.add(new int[]{first[0], first[1], first[2]});

        double[] distances = new double[pixels.size()];
        for (int i = 1; i < k; i++) {
            double totalDistance = 0;
            for (int j = 0; j < pixels.size(); j++) {
                double minDist = Double.MAX_VALUE;
                for (int[] centroid : centroids) {
                    double dist = colorDistance(pixels.get(j), centroid);
                    minDist = Math.min(minDist, dist);
                }
                distances[j] = minDist * minDist;
                totalDistance += distances[j];
            }

            double threshold = random.nextDouble() * totalDistance;
            double sum = 0;
            int selected = 0;
            for (int j = 0; j < pixels.size(); j++) {
                sum += distances[j];
                if (sum >= threshold) { selected = j; break; }
            }
            int[] next = pixels.get(selected);
            centroids.add(new int[]{next[0], next[1], next[2]});
        }
        return centroids;
    }

    /**
     * 找最近聚类中心（List 版，供串行使用）
     */
    private static int findNearestCentroid(int[] pixel, List<int[]> centroids) {
        int nearest = 0;
        double minDistance = Double.MAX_VALUE;
        for (int i = 0; i < centroids.size(); i++) {
            double distance = colorDistance(pixel, centroids.get(i));
            if (distance < minDistance) {
                minDistance = distance;
                nearest = i;
            }
        }
        return nearest;
    }

    /**
     * 找最近聚类中心（数组版，供并行使用）
     * 用平方距离代替欧氏距离：比较大小时 sqrt 是单调的，省去开方不影响结果
     */
    private static int findNearestCentroidArray(int[] pixel, int[][] centroids) {
        int nearest = 0;
        long minDist = Long.MAX_VALUE;
        for (int i = 0; i < centroids.length; i++) {
            int dr = pixel[0] - centroids[i][0];
            int dg = pixel[1] - centroids[i][1];
            int db = pixel[2] - centroids[i][2];
            long dist = (long) dr * dr + (long) dg * dg + (long) db * db;
            if (dist < minDist) {
                minDist = dist;
                nearest = i;
            }
        }
        return nearest;
    }

    /**
     * 重新计算聚类中心均值
     */
    private static List<int[]> recalculateCentroids(List<int[]> pixels,
                                                     int[] assignments, int k) {
        List<int[]> newCentroids = new ArrayList<>();
        int[][] sums = new int[k][3];
        int[] counts = new int[k];

        for (int i = 0; i < pixels.size(); i++) {
            int cluster = assignments[i];
            int[] pixel = pixels.get(i);
            sums[cluster][0] += pixel[0];
            sums[cluster][1] += pixel[1];
            sums[cluster][2] += pixel[2];
            counts[cluster]++;
        }

        for (int i = 0; i < k; i++) {
            if (counts[i] > 0) {
                newCentroids.add(new int[]{
                        sums[i][0] / counts[i],
                        sums[i][1] / counts[i],
                        sums[i][2] / counts[i]
                });
            }
        }
        return newCentroids;
    }

    /**
     * RGB 欧氏距离
     */
    private static double colorDistance(int[] c1, int[] c2) {
        int dr = c1[0] - c2[0];
        int dg = c1[1] - c2[1];
        int db = c1[2] - c2[2];
        return Math.sqrt(dr * dr + dg * dg + db * db);
    }
}
