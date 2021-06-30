import com.android.build.gradle.internal.dsl.BuildType
import libraries.*
import versions.coroutinesVersion

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("kotlin-android-extensions")
}

val reqCompileSdkVersion:String by project
val reqTargetSdkVersion:String by project
val reqMinSdkVersion:String by project

android {
    compileSdkVersion(reqCompileSdkVersion.toInt())

    defaultConfig {
        applicationId= "uk.ac.bmth.aprog.testapp"
        minSdkVersion(reqMinSdkVersion.toInt())
        targetSdkVersion(reqTargetSdkVersion.toInt())
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    packagingOptions {
        pickFirst("META-INF/atomicfu.kotlin_module")
        pickFirst("META-INF/AL2.0")
        pickFirst("META-INF/LGPL2.1")
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

