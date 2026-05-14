package com.cool.music.util;

import static com.cool.music.util.DBUtil.con;


import android.database.Cursor;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class SQLiteDbUtils {

    private SQLiteDbUtils() {
    }

    public static <T> List<T> queryList(String sql, Class<T> modelClass, String... selectionArgs) {
        Cursor cursor = null;
        List<T> results = new ArrayList<>();

        try {
            cursor = con.rawQuery(sql, selectionArgs);

            while (cursor.moveToNext()) {
                T entity = newInstance(modelClass);
                Field[] fields = modelClass.getDeclaredFields();

                for (Field field : fields) {
                    field.setAccessible(true);

                    String columnName = field.getName();
                    int columnIndex = cursor.getColumnIndex(columnName);
                    if (columnIndex < 0) {
                        // 查询结果里没有这个列名，跳过
                        continue;
                    }

                    String value = cursor.getString(columnIndex);
                    field.set(entity, value);
                }

                results.add(entity);
            }

            return results;
        } catch (Exception e) {
            throw new RuntimeException("queryList failed: " + sql, e);
        } finally {
            if (cursor != null) cursor.close();
        }
    }

    public static <T> T queryOne(String sql, Class<T> modelClass, String... selectionArgs) {
        Cursor cursor = null;

        try {
            cursor = con.rawQuery(sql, selectionArgs);

            if (!cursor.moveToNext()) {
                return null;
            }

            T entity = newInstance(modelClass);
            Field[] fields = modelClass.getDeclaredFields();

            for (Field field : fields) {
                field.setAccessible(true);

                String columnName = field.getName();
                int columnIndex = cursor.getColumnIndex(columnName);
                if (columnIndex < 0) {
                    continue;
                }

                String value = cursor.getString(columnIndex);
                field.set(entity, value);
            }

            return entity;
        } catch (Exception e) {
            throw new RuntimeException("queryOne failed: " + sql, e);
        } finally {
            if (cursor != null) cursor.close();
        }
    }

    /**
     * 执行 INSERT / UPDATE / DELETE 等不返回 Cursor 的 SQL
     * @return 1 表示成功，-1 表示失败（保持你原来的返回约定）
     */
    public static int executeUpdate(String sql, String... bindArgs) {
        try {
            con.execSQL(sql, bindArgs);
            return 1;
        } catch (Exception e) {
            return -1;
        }
    }

    public static <T> List<Map<String, String>> queryMapList(String sql, Class<T> modelClass, String... selectionArgs) {
        Cursor cursor = null;
        List<Map<String, String>> results = new ArrayList<>();

        try {
            cursor = con.rawQuery(sql, selectionArgs);

            Field[] fields = modelClass.getDeclaredFields();

            while (cursor.moveToNext()) {
                Map<String, String> rowMap = new HashMap<>();

                for (Field field : fields) {
                    field.setAccessible(true);

                    String columnName = field.getName();
                    int columnIndex = cursor.getColumnIndex(columnName);
                    if (columnIndex < 0) {
                        continue;
                    }

                    String value = cursor.getString(columnIndex);
                    rowMap.put(columnName, value);
                }

                results.add(rowMap);
            }

            return results;
        } catch (Exception e) {
            throw new RuntimeException("queryMapList failed: " + sql, e);
        } finally {
            if (cursor != null) cursor.close();
        }
    }

    public static <T> Map<String, String> queryMapOne(String sql, Class<T> modelClass, String... selectionArgs) {
        Cursor cursor = null;

        try {
            cursor = con.rawQuery(sql, selectionArgs);

            if (!cursor.moveToNext()) {
                return null;
            }

            Map<String, String> rowMap = new HashMap<>();
            Field[] fields = modelClass.getDeclaredFields();

            for (Field field : fields) {
                field.setAccessible(true);

                String columnName = field.getName();
                int columnIndex = cursor.getColumnIndex(columnName);
                if (columnIndex < 0) {
                    continue;
                }

                String value = cursor.getString(columnIndex);
                rowMap.put(columnName, value);
            }

            return rowMap;
        } catch (Exception e) {
            throw new RuntimeException("queryMapOne failed: " + sql, e);
        } finally {
            if (cursor != null) cursor.close();
        }
    }

    private static <T> T newInstance(Class<T> modelClass)
            throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        Constructor<T> constructor = modelClass.getDeclaredConstructor();
        constructor.setAccessible(true);
        return constructor.newInstance();
    }

    public static int queryCount(String sql, String... selectionArgs) {
        Cursor cursor = null;
        try {
            cursor = con.rawQuery(sql, selectionArgs);
            return cursor.getCount();
        } catch (Exception e) {
            throw new RuntimeException("queryCount failed: " + sql, e);
        } finally {
            if (cursor != null) cursor.close();
        }
    }

    public static List<Map<String, String>> queryMapList(String sql, String... selectionArgs) {
        Cursor cursor = null;
        List<Map<String, String>> results = new ArrayList<>();

        try {
            cursor = con.rawQuery(sql, selectionArgs);
            String[] columnNames = cursor.getColumnNames();  // 直接获取所有列名

            while (cursor.moveToNext()) {
                Map<String, String> rowMap = new HashMap<>();

                for (String columnName : columnNames) {
                    int columnIndex = cursor.getColumnIndex(columnName);
                    if (columnIndex >= 0) {
                        String value = cursor.getString(columnIndex);
                        rowMap.put(columnName, value);
                    }
                }

                results.add(rowMap);
            }

            return results;
        } catch (Exception e) {
            throw new RuntimeException("queryMapList failed: " + sql, e);
        } finally {
            if (cursor != null) cursor.close();
        }
    }
}
