import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.application")
    kotlin("android")
    id("org.jetbrains.kotlin.plugin.compose")
}

// CI 실행 번호를 버전으로 써서 새로 받은 APK가 이전 것 위에 덮어 설치되게 한다.
val runNumber = (System.getenv("GITHUB_RUN_NUMBER") ?: "1").toInt()

android {
    namespace = "com.moon.lotto"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.moon.lotto"
        minSdk = 26
        targetSdk = 35
        versionCode = runNumber
        versionName = "1.0.$runNumber"
    }

    // 직접 설치용 키. 어느 컴퓨터에서 빌드해도 같은 키라 업데이트 설치가 된다.
    // 플레이스토어에 올릴 때는 따로 만든 비공개 키로 바꿔야 한다.
    signingConfigs {
        create("shared") {
            storeFile = file("lotto.keystore")
            storePassword = "lotto-lab"
            keyAlias = "lotto"
            keyPassword = "lotto-lab"
        }
    }
    buildTypes {
        debug { signingConfig = signingConfigs.getByName("shared") }
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("shared")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures { compose = true }

    // 처음 켤 때 인터넷이 없어도 되게, 저장소의 draws.json 을 앱에 같이 넣는다.
    sourceSets["main"].assets.srcDir(layout.buildDirectory.dir("generated/drawsAsset"))
}

kotlin { compilerOptions { jvmTarget.set(JvmTarget.JVM_17) } }

val copyDraws by tasks.registering(Copy::class) {
    from(rootProject.file("../draws.json"))
    into(layout.buildDirectory.dir("generated/drawsAsset"))
}
tasks.named("preBuild") { dependsOn(copyDraws) }

dependencies {
    implementation(project(":core"))
    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-core")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
}
