import com.android.build.gradle.internal.dsl.BuildType

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

        testInstrumentationRunner="androidx.test.runner.AndroidJUnitRunner"

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

    packagingOptions {
        pickFirst("META-INF/atomicfu.kotlin_module")
    }
}

dependencies {
    implementation(project(":appcompat"))

    implementation(Libraries.supportLib)
    implementation(Libraries.androidExtensionRuntime)

    implementation(Libraries.constraintLayout)
    implementation(Libraries.kotlinlib)
    implementation(Libraries.kryo)

    testImplementation(Libraries.junit)
//    testImplementation (Libraries.kryo)
//    androidTestImplementation (Libraries.kryo)
    androidTestRuntimeOnly(Libraries.androidExtensionRuntime)
    useEspresso()
}

androidExtensions {
    isExperimental = true
}


projectRepositories()

