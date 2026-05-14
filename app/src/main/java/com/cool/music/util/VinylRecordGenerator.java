package com.cool.music.util;

import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;

/**
 * 唱片效果生成器
 * 将专辑封面转换为唱片样式的图片
 *
 * 使用示例：
 * Bitmap vinylBitmap = VinylRecordGenerator.generateVinylRecord(albumBitmap, "#FF5722", 500);
 */
public class VinylRecordGenerator {

    /**
     * 生成唱片效果图片
     *
     * @param albumBitmap   专辑封面图片
     * @param themeColorHex 主题色（十六进制字符串，如 "#FF5722" 或 "FF5722"）
     * @param outputSize    输出图片尺寸（正方形边长，单位像素）
     * @return 生成的唱片效果图片
     */
    public static Bitmap generateVinylRecord(Bitmap albumBitmap, String themeColorHex, int outputSize) {
        // 解析主题色
        int themeColor = parseColor(themeColorHex);

        // 创建输出位图
        Bitmap output = Bitmap.createBitmap(outputSize, outputSize, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);

        // 中心点和半径
        float centerX = outputSize / 2f;
        float centerY = outputSize / 2f;
        float recordRadius = outputSize / 2f;

        // 专辑封面占唱片的比例（中心区域）
        float albumRatio = 0.6f;
        float albumRadius = recordRadius * albumRatio;

        // 1. 绘制唱片底色（主题色渐变）
        drawRecordBase(canvas, centerX, centerY, recordRadius, themeColor);

        // 2. 绘制同心圆纹理
        drawGrooves(canvas, centerX, centerY, albumRadius, recordRadius, themeColor);

        // 3. 绘制光泽效果
        drawShineEffect(canvas, centerX, centerY, recordRadius, themeColor);

        // 4. 绘制圆形专辑封面
        drawCircularAlbum(canvas, albumBitmap, centerX, centerY, albumRadius);

        // 5. 绘制专辑封面边缘效果
        drawAlbumBorder(canvas, centerX, centerY, albumRadius, themeColor);

        return output;
    }

    /**
     * 生成唱片效果图片（可自定义专辑封面大小比例）
     *
     * @param albumBitmap   专辑封面图片
     * @param themeColorHex 主题色（十六进制字符串）
     * @param outputSize    输出图片尺寸
     * @param albumRatio    专辑封面占唱片的比例（0.0 - 1.0，推荐 0.3 - 0.5）
     * @return 生成的唱片效果图片
     */
    public static Bitmap generateVinylRecord(Bitmap albumBitmap, String themeColorHex,
                                             int outputSize, float albumRatio) {
        // 限制比例范围
        albumRatio = Math.max(0.2f, Math.min(0.6f, albumRatio));

        int themeColor = parseColor(themeColorHex);

        Bitmap output = Bitmap.createBitmap(outputSize, outputSize, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);

        float centerX = outputSize / 2f;
        float centerY = outputSize / 2f;
        float recordRadius = outputSize / 2f;
        float albumRadius = recordRadius * albumRatio;

        drawRecordBase(canvas, centerX, centerY, recordRadius, themeColor);
        drawGrooves(canvas, centerX, centerY, albumRadius, recordRadius, themeColor);
        drawShineEffect(canvas, centerX, centerY, recordRadius, themeColor);
        drawCircularAlbum(canvas, albumBitmap, centerX, centerY, albumRadius);
        drawAlbumBorder(canvas, centerX, centerY, albumRadius, themeColor);

        return output;
    }

    /**
     * 解析颜色字符串
     */
    private static int parseColor(String colorHex) {
        if (colorHex == null || colorHex.isEmpty()) {
            return Color.parseColor("#6200EE"); // 默认紫色
        }

        String hex = colorHex.trim();
        if (!hex.startsWith("#")) {
            hex = "#" + hex;
        }

        try {
            if (hex.length() == 7 || hex.length() == 9) {
                return Color.parseColor(hex);
            }
        } catch (IllegalArgumentException e) {
            // 解析失败，使用默认色
        }

        return Color.parseColor("#6200EE");
    }

    /**
     * 绘制唱片底色（径向渐变）
     */
    private static void drawRecordBase(Canvas canvas, float cx, float cy,
                                       float radius, int themeColor) {
        Paint basePaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        // 从主题色生成深浅变化
        int darkColor = darkenColor(themeColor, 0.3f);
        int lightColor = lightenColor(themeColor, 0.1f);
        int midColor = themeColor;
        int edgeColor = darkenColor(themeColor, 0.5f);

        // 创建径向渐变
        RadialGradient gradient = new RadialGradient(
                cx, cy, radius,
                new int[]{lightColor, midColor, darkColor, edgeColor},
                new float[]{0.0f, 0.4f, 0.8f, 1.0f},
                Shader.TileMode.CLAMP
        );

        basePaint.setShader(gradient);
        canvas.drawCircle(cx, cy, radius, basePaint);
    }

