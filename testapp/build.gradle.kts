import com.android.build.gradle.internal.dsl.BuildType
import org.jetbrains.kotlin.gradle.dsl.Coroutines
import org.jetbrains.kotlin.gradle.dsl.KotlinCompile
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmCompile

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("kotlin-android-extensions")
}

android {
    compileSdkVersion(Versions.compileSdk)

    defaultConfig {
        applicationId= "uk.ac.bmth.aprog.testapp"
        minSdkVersion(Versions.minSdk)
        targetSdkVersion(Versions.targetSdk)
        versionCode=1
        versionName="1.0"

        testInstrumentationRunner="android.support.test.runner.AndroidJUnitRunner"

    }

    buildTypes {
        getByName<BuildType>("release") {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        setTargetCompatibility("1.8")
        setSourceCompatibility("1.8")
    }
}

dependencies {
    implementation(project(":appcompat"))

    implementation(Libraries.supportLib)
    testImplementation(Libraries.junit)
    testImplementation (Libraries.kryo)
    androidTestImplementation (Libraries.kryo)
    useEspresso()

    implementation (Libraries.constraintLayout)
    implementation(Libraries.kotlinlib)
    implementation(Libraries.kryo)
}

kotlin {
    experimental.coroutines = Coroutines.ENABLE
}

androidExtensions {
    isExperimental = true
}


projectRepositories()

