import com.jfrog.bintray.gradle.BintrayExtension
import com.jfrog.bintray.gradle.tasks.BintrayUploadTask
import org.gradle.kotlin.dsl.kotlin
import org.jetbrains.dokka.gradle.DokkaAndroidTask
import org.jetbrains.dokka.gradle.LinkMapping
import java.util.Date
import versions.*
import libraries.*

plugins {
    id("com.android.library")
    kotlin("android")
    id("kotlin-android-extensions")
    id("maven-publish")
    id("com.jfrog.bintray")
    id("org.jetbrains.dokka-android")
}

version = selfVersion
group = "net.devrieze"
description = "Extension for android coroutines that supports the appcompat library"

projectRepositories()

android {
    compileSdkVersion(compileSdk)

    defaultConfig {
        minSdkVersion(minSdk)
        targetSdkVersion(targetSdk)
        versionName = selfVersion
    }

    compileOptions {
        setSourceCompatibility(JavaVersion.VERSION_1_8)
        setTargetCompatibility(JavaVersion.VERSION_1_8)
    }

    packagingOptions {
        pickFirst("META-INF/atomicfu.kotlin_module")
    }

}

dependencies {
    implementation(supportLibSpec)

    implementation(kotlin("stdlib"))
    implementation(androidExtensionRuntimeSpec)

    api(project(":core"))
}

val sourcesJar = task<Jar>("androidSourcesJar") {
    classifier = "sources"
    from(android.sourceSets["main"].java.srcDirs)
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

afterEvaluate{
    publishing {
        (publications) {
            create<MavenPublication>("MyPublication") {
                artifact(tasks["bundleReleaseAar"])

                groupId = project.group as String
                artifactId = "android-coroutines-appcompat"
                artifact(sourcesJar).apply {
                    classifier="sources"
                }
                pom {
                    withXml {
                        dependencies {
                            dependency("$groupId:android-coroutines:[$version]", type = "aar")
                            dependency(supportLibSpec)
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
}


tasks.withType<BintrayUploadTask> {
    dependsOn(sourcesJar)
}
