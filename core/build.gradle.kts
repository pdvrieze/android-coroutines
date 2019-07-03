import com.jfrog.bintray.gradle.BintrayExtension
import com.jfrog.bintray.gradle.tasks.BintrayUploadTask
import org.gradle.kotlin.dsl.repositories
import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.gradle.DokkaAndroidTask
import org.jetbrains.dokka.gradle.LinkMapping
import java.util.Date
import java.net.URL

plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("kotlin-android-extensions")
    id("maven-publish")
    id("com.jfrog.bintray")
    id("org.jetbrains.dokka-android")
    idea
}

version = Versions.self
group = "net.devrieze"
description = "Library to add coroutine support for Android flow"

repositories {
    mavenLocal()
    jcenter()
    google()
    maven(url = "https://dl.bintray.com/kotlin/kotlin-eap")
}


android {
    compileSdkVersion(Versions.compileSdk)

    defaultConfig {
        minSdkVersion(Versions.minSdk)
        targetSdkVersion(Versions.targetSdk)
        versionName = Versions.self
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
    implementation(Libraries.supportLib)
    implementation(Libraries.kryo)
    implementation(Libraries.kotlinlib)
    implementation(Libraries.androidExtensionRuntime)

    api(Libraries.coroutines)
    api(Libraries.coroutinesAndroid)
}

val sourcesJar = task<Jar>("androidSourcesJar") {
    classifier = "sources"
    from(android.sourceSets["main"].java.srcDirs)
}

androidExtensions {
    isExperimental = true
}

tasks.withType<DokkaAndroidTask> {
    externalDocumentationLink(delegateClosureOf<DokkaConfiguration.ExternalDocumentationLink.Builder> {
        url = URL("https://developer.android.com/reference/")
    })
    linkMappings.add(LinkMapping().apply {
        dir = "src/main/java"
        url = "https://github.com/pdvrieze/android-coroutines/tree/master/core/src/main/java"
        suffix = "#L"
    })
    outputFormat = "html"
}


afterEvaluate {
    publishing {
        (publications) {
            create<MavenPublication>("MyPublication") {
                artifact(tasks["bundleReleaseAar"])

                groupId = project.group as String
                artifactId = "android-coroutines"
                artifact(sourcesJar).apply {
                    classifier = "sources"
                }
                pom {
                    withXml {
                        dependencies {
                            dependency(Libraries.kryo)
                            dependency(Libraries.kotlinlib)
                            dependency(Libraries.androidExtensionRuntime)

                            dependency(Libraries.coroutinesAndroid, type = "jar")
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
            name = "android-coroutines"
            userOrg = "pdvrieze"
            setLicenses("Apache-2.0")
            vcsUrl = "https://github.com/pdvrieze/android-coroutines.git"

            version.apply {
                name = project.version as String
                desc =
                    "Context capture is still a major issue, try to provide wrappers to prevent this."
                released = Date().toString()
                vcsTag = "v$version"
            }
        })
    }
}

tasks.withType<BintrayUploadTask> {
    dependsOn(sourcesJar)
}

idea {
    module {
        name = "android-coroutines.core"
    }
}