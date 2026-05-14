package com.cool.music.util; // 注：此处“until”建议改为“util”（工具类包名常用写法）

import static android.content.Context.MODE_PRIVATE;

import android.content.Context;
import android.content.SharedPreferences;
import android.widget.Toast;

// no usages
public class Tools {

    // no usages
    public static void toast(Context context, String text){
        Toast.makeText(context, text, Toast.LENGTH_SHORT).show();
    }


    public static  String getOnAccount(Context context){
        SharedPreferences sharedPreferences = context.getSharedPreferences("data", MODE_PRIVATE);
        return sharedPreferences.getString("account", "admin");

    }
    public static  void addPrePreferenceData(Context context,String key,String value){
        SharedPreferences sharedPreferences = context.getSharedPreferences("data", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(key, value);

        editor.apply();
    }
    public static  String getPrePreferenceData(Context context, String key){
        SharedPreferences sharedPreferences = context.getSharedPreferences("data", MODE_PRIVATE);
        return sharedPreferences.getString(key, "admin");

    }
}