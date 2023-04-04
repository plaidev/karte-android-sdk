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
package io.karte.android.inappmessaging.integration

import android.app.Activity
import android.widget.PopupWindow
import com.google.common.truth.Truth.assertThat
import io.karte.android.inappmessaging.InAppMessaging
import io.karte.android.test_lib.integration.DryRunTestCase
import org.junit.Test
import org.robolectric.Robolectric

class DryRunTest : DryRunTestCase() {
    @Test
    fun testInAppMessaging() {
        assertThat(InAppMessaging.delegate).isNull()
        assertThat(InAppMessaging.isPresenting).isFalse()

        InAppMessaging.dismiss()
        InAppMessaging.suppress()
        InAppMessaging.unsuppress()

        InAppMessaging.registerWindow(Robolectric.buildActivity(Activity::class.java).get().window)
        InAppMessaging.registerPopupWindow(PopupWindow())

        assertDryRun()
    }
}
