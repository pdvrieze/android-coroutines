
pluginManagement {
    val androidBuildToolsVersion: String by settings
    val kotlinVersion: String by settings
    val dokkaVersion: String by settings
    val bintrayVersion: String by settings

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
                    val ver = requested.version ?: androidBuildToolsVersion
                    useModule("com.android.tools.build:gradle:${ver}");
                }
                "org.jetbrains.kotlin.android",
                "kotlin-android-extensions"   -> {
                    val ver = requested.version ?: kotlinVersion
                    useModule("org.jetbrains.kotlin:kotlin-gradle-plugin:${ver}");
                }
                "org.jetbrains.dokka" -> {
                    val ver = requested.version ?: dokkaVersion
                    useVersion(ver)
//                    useModule("${requested.module}:${ver}")
                }
                "com.jfrog.bintray" -> {
                    val ver = requested.version ?: bintrayVersion
                    useVersion(ver)
                }
            }
        }
    }
}

include(":testapp", ":core", ":appcompat")
