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
import io.karte.android.core.logger.Logger
import java.util.concurrent.Executors

private const val LOG_TAG = "Karte.AdvertisingId"

internal object AdvertisingId {
    fun getAdvertisingId(context: Context, completion: (String) -> Unit) {
        try {
            try {
                getByAndroidX(context, completion)
                return
            } catch (e: NoClassDefFoundError) {
                Logger.d(LOG_TAG, "Not found package: androidx.ads.identifier.")
            }
            try {
                getByGms(context, completion)
                return
            } catch (e: NoClassDefFoundError) {
                Logger.d(LOG_TAG, "Not found package: com.google.android.gms.ads.identifier.")
            }
        } catch (e: Exception) {
            Logger.e(LOG_TAG, "Failed to get AdvertisingId: '${e.message}'")
        }
    }

    private fun getByAndroidX(context: Context, completion: (String) -> Unit) {
        if (androidx.ads.identifier.AdvertisingIdClient.isAdvertisingIdProviderAvailable(context)) {
            Logger.d(LOG_TAG, "Try to get advertising id by androidx.ads.")
            val future = androidx.ads.identifier.AdvertisingIdClient.getAdvertisingIdInfo(context)
            future.addListener(Runnable {
                val info = future.get()
                Logger.d(LOG_TAG, "Got advertising id: ${info.id}")
                completion(info.id)
            }, Executors.newSingleThreadExecutor())
        } else {
            Logger.w(LOG_TAG, "Advertising id is opt outed.")
        }
    }

    private fun getByGms(context: Context, completion: (String) -> Unit) {
        Logger.d(
            LOG_TAG,
            "Try to get advertising id by ${com.google.android.gms.ads.identifier.AdvertisingIdClient::class.java}"
        )
        Thread {
            try {
                val info =
                    com.google.android.gms.ads.identifier.AdvertisingIdClient.getAdvertisingIdInfo(
                        context
                    )
                if (!info.isLimitAdTrackingEnabled) {
                    Logger.d(LOG_TAG, "Got advertising id: ${info.id}")
                    completion(info.id)
                } else {
                    Logger.w(LOG_TAG, "Advertising id is opt outed.")
                }
            } catch (e: Exception) {
                Logger.e(LOG_TAG, "Failed to get AdvertisingId: '${e.message}'")
            }
        }.start()
    }
}
