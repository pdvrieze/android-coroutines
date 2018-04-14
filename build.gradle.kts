import org.jetbrains.kotlin.gradle.plugin.KotlinAndroidPluginWrapper

/*
 * Copyright (c) 2016.
 *
 * This file is part of ProcessManager.
 *
 * ProcessManager is free software: you can redistribute it and/or modify it under the terms of version 2.1 of the
 * GNU Lesser General Public License as published by the Free Software Foundation.
 *
 * ProcessManager is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with Foobar.  If not,
 * see <http://www.gnu.org/licenses/>.
 */

val androidCompatVersion: String by extra("27.1.1")
val androidTarget: Int by extra(27)
val kotlinVersion: String by extra("1.2.31")

plugins {
    base

    val kotlinVersion: String = "1.2.31"

    id("com.android.application") version "3.1.1" apply false
    kotlin("android") version kotlinVersion apply false

    id("kotlin-android-extensions") version kotlinVersion apply false
    id("com.jfrog.bintray") version "1.8.0" apply false

    id("org.jetbrains.dokka-android") version "0.9.16" apply false
}
