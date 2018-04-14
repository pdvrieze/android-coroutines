pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
    }
    resolutionStrategy {
        eachPlugin {
            when (requested.id.id) {
                "com.android.application" -> useModule("com.android.tools.build:gradle:${requested.version}")
                "kotlin-android-extensions" -> useModule("org.jetbrains.kotlin:kotlin-gradle-plugin:${requested.version}")
                "org.jetbrains.dokka-android" -> useModule("org.jetbrains.dokka:dokka-android-gradle-plugin:${requested.version}")
            }
        }
    }
}

val foobar = "5"

include(":testapp", ":core", ":appcompat")
//rootProject.name="android-coroutines"