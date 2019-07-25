import org.gradle.api.Project
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.dsl.DependencyHandler
import org.gradle.kotlin.dsl.extra
import org.gradle.kotlin.dsl.invoke
import org.gradle.kotlin.dsl.*

object Versions {
    val kotlin = "1.3.41"
    val kryo = "4.0.2"
    val coroutines = "1.2.0"
    val androidBuildTools = "3.4.2"
    val dokka = "0.9.16"
    val bintray = "1.8.3"
    val self = "0.7.990-SNAPSHOT"
    @Deprecated("Use self", ReplaceWith("this.self"))
    val myVersion get() = self
    val constraintLayout = "1.1.3"
    val minSdk = 16
    val targetSdk = 28
    val compileSdk = 28
    val androidCompat = "28.0.0"
    val junit ="4.12"
    val espressoCore = "3.1.0"
    val androidTestSupport = "1.1.0"
}
object Libraries {
    val supportLib = "com.android.support:appcompat-v7:${Versions.androidCompat}"
    val junit = "junit:junit:${Versions.junit}"
    val kryo = "com.esotericsoftware:kryo:${Versions.kryo}"
    val androidTestRunner = "androidx.test:runner:${Versions.androidTestSupport}"
    val androidTestRules = "androidx.test:rules:${Versions.androidTestSupport}"
    val espressoCore ="androidx.test.espresso:espresso-core:${Versions.espressoCore}"
    val constraintLayout = "com.android.support.constraint:constraint-layout:${Versions.constraintLayout}"
    val kotlinlib = "org.jetbrains.kotlin:kotlin-stdlib:${Versions.kotlin}"
    val kotlinlib8 = "org.jetbrains.kotlin:kotlin-stdlib-jdk8:${Versions.kotlin}"
    val androidExtensionRuntime = "org.jetbrains.kotlin:kotlin-android-extensions-runtime:${Versions.kotlin}"
    val coroutines = "org.jetbrains.kotlinx:kotlinx-coroutines-core:${Versions.coroutines}"
    val coroutinesAndroid = "org.jetbrains.kotlinx:kotlinx-coroutines-android:${Versions.coroutines}"
}

private fun DependencyHandler.androidTestImplementation(dependencyNotation: Any): Dependency? =
        add("androidTestImplementation", dependencyNotation)


fun DependencyHandlerScope.useEspresso() {
    androidTestImplementation("androidx.test.ext:junit:1.0.0")
    androidTestImplementation(Libraries.androidTestRunner)
    androidTestImplementation(Libraries.androidTestRules)
    androidTestImplementation(Libraries.espressoCore)
}

fun Project.projectRepositories() {
    repositories {
        mavenLocal()
        jcenter()
        google()
        maven(url = "https://dl.bintray.com/kotlin/kotlin-eap")
    }
}
