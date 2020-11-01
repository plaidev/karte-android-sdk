//
//  Copyright 2020 PLAID, Inc.
//
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//      https://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License.
//
package io.karte.android

import android.content.pm.PackageInfo
import android.os.Build
import io.karte.android.core.logger.LogLevel
import io.karte.android.shadow.CustomShadowWebView
import io.karte.android.utilities.connectivity.Connectivity
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkObject
import org.junit.After
import org.junit.Before
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLog
import org.robolectric.util.ReflectionHelpers

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], shadows = [CustomShadowWebView::class])
abstract class RobolectricTestCase {

    val application by lazy { application() }

    @Before
    fun before() {
        // Display logs in console.
        ShadowLog.stream = System.out

        // make online
        mockkObject(Connectivity)
        every { Connectivity.isOnline(any()) } returns true

        KarteApp.setLogLevel(LogLevel.VERBOSE)

        // Init Device Info
        val packageInfo = PackageInfo()
        packageInfo.versionName = "1.0.0"
        @Suppress("DEPRECATION")
        packageInfo.versionCode = 1
        packageInfo.packageName = application.packageName
        shadowOf(application.packageManager).installPackage(packageInfo)

        ReflectionHelpers.setStaticField(Build.VERSION::class.java, "RELEASE", "8.0.0")
        ReflectionHelpers.setStaticField(Build::class.java, "DEVICE", "mario device")
        ReflectionHelpers.setStaticField(Build::class.java, "BRAND", "google")
        ReflectionHelpers.setStaticField(Build::class.java, "MODEL", "Nexus")
        ReflectionHelpers.setStaticField(Build::class.java, "PRODUCT", "KARTE for APP")
    }

    @After
    fun after() {
        unmockkObject(Connectivity)
    }
}
