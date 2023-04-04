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
package io.karte.android.variables.integration

import com.google.common.truth.Truth.assertThat
import io.karte.android.test_lib.integration.DryRunTestCase
import io.karte.android.variables.Variables
import org.junit.Test

class DryRunTest : DryRunTestCase() {
    @Test
    fun testVariables() {
        Variables.fetch()
        val variable = Variables.get("test")
        assertThat(variable.isDefined).isFalse()

        Variables.trackOpen(listOf(variable))
        Variables.trackClick(listOf(variable))
        assertDryRun()
    }
}
