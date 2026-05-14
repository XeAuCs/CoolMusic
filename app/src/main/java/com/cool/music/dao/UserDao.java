package com.cool.music.dao;

import android.database.sqlite.SQLiteDatabase;
import android.database.Cursor;
import android.util.Log;

import com.cool.music.bean.SheetBean;
import com.cool.music.bean.UserBean;
import com.cool.music.util.DBUtil;
import com.cool.music.util.SQLiteDbUtils;

public class UserDao {
    public static SQLiteDatabase db = DBUtil.con;

    public static int isLogin(String account, String password) {
        String[] data = {account, password};


        String sql = "select * from d_user where account=? and password=?";
        Cursor result = db.rawQuery(sql, data);

        try {
            if (result.moveToNext()) {
                int permissionIndex = result.getColumnIndex("permission");
                String permissionValue = result.getString(permissionIndex);
                Log.d("登录权限", "Permission: " + permissionValue);
                return Integer.parseInt(permissionValue);
            }
            return -1;
        } finally {
            result.close();
        }
    }
    public static int register(String account, String nickname, String password,String avatar,String sex) {
        String[] data = {account, nickname, password,avatar,sex};
        String sql = "insert into d_user (account,nickname,password,avatar,sex) values(?,?,?,?,?)";
        try {
            db.execSQL(sql, data);
            return 1;
        } catch (Exception e) {
            return -1;
        }


    }

    // 根据账号查询用户
    public static UserBean getUserByAccount(String account) {
        String sql = "select * from d_user where account=?";
        return SQLiteDbUtils.queryOne(sql, UserBean.class, account);
    }


    public static void updateUserInfo(String account, String nickname, String password,String avatar_path,String background_path,String sex,String address){
        String sql = "update d_user set nickname=?,password=?,avatar_path=?,background_path=?,sex=?,address=? where account=?";
        SQLiteDbUtils.executeUpdate(sql,nickname,password,avatar_path,background_path,sex,address,account);
    }


    public static UserBean getUserBySheet(String sheetId) {
        if (sheetId == null || sheetId.isEmpty()) {
            return null;
        }

        String sql = "select * from d_sheet where id=?";
        SheetBean sheet = SQLiteDbUtils.queryOne(sql, SheetBean.class, sheetId);

        if (sheet == null) {
            return null;
        }

        String ownerId = sheet.getOwner_id();
        if (ownerId == null || ownerId.isEmpty()) {
            return null;
        }

        return getUserByAccount(ownerId);
    }









}

