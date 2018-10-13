import org.gradle.api.tasks.wrapper.Wrapper

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
val androidTarget: Int by extra(28)

plugins {
    id("com.android.library") apply false

    kotlin("android") apply false
}

task("wrapper", Wrapper::class) {
    gradleVersion = "4.10.2"
}
