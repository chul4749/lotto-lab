import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// 안드로이드에 기대지 않는 순수 로직(통계·뽑기·등수·시뮬·카페 글). 휴대폰 없이 단위 테스트한다.
plugins {
    kotlin("jvm")
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}
kotlin { compilerOptions { jvmTarget.set(JvmTarget.JVM_17) } }

dependencies {
    testImplementation(kotlin("test"))
}

tasks.test { useJUnitPlatform() }
