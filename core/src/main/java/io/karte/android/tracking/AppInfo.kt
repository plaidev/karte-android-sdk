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
package io.karte.android.tracking

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import android.util.DisplayMetrics
import android.view.WindowManager
import io.karte.android.BuildConfig
import io.karte.android.KarteApp
import io.karte.android.core.config.Config
import io.karte.android.core.logger.Logger
import io.karte.android.core.repository.Repository
import org.json.JSONException
import org.json.JSONObject
import java.util.Locale

private const val LOG_TAG = "Karte.AppInfo"

private const val VERSION_NAME_KEY = "app_version_name"
private const val VERSION_CODE_KEY = "app_version_code"
private const val ADVERTISING_ID_KEY = "device_advertising_id"

private interface Serializable {
    fun serialize(): JSONObject
}

/**アプリケーション情報を保持するクラスです。*/
class AppInfo(context: Context, repository: Repository, config: Config) : Serializable {
    private val versionName: String?
    private val versionCode: Int?
    private val karteSdkVersion = BuildConfig.VERSION_NAME
    private val systemInfo = SystemInfo(config.enabledTrackingAaid, Screen(context))
    private val moduleInfo = ModuleInfo()
    private val packageName = context.packageName

    private val prevVersionName: String? = repository.get<String?>(VERSION_NAME_KEY, null)
    private val prevVersionCode: Int = repository.get(VERSION_CODE_KEY, -1)
    /**アプリケーション情報のJSONObjectです。*/
    val json: JSONObject

    init {
        // get current version
        val packageInfo: PackageInfo? = packageName?.let {
            try {
                context.packageManager.getPackageInfo(it, 0)
            } catch (e: PackageManager.NameNotFoundException) {
                Logger.e(LOG_TAG, "Failed to get current package info.", e)
                null
            }
        }
        versionName = packageInfo?.versionName
        versionCode = packageInfo?.versionCode ?: -1

        // write to shared preferences
        if (versionCode != -1 && versionCode != prevVersionCode) {
            repository.put(VERSION_NAME_KEY, versionName)
            repository.put(VERSION_CODE_KEY, versionCode)
        }

        systemInfo.advertisingId = repository.get<String?>(ADVERTISING_ID_KEY, null)
        if (config.enabledTrackingAaid) {
            AdvertisingId.getAdvertisingId(context) { aaid ->
                Logger.d(LOG_TAG, "getAdvertisingId $aaid")
                repository.put(ADVERTISING_ID_KEY, aaid)
                systemInfo.advertisingId = aaid
                updateSystemInfo()
            }
        }
        json = serialize()
        Logger.v(LOG_TAG, "Constructed App info: $json")
    }

    override fun serialize(): JSONObject {
        return try {
            JSONObject()
                .put("version_name", versionName)
                .put("version_code", versionCode.toString())
                .put("karte_sdk_version", karteSdkVersion)
                .put("package_name", packageName)
                .put("system_info", systemInfo.serialize())
                .put("module_info", moduleInfo.serialize())
        } catch (e: JSONException) {
            Logger.e(LOG_TAG, "Failed to construct json.", e)
            JSONObject()
        }
    }

    private fun serializeForUpdate(): JSONObject {
        return try {
            JSONObject(json, json.keys().asSequence().toList().toTypedArray())
                .put("prev_version_name", prevVersionName)
                .put("prev_version_code", prevVersionCode.toString())
        } catch (e: JSONException) {
            Logger.e(LOG_TAG, "Failed to construct json.", e)
            JSONObject()
        }
    }

    internal fun trackAppLifecycle() {
        if (versionCode == -1) {
            return
        }

        if (prevVersionCode == -1) {
            // application installed
            Tracker.track(Event(AutoEventName.NativeAppInstall, json))
        } else if (prevVersionCode != versionCode) {
            // application updated
            Tracker.track(Event(AutoEventName.NativeAppUpdate, serializeForUpdate()))
        }
    }

    internal fun updateModuleInfo() {
        json.put("module_info", moduleInfo.serialize())
    }

    private fun updateSystemInfo() {
        json.put("system_info", systemInfo.serialize())
    }
}

private class ModuleInfo : Serializable {
    override fun serialize(): JSONObject {
        val modules = JSONObject()
        KarteApp.self.libraries.filter { it.isPublic }.forEach {
            modules.put(it.name, it.version)
        }
        modules.put("core", BuildConfig.VERSION_NAME)
        return modules
    }
}

private class SystemInfo(val enabledTrackingAaid: Boolean, val screen: Screen) : Serializable {
    private val os: String = "Android"
    private val osVersion: String? = Build.VERSION.RELEASE
    private val device: String? = Build.DEVICE
    private val brand: String? = Build.BRAND
    private val model: String? = Build.MODEL
    private val product: String? = Build.PRODUCT
    internal var advertisingId: String? = null
    val language: String? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        Locale.getDefault().toLanguageTag()
    } else {
        null
    }

    override fun serialize(): JSONObject {
        val values = JSONObject()
        values.put("os", os)
        osVersion?.let { values.put("os_version", it) }
        device?.let { values.put("device", it) }
        brand?.let { values.put("brand", it) }
        model?.let { values.put("model", it) }
        product?.let { values.put("product", it) }
        if (enabledTrackingAaid) advertisingId?.let { values.put("aaid", it) }
        language?.let { values.put("language", it) }
        values.put("screen", screen.serialize())
        return values
    }
}

private class Screen(context: Context) : Serializable {
    private val width: Int
    private val height: Int

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            val metrics = DisplayMetrics()
            windowManager.defaultDisplay.getRealMetrics(metrics)
            width = (metrics.widthPixels / metrics.density).toInt()
            height = (metrics.heightPixels / metrics.density).toInt()
        } else {
            // SDKの対応バージョンはAndroid 4.4以上なので、ここはケアしなくて良い
            width = 0
            height = 0
        }
    }

    override fun serialize(): JSONObject = JSONObject().put("width", width).put("height", height)
}
