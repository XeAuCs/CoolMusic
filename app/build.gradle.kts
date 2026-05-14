plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.cool.music"
    compileSdk {
        version = release(36)
    }
    // Kotlin DSL 语法
    aaptOptions {
        noCompress += listOf("mp3", "ogg", "wav","flac")
    }

    defaultConfig {
        applicationId = "com.cool.music"
        minSdk = 34
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)

    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    implementation("com.github.bumptech.glide:glide:4.12.0")
    implementation("androidx.cardview:cardview:1.0.0")
    // 不需要额外仓库，直接用 mavenCentral
    implementation("com.mrljdx:ffmpeg-kit-full:6.1.2")

    implementation("androidx.media3:media3-exoplayer:1.9.0")
    implementation("androidx.media3:media3-ui:1.9.0")
    implementation("androidx.media3:media3-session:1.9.0")
// 后台播放、通知栏控制
// 可选：支持 DASH、HLS 流媒体
    implementation("androidx.media3:media3-exoplayer-dash:1.9.0")
    implementation("androidx.media3:media3-exoplayer-hls:1.9.0")
    implementation("org.greenrobot:eventbus:3.3.1")

    implementation("androidx.palette:palette:1.0.0")

    //implementation("com.github.afollestad:material-dialogs-core:3.3.0-alpha1")

}