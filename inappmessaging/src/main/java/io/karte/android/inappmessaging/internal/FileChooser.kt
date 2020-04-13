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
package io.karte.android.inappmessaging.internal

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.fragment.app.Fragment
import io.karte.android.inappmessaging.InAppMessaging
import android.app.Fragment as DeprecatedFragment

internal typealias FileChooserListener = (Array<Uri>?) -> Unit

private const val ACTION_GET_CONTENT_TYPE = "image/*"
private const val ACTION_GET_CONTENT_REQUEST_CODE = 1

@SuppressLint("ValidFragment")
internal class FileChooserDeprecatedFragment : DeprecatedFragment() {

    var listener: FileChooserListener? = null
    private var activityStarted = false

    override fun onResume() {
        super.onResume()

        if (!activityStarted) {
            activityStarted = true

            InAppMessaging.self?.enablePreventRelayFlag(activity)

            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.addCategory(Intent.CATEGORY_OPENABLE)
            intent.type = ACTION_GET_CONTENT_TYPE
            startActivityForResult(intent, ACTION_GET_CONTENT_REQUEST_CODE)
        } else {
            // onActivityResultが呼び出されない場合にフラグメントが残留するのを避ける
            removeFragment()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        var uris: Array<Uri>? = null
        if (requestCode == ACTION_GET_CONTENT_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            uris = data?.data?.let { arrayOf(it) }
        }

        listener?.invoke(uris)
        listener = null

        removeFragment()

        super.onActivityResult(requestCode, resultCode, data)
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }

    private fun removeFragment() {
        val fm = fragmentManager
        fm?.beginTransaction()?.remove(this)?.commit()
    }

    companion object {

        fun newInstance(): FileChooserDeprecatedFragment {
            return FileChooserDeprecatedFragment()
        }
    }
} // Required empty public constructor

internal class FileChooserFragment : Fragment() {

    var listener: FileChooserListener? = null
    private var activityStarted = false

    override fun onResume() {
        super.onResume()

        if (!activityStarted) {
            activityStarted = true

            InAppMessaging.self?.enablePreventRelayFlag(activity)

            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.addCategory(Intent.CATEGORY_OPENABLE)
            intent.type = ACTION_GET_CONTENT_TYPE
            startActivityForResult(intent, ACTION_GET_CONTENT_REQUEST_CODE)
        } else {
            // onActivityResultが呼び出されない場合にフラグメントが残留するのを避ける
            removeFragment()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        var uris: Array<Uri>? = null
        if (requestCode == ACTION_GET_CONTENT_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            uris = data?.data?.let { arrayOf<Uri>(it) }
        }

        listener?.invoke(uris)
        listener = null

        removeFragment()

        super.onActivityResult(requestCode, resultCode, data)
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }

    private fun removeFragment() {
        val fm = fragmentManager
        fm?.beginTransaction()?.remove(this)?.commit()
    }

    companion object {

        fun newInstance(): FileChooserFragment {
            return FileChooserFragment()
        }
    }
} // Required empty public constructor
