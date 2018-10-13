pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        maven(url = "https://dl.bintray.com/kotlin/kotlin-eap")
//        maven("https://dl.bintray.com/kotlin/kotlin-dev")
    }
    resolutionStrategy {
        eachPlugin {
            when (requested.id.id) {
                "com.android.library",
                "com.android.application"     -> {
                    val ver = requested.version ?: Versions.androidBuildTools
                    useModule("com.android.tools.build:gradle:${ver}");
                }
                "org.jetbrains.kotlin.android",
                "kotlin-android-extensions"   -> {
                    val ver = requested.version ?: Versions.kotlin
                    useModule("org.jetbrains.kotlin:kotlin-gradle-plugin:${ver}");
                }
                "org.jetbrains.dokka-android" -> {
                    val ver = requested.version ?: Versions.dokka
                    useModule("org.jetbrains.dokka:dokka-android-gradle-plugin:${ver}")
                }
                "com.jfrog.bintray" -> {
                    val ver = requested.version ?: Versions.bintray
                    useVersion(ver)
                }
            }
        }
    }
}

include(":testapp", ":core", ":appcompat")
