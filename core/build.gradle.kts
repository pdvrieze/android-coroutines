import libraries.*
import versions.selfVersion

plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("kotlin-android-extensions")
    id("maven-publish")
    id("org.jetbrains.dokka")
    idea
}

version = selfVersion
group = "net.devrieze"
description = "Library to add coroutine support for Android flow"

repositories {
    mavenLocal()
    mavenCentral()
    google()
}

val reqCompileSdkVersion:String by project
val reqTargetSdkVersion:String by project
val reqMinSdkVersion:String by project

android {
    compileSdkVersion(reqCompileSdkVersion.toInt())

    defaultConfig {
        minSdkVersion(reqMinSdkVersion.toInt())
        targetSdkVersion(reqTargetSdkVersion.toInt())
        versionName = selfVersion
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    packagingOptions {
        pickFirst("META-INF/atomicfu.kotlin_module")
    }
}

dependencies {
    implementation(supportLibSpec)
    implementation(kryoSpec)
    implementation(kotlinlibSpec)
    implementation(androidExtensionRuntimeSpec)

    api(coroutinesSpec)
    api(coroutinesAndroidSpec)
}

val sourcesJar = task<Jar>("androidSourcesJar") {
    classifier = "sources"
    from(android.sourceSets["main"].java.srcDirs)
}

androidExtensions {
    isExperimental = true
}

/*
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
*/


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
                            dependency(kryoSpec)
                            dependency(kotlinlibSpec)
                            dependency(androidExtensionRuntimeSpec)

                            dependency(coroutinesAndroidSpec, type = "jar")
                        }
                    }
                }
            }
        }
    }

}

idea {
    module {
        name = "android-coroutines.core"
    }
}