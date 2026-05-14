package com.cool.music;

import android.content.Context;

import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;

import com.cool.music.util.MusicMetadataUtil;
import com.cool.music.util.ThemeColorExtractor;

import java.util.List;

/**
 * Instrumented test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class ExampleInstrumentedTest {
    @Test
    public void useAppContext() {
        // Context of the app under test.
        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        assertEquals("com.cool.music", appContext.getPackageName());

        MusicMetadataUtil.MusicInfo musicInfo = MusicMetadataUtil.getMusicInfo("/storage/emulated/0/Android/data/com.cool.music/files/Music/52cd2c3fd7c04ae78b67a82db0b182b8.flac");
        String[] colors = ThemeColorExtractor.extractThemeColors(musicInfo.getCoverBitmap(), 6);
        for(String color : colors){
            System.out.println(color);
        }

    }
}