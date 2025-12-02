//
//  Copyright 2023 PLAID, Inc.
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
@file:Suppress("DEPRECATION")

package io.karte.android.inappmessaging.internal.view

import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.app.DialogFragment
import android.os.Bundle
import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY)
class AlertDialogFragment : DialogFragment() {
    @Deprecated("OVERRIDE_DEPRECATION")
    override fun onCreateDialog(savedInstanceState: Bundle): Dialog {
        val builder = AlertDialog.Builder(activity)
        val message = arguments.getString("message")
        builder.setMessage(message)
        builder.setPositiveButton(android.R.string.ok, null)
        return builder.create()
    }

    companion object {

        internal fun show(activity: Activity, message: String) {
            val alertDialogFragment = AlertDialogFragment()
            val bundle = Bundle()
            bundle.putString("message", message)
            alertDialogFragment.arguments = bundle
            alertDialogFragment.show(activity.fragmentManager, "krt_alert_dialog")
        }
    }
}
