import com.jfrog.bintray.gradle.BintrayExtension
import com.jfrog.bintray.gradle.BintrayUploadTask
import groovy.lang.Closure
import org.gradle.kotlin.dsl.extra
import org.gradle.kotlin.dsl.kotlin
import org.gradle.kotlin.dsl.repositories
import org.jetbrains.dokka.gradle.DokkaAndroidTask
import org.jetbrains.dokka.gradle.LinkMapping
import org.jetbrains.kotlin.gradle.dsl.Coroutines
import java.util.Date

val androidCompatVersion get() = rootProject.extra["androidCompatVersion"] as String
val kotlinVersion: String get() = rootProject.extra["kotlinVersion"] as String

plugins {
    id("com.android.library")
    kotlin("android")
    id("kotlin-android-extensions")
    id("maven-publish")
    id("com.jfrog.bintray")
    id("org.jetbrains.dokka-android")
}

version = "0.6.3"
group = "net.devrieze"
description = "Library to add coroutine support for Android flow"

repositories {
    mavenLocal()
    jcenter()
    google()
}

task("wrapper", Wrapper::class) {
    gradleVersion = "4.6"
}


android {
    compileSdkVersion(27)

    defaultConfig {
        minSdkVersion(14)
        targetSdkVersion(27)
        versionName = version as String
    }

    compileOptions {
        setSourceCompatibility(JavaVersion.VERSION_1_8)
        setTargetCompatibility(JavaVersion.VERSION_1_8)
    }
}

dependencies {
    implementation("com.android.support:support-v4:$androidCompatVersion")
    implementation("com.esotericsoftware:kryo:4.0.1")
    implementation(kotlin("stdlib"))
    implementation(kotlin("android-extensions-runtime", kotlinVersion))

    api("org.jetbrains.kotlinx:kotlinx-coroutines-android:0.22.5")
}

val sourcesJar = task("androidSourcesJar", Jar::class) {
    classifier = "sources"
    from(android.sourceSets["main"].java.srcDirs)
}

kotlin {
    experimental.coroutines = Coroutines.ENABLE
}

androidExtensions {
    isExperimental = true
}

tasks.withType<DokkaAndroidTask> {
    linkMappings.add(LinkMapping().apply {
        dir="src/main/java"
        url = "https://github.com/pdvrieze/android-coroutines/tree/master/core/src/main/java"
        suffix = "#L"
    })
    outputFormat = "html"
}

publishing {
    (publications) {
        "MyPublication"(MavenPublication::class) {
            artifact(tasks.getByName("bundleRelease"))

//            artifact(project.artifacts.["bundleRelease"])
//            from(components["java"])
            groupId = project.group as String
            artifactId = "android-coroutines"
            artifact(sourcesJar).apply {
                classifier="sources"
            }
        }
    }
}

bintray {
    var user: String? = null
    var key: String? = null
    if (rootProject.hasProperty("bintrayUser")) {
        user = rootProject.property("bintrayUser") as String?
        key = rootProject.property("bintrayApiKey") as String?
    }

    setPublications("MyPublication")

    pkg(closureOf<BintrayExtension.PackageConfig> {
        repo = "maven"
        name = "android-coroutines"
        userOrg = "pdvrieze"
        setLicenses("Apache-2.0")
        vcsUrl = "https://github.com/pdvrieze/android-coroutines.git"

        version.apply {
            name = project.version as String
            desc = "Context capture is still a major issue, try to provide wrappers to prevent this."
            released = Date().toString()
            vcsTag = "v$version"
        }
    })
}

tasks.withType<BintrayUploadTask> {
    dependsOn(sourcesJar)
}

/*

bintrayUpload {
    dependsOn(androidSourcesJar)
}
*/
