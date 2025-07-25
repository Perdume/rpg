plugins {
    java
    id("com.github.johnrengelman.shadow") version "8.1.1"
    id("io.papermc.paperweight.userdev") version "2.0.0-beta.17"
}

group = "com.perdume.rpg"
version = "1.0-SNAPSHOT"

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://jitpack.io")
    maven("https://mvn.mythiccraft.io/repository/maven-public/")
}

dependencies {
    paperweight.paperDevBundle("1.21.7-R0.1-SNAPSHOT") // 1.21.x 버전의 Paper 서버 사용
    // [핵심] VaultAPI에서 오래된 bukkit 의존성을 제외하여 충돌 해결
    compileOnly("com.github.MilkBowl:VaultAPI:1.7") {
        exclude(group = "org.bukkit", module = "bukkit")
    }

}

// Shadow JAR 설정
tasks {

    build {
        dependsOn(jar)
        paperweight.reobfArtifactConfiguration = io.papermc.paperweight.userdev.ReobfArtifactConfiguration.MOJANG_PRODUCTION
    }
    withType<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar> {
        archiveClassifier.set("") // -all 제거
        from(sourceSets.main.get().resources)
    }

    withType<JavaCompile> {
        options.encoding = "UTF-8"
    }
    // plugin.yml 같은 리소스 파일을 처리할 때도 UTF-8을 사용합니다.
    processResources {
        from(sourceSets.main.get().resources.srcDirs) {
            include("**/*.yml")
        }
        filteringCharset = "UTF-8"
    }
}

tasks.jar {
    manifest {
        attributes["paperweight-mappings-namespace"] = "mojang"
    }
}