    /**
     * 绘制同心圆纹理（唱片沟槽效果）
     */
    private static void drawGrooves(Canvas canvas, float cx, float cy,
                                    float innerRadius, float outerRadius, int themeColor) {
        Paint groovePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        groovePaint.setStyle(Paint.Style.STROKE);

        // 根据输出尺寸调整沟槽间距
        float grooveSpacing = Math.max(2f, outerRadius / 80f);
        int grooveCount = (int) ((outerRadius - innerRadius - 10) / grooveSpacing);

        int darkGroove = darkenColor(themeColor, 0.4f);
        int lightGroove = lightenColor(themeColor, 0.15f);

        // 绘制细密的沟槽纹理
        for (int i = 0; i < grooveCount; i++) {
            float currentRadius = innerRadius + 5 + (i * grooveSpacing);

            // 交替使用深浅色创建纹理效果
            if (i % 3 == 0) {
                groovePaint.setColor(setAlpha(darkGroove, 80));
                groovePaint.setStrokeWidth(1.5f);
            } else if (i % 3 == 1) {
                groovePaint.setColor(setAlpha(lightGroove, 60));
                groovePaint.setStrokeWidth(1.0f);
            } else {
                groovePaint.setColor(setAlpha(themeColor, 40));
                groovePaint.setStrokeWidth(0.8f);
            }

            canvas.drawCircle(cx, cy, currentRadius, groovePaint);
        }

        // 添加一些更明显的主沟槽
        groovePaint.setStrokeWidth(2f);
        float majorGrooveSpacing = (outerRadius - innerRadius - 10) / 8f;
        for (int i = 1; i <= 7; i++) {
            float r = innerRadius + 5 + (i * majorGrooveSpacing);
            groovePaint.setColor(setAlpha(darkenColor(themeColor, 0.5f), 100));
            canvas.drawCircle(cx, cy, r, groovePaint);
        }

        // 边缘加深
        Paint edgePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        edgePaint.setStyle(Paint.Style.STROKE);
        edgePaint.setStrokeWidth(3f);
        edgePaint.setColor(setAlpha(darkenColor(themeColor, 0.6f), 150));
        canvas.drawCircle(cx, cy, outerRadius - 2, edgePaint);
    }

    /**
     * 绘制光泽效果
     */
    private static void drawShineEffect(Canvas canvas, float cx, float cy,
                                        float radius, int themeColor) {
        Paint shinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        // 创建斜向线性渐变模拟光泽
        float offset = radius * 0.7f;
        LinearGradient shineGradient = new LinearGradient(
                cx - offset, cy - offset,
                cx + offset, cy + offset,
                new int[]{
                        Color.TRANSPARENT,
                        setAlpha(Color.WHITE, 25),
                        setAlpha(Color.WHITE, 50),
                        setAlpha(Color.WHITE, 25),
                        Color.TRANSPARENT
                },
                new float[]{0.0f, 0.35f, 0.5f, 0.65f, 1.0f},
                Shader.TileMode.CLAMP
        );

        shinePaint.setShader(shineGradient);
        canvas.drawCircle(cx, cy, radius, shinePaint);

        // 添加高光弧线
        Paint highlightPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        highlightPaint.setStyle(Paint.Style.STROKE);
        highlightPaint.setStrokeWidth(radius * 0.12f);
        highlightPaint.setStrokeCap(Paint.Cap.ROUND);

        LinearGradient arcGradient = new LinearGradient(
                cx - radius * 0.5f, cy - radius * 0.8f,
                cx + radius * 0.3f, cy - radius * 0.3f,
                new int[]{Color.TRANSPARENT, setAlpha(Color.WHITE, 35), Color.TRANSPARENT},
                new float[]{0.0f, 0.5f, 1.0f},
                Shader.TileMode.CLAMP
        );
        highlightPaint.setShader(arcGradient);

        // 绘制高光弧
        canvas.save();
        canvas.rotate(-45, cx, cy);
        float arcSize = radius * 0.75f;
        canvas.drawArc(
                cx - arcSize, cy - arcSize,
                cx + arcSize, cy + arcSize,
                -70, 60, false, highlightPaint
        );
        canvas.restore();

        // 底部反光
        Paint bottomShinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        bottomShinePaint.setStyle(Paint.Style.STROKE);
        bottomShinePaint.setStrokeWidth(radius * 0.08f);
        bottomShinePaint.setStrokeCap(Paint.Cap.ROUND);

        LinearGradient bottomGradient = new LinearGradient(
                cx - radius * 0.3f, cy + radius * 0.5f,
                cx + radius * 0.5f, cy + radius * 0.8f,
                new int[]{Color.TRANSPARENT, setAlpha(Color.WHITE, 20), Color.TRANSPARENT},
                new float[]{0.0f, 0.5f, 1.0f},
                Shader.TileMode.CLAMP
        );
        bottomShinePaint.setShader(bottomGradient);

        canvas.save();
        canvas.rotate(135, cx, cy);
        float bottomArcSize = radius * 0.8f;
        canvas.drawArc(
                cx - bottomArcSize, cy - bottomArcSize,
                cx + bottomArcSize, cy + bottomArcSize,
                -50, 40, false, bottomShinePaint
        );
        canvas.restore();
    }

