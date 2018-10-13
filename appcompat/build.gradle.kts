import com.android.build.gradle.LibraryExtension
import com.jfrog.bintray.gradle.BintrayExtension
import com.jfrog.bintray.gradle.tasks.BintrayUploadTask
import groovy.lang.Closure
import groovy.util.Node
import org.gradle.kotlin.dsl.extra
import org.gradle.kotlin.dsl.kotlin
import org.gradle.kotlin.dsl.repositories
import org.jetbrains.dokka.gradle.DokkaAndroidTask
import org.jetbrains.dokka.gradle.LinkMapping
import org.jetbrains.kotlin.gradle.dsl.Coroutines
import java.util.Date

plugins {
    id("com.android.library")
    kotlin("android")
    id("kotlin-android-extensions")
    id("maven-publish")
    id("com.jfrog.bintray")
    id("org.jetbrains.dokka-android")
}

val androidCompatVersion:String by rootProject
val androidTarget:Int by rootProject
val myVersion:String by project
val kotlinVersion:String by project

version = myVersion
group = "net.devrieze"
description = "Extension for android coroutines that supports the appcompat library"

repositories {
    mavenLocal()
    jcenter()
    google()
    maven(url = "https://dl.bintray.com/kotlin/kotlin-dev")
}

android {
    compileSdkVersion(androidTarget)

    defaultConfig {
        minSdkVersion(14)
        targetSdkVersion(androidTarget)
        versionName = version as String
    }

    compileOptions {
        setSourceCompatibility(JavaVersion.VERSION_1_8)
        setTargetCompatibility(JavaVersion.VERSION_1_8)
    }
}

dependencies {
    implementation("com.android.support:appcompat-v7:$androidCompatVersion")

    implementation(kotlin("stdlib"))
    implementation(kotlin("android-extensions-runtime", kotlinVersion))

    api(project(":core"))
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
        url = "https://github.com/pdvrieze/android-coroutines/tree/master/appcompat/src/main/java"
        suffix = "#L"
    })
    outputFormat = "html"
}

inline fun XmlProvider.dependencies(config: Node.() -> Unit): Unit {
    asNode().dependencies(config)
}

inline fun Node.dependencies(config: Node.() -> Unit): Node {
    val ch: List<Node> = children() as List<Node>
    val node: Node = ch.firstOrNull { name == "dependencies" } ?: appendNode("dependencies")
    return node.apply(config)
}

fun Node.dependency(spec: String, type: String = "jar", scope: String = "compile", optional: Boolean = false): Node {
    return spec.split(':', limit = 3).run {
        val groupId = get(0)
        val artifactId = get(1)
        val version = get(2)
        dependency(groupId, artifactId, version, type, scope, optional)
    }
}

fun Node.dependency(groupId: String, artifactId: String, version: String, type: String = "jar", scope: String = "compile", optional: Boolean = false): Node {
    return appendNode("dependency").apply {
        appendNode("groupId", groupId)
        appendNode("artifactId", artifactId)
        appendNode("version", version)
        appendNode("type", type)
        if (scope != "compile") appendNode("scope", scope)
        if (optional) appendNode("optional", "true")
    }
}

publishing {
    (publications) {
        create<MavenPublication>("MyPublication") {
            artifact(tasks.getByName("bundleRelease"))

//            artifact(project.artifacts.["bundleRelease"])
//            from(components["java"])
            groupId = project.group as String
            artifactId = "android-coroutines-appcompat"
            artifact(sourcesJar).apply {
                classifier="sources"
            }
            pom {
                withXml {
                    dependencies {
                        dependency("$groupId:android-coroutines:[$version]", type = "aar")
                        dependency("com.android.support:appcompat-v7:$androidCompatVersion")
                        // all other dependencies are transitive
                    }
                }
            }
        }
    }
}

bintray {
    if (rootProject.hasProperty("bintrayUser")) {
        user = rootProject.property("bintrayUser") as String?
        key = rootProject.property("bintrayApiKey") as String?
    }

    setPublications("MyPublication")

    pkg(closureOf<BintrayExtension.PackageConfig> {
        repo = "maven"
        name = "android-coroutines-appcompat"
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
