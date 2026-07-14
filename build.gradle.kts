plugins {
    id("java")
    id("org.jetbrains.intellij") version "1.17.4"
}

group = "com.endpointexplorer"
version = "1.0.0"

repositories {
    mavenCentral()
}

intellij {
//    localPath.set("D:\\Program Files\\JetBrains\\IntelliJ IDEA 2021.1.3")
    version.set("2021.3")  // 最低支持的版本
    type.set("IC")  // IntelliJ Community Edition
    plugins.set(listOf("com.intellij.java"))
    ideaDependencyCachePath.set("D:/.idea-cache")
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

tasks {
    patchPluginXml {
        sinceBuild.set("211")
        untilBuild.set("213.*")
    }

    buildSearchableOptions {
        enabled = false
    }

    // 自定义输出文件名
    buildPlugin {
        archiveFileName.set("endpoint-explorer-${version}.zip")
    }
}