    /**
     * 绘制圆形专辑封面
     */
    private static void drawCircularAlbum(Canvas canvas, Bitmap albumBitmap,
                                          float cx, float cy, float radius) {
        if (albumBitmap == null || albumBitmap.isRecycled()) {
            Paint defaultPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            defaultPaint.setColor(Color.DKGRAY);
            canvas.drawCircle(cx, cy, radius, defaultPaint);
            return;
        }

        // 如果是 HARDWARE Bitmap（常见于 Glide/Android 8+），先拷贝成可用于 Shader 的格式
        if (albumBitmap.getConfig() == Bitmap.Config.HARDWARE) {
            albumBitmap = albumBitmap.copy(Bitmap.Config.ARGB_8888, false);
        }

        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setFilterBitmap(true);
        paint.setDither(true);

        BitmapShader shader = new BitmapShader(albumBitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);

        float diameter = radius * 2f;
        int bw = albumBitmap.getWidth();
        int bh = albumBitmap.getHeight();

        // centerCrop：按较大的缩放系数放大，保证圆里填满且不变形
        float scale = diameter / Math.min(bw, bh);

        // 先算在“直径方框”里的居中偏移
        float dx = (diameter - bw * scale) * 0.5f;
        float dy = (diameter - bh * scale) * 0.5f;

        Matrix matrix = new Matrix();
        matrix.setScale(scale, scale);

        // 关键：把 shader 对齐到圆外接矩形的左上角 (cx - r, cy - r)
        matrix.postTranslate((cx - radius) + dx, (cy - radius) + dy);

        shader.setLocalMatrix(matrix);
        paint.setShader(shader);

        canvas.drawCircle(cx, cy, radius, paint);
    }



    /**
     * 绘制专辑封面边缘效果
     */
    private static void drawAlbumBorder(Canvas canvas, float cx, float cy,
                                        float radius, int themeColor) {
        // 外圈装饰 - 渐变边框
        Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(4f);

        RadialGradient borderGradient = new RadialGradient(
                cx, cy, radius * 1.1f,
                new int[]{lightenColor(themeColor, 0.3f), themeColor, darkenColor(themeColor, 0.3f)},
                new float[]{0.85f, 0.92f, 1.0f},
                Shader.TileMode.CLAMP
        );
        borderPaint.setShader(borderGradient);
        canvas.drawCircle(cx, cy, radius + 2, borderPaint);

        // 内圈高光
        Paint innerRingPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        innerRingPaint.setStyle(Paint.Style.STROKE);
        innerRingPaint.setStrokeWidth(1.5f);
        innerRingPaint.setColor(setAlpha(Color.WHITE, 100));
        canvas.drawCircle(cx, cy, radius - 1, innerRingPaint);

        // 内圈阴影
        Paint shadowRingPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        shadowRingPaint.setStyle(Paint.Style.STROKE);
        shadowRingPaint.setStrokeWidth(2f);
        shadowRingPaint.setColor(setAlpha(Color.BLACK, 60));
        canvas.drawCircle(cx, cy, radius + 1, shadowRingPaint);
    }

    // ==================== 颜色工具方法 ====================

    /**
     * 加深颜色
     * @param color 原始颜色
     * @param factor 加深系数 (0.0 - 1.0)
     * @return 加深后的颜色
     */
    private static int darkenColor(int color, float factor) {
        int r = (int) (Color.red(color) * (1 - factor));
        int g = (int) (Color.green(color) * (1 - factor));
        int b = (int) (Color.blue(color) * (1 - factor));
        return Color.argb(Color.alpha(color),
                Math.max(0, r), Math.max(0, g), Math.max(0, b));
    }

    /**
     * 提亮颜色
     * @param color 原始颜色
     * @param factor 提亮系数 (0.0 - 1.0)
     * @return 提亮后的颜色
     */
    private static int lightenColor(int color, float factor) {
        int r = Color.red(color);
        int g = Color.green(color);
        int b = Color.blue(color);

        r = (int) (r + (255 - r) * factor);
        g = (int) (g + (255 - g) * factor);
        b = (int) (b + (255 - b) * factor);

        return Color.argb(Color.alpha(color),
                Math.min(255, r), Math.min(255, g), Math.min(255, b));
    }

    /**
     * 设置颜色透明度
     * @param color 原始颜色
     * @param alpha 透明度 (0 - 255)
     * @return 设置透明度后的颜色
     */
    private static int setAlpha(int color, int alpha) {
        return Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color));
    }
}
