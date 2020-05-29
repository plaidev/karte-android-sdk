package io.karte.android.variables.integration

import com.google.common.truth.Truth.assertThat
import io.karte.android.integration.DryRunTestCase
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
