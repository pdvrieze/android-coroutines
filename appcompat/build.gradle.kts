import libraries.androidExtensionRuntimeSpec
import libraries.supportLibSpec
import versions.selfVersion

plugins {
    id("com.android.library")
    kotlin("android")
    id("kotlin-android-extensions")
    id("maven-publish")
    id("org.jetbrains.dokka")
}

version = selfVersion
group = "net.devrieze"
description = "Extension for android coroutines that supports the appcompat library"

projectRepositories()

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

//tasks.withType<DokkaTask> {
//    dokkaSourceSets.all {
//
//    }
////    linkMappings.add(LinkMapping().apply {
////        dir="src/main/java"
////        url = "https://github.com/pdvrieze/android-coroutines/tree/master/appcompat/src/main/java"
////        suffix = "#L"
////    })
////    outputFormat = "html"
//}

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

}
