import com.android.build.gradle.internal.dsl.BuildType
import libraries.*
import versions.*

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("kotlin-android-extensions")
}

android {
    compileSdkVersion(compileSdk)

    defaultConfig {
        applicationId= "uk.ac.bmth.aprog.testapp"
        minSdkVersion(minSdk)
        targetSdkVersion(targetSdk)
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

    implementation(supportLibSpec)
    implementation(androidExtensionRuntimeSpec)

    implementation(constraintLayoutSpec)
    implementation(kotlinlibSpec)
    implementation(kryoSpec)

    testImplementation(junitSpec)
    androidTestImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:$coroutinesVersion")
//    testImplementation (kryoSpec)
//    androidTestImplementation (kryoSpec)
    androidTestRuntimeOnly(androidExtensionRuntimeSpec)
    useEspresso(project)
}

androidExtensions {
    isExperimental = true
}


projectRepositories()

