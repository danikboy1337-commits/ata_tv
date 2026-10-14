plugins {
    id("com.android.application")
}

val appVersionName = "1.0.0"

android {
    namespace = "kz.atatv"
    compileSdk = 35

    defaultConfig {
        applicationId = "kz.atatv"
        minSdk = 21
        // 34, а не 35: на Android 15 при targetSdk 35 принудительно включается edge-to-edge,
        // а приставкам это ничего не даёт.
        targetSdk = 34
        versionCode = 1
        versionName = appVersionName
    }

    // Ключ НЕ секретный и лежит в репозитории намеренно: приложение ставится вручную
    // (не через магазин), а одинаковая подпись нужна, чтобы обновления ставились поверх.
    signingConfigs {
        create("ataTv") {
            storeFile = rootProject.file("signing/ata-tv.p12")
            storeType = "pkcs12"
            storePassword = "atatv-public"
            keyAlias = "ata-tv"
            keyPassword = "atatv-public"
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("ataTv")
        }
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("ataTv")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    lint {
        abortOnError = true
        checkReleaseBuilds = true
        // targetSdk 34 выбран осознанно (см. defaultConfig).
        disable += "OldTargetApi"
        // Баннер Android TV по спецификации нужен только в drawable-xhdpi.
        disable += "IconMissingDensityFolder"
    }
}

dependencies {
    testImplementation("junit:junit:4.13.2")
}

// ./gradlew :app:exportReleaseApk — собрать подписанный APK и положить в dist/.
tasks.register<Copy>("exportReleaseApk") {
    dependsOn("assembleRelease")
    from(layout.buildDirectory.dir("outputs/apk/release"))
    include("*.apk")
    into(rootProject.layout.projectDirectory.dir("dist"))
    rename { "ata-tv-$appVersionName.apk" }
}
