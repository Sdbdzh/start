plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.start.launcher"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.start.launcher"
        // 按 Android 16+ 专属方案锁定：minSdk 36
        minSdk = 36
        targetSdk = 36
        versionCode = 3
        versionName = "0.3.0"
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    buildFeatures {
        compose = true
        // 生成 BuildConfig：关于页版本号读取 versionName，避免硬编码漏改
        buildConfig = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
    }
}

dependencies {
    // AndroidX 基础
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    // Compose + Material 3 Expressive
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.foundation)
    implementation(libs.compose.animation)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.core)

    // 数据层：Room（应用/分类/启动统计）+ DataStore（主题设置）
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.datastore.preferences)
    // 背景图 EXIF 旋转读取
    implementation(libs.androidx.exifinterface)

    // 动态取色：种子色 -> M3 色板（含 AMOLED 支持）
    implementation(libs.materialkolor)

    // 仅调试用：Compose 布局检查器
    debugImplementation(libs.compose.ui.tooling)
}
