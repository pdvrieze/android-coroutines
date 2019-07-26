package versions

import org.gradle.api.Project

val Project.kotlinVersion: String get() = property("kotlinVersion") as String
val Project.kryoVersion: String get() = property("kryoVersion") as String
val Project.coroutinesVersion: String get() = property("coroutinesVersion") as String
val Project.androidBuildToolsVersion: String get() = property("androidBuildToolsVersion") as String
val Project.dokkaVersion: String get() = property("dokkaVersion") as String
val Project.bintrayVersion: String get() = property("bintrayVersion") as String
val Project.selfVersion: String get() = property("selfVersion") as String
val Project.constraintLayoutVersion: String get() = property("constraintLayoutVersion") as String
val Project.androidCompatVersion: String get() = property("androidCompatVersion") as String
val Project.junitVersion: String get() = property("junitVersion") as String
val Project.espressoCoreVersion: String get() = property("espressoCoreVersion") as String
val Project.androidTestSupportVersion: String get() = property("androidTestSupportVersion") as String

val Project.minSdk: Int get() = (property("minSdk") as String).toInt()
val Project.targetSdk: Int get() = (property("targetSdk") as String).toInt()
val Project.compileSdk: Int get() = (property("compileSdk") as String).toInt()
