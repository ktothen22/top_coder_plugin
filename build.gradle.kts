plugins {
    kotlin("jvm") version "1.9.22" // Kotlin JVM 플러그인 사용 (안정 버전)
    id("org.jetbrains.intellij") version "1.17.3" // IntelliJ 플랫폼 플러그인
}

group = "com.topcoder"
version = "0.1.0"

repositories {
    mavenCentral() // 외부 라이브러리 검색
}

intellij {
    version.set("2023.1") // 대상 IntelliJ/PyCharm 버전
    type.set("PC") // PyCharm Community Edition
    plugins.set(listOf()) // 추가 IntelliJ 플러그인 없으면 비워둠
}

dependencies {
    implementation("org.json:json:20240303") // 간단한 JSON 파싱용 라이브러리
}

kotlin {
    jvmToolchain(21) // Kotlin 컴파일 시 사용될 JVM 버전 (컨테이너에서 제공되는 버전)
}

tasks {
    patchPluginXml {
        sinceBuild.set("231") // 호환 가능한 최소 빌드 버전
        untilBuild.set("251.*") // 테스트 범위 상한 (필요에 따라 조정)
    }
}
