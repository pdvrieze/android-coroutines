package libraries

import org.gradle.api.Project
import versions.*

val Project.supportLibSpec get() = "com.android.support:appcompat-v7:$androidCompatVersion"
val Project.junitSpec get() = "junitSpec:junit:$junitVersion"
val Project.kryoSpec get() = "com.esotericsoftware:kryo:$kryoVersion"
val Project.androidTestRunnerSpec get() = "androidx.test:runner:$androidTestSupportVersion"
val Project.androidTestRulesSpec get() = "androidx.test:rules:$androidTestSupportVersion"
val Project.espressoCoreSpec get() ="androidx.test.espresso:espresso-core:$espressoCoreVersion"
val Project.constraintLayoutSpec get() = "com.android.support.constraint:constraint-layout:$constraintLayoutVersion"
val Project.kotlinlibSpec get() = "org.jetbrains.kotlin:kotlin-stdlib:$kotlinVersion"
val Project.kotlinlib8Spec get() = "org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kotlinVersion"
val Project.androidExtensionRuntimeSpec get() = "org.jetbrains.kotlin:kotlin-android-extensions-runtime:$kotlinVersion"
val Project.coroutinesSpec get() = "org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion"
val Project.coroutinesAndroidSpec get() = "org.jetbrains.kotlinx:kotlinx-coroutines-android:$coroutinesVersion"
