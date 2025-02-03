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
package io.karte.android.utilities.connectivity

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.annotation.RequiresApi
import io.karte.android.core.logger.Logger

private const val LOG_TAG = "Karte.Connectivity"

internal object Connectivity {
    fun isOnline(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            isOnlineNow(context)
        } else {
            isOnlineLegacy(context)
        }
    }

    @Suppress("DEPRECATION")
    private fun isOnlineLegacy(context: Context): Boolean {
        val manager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        return manager.activeNetworkInfo?.isConnectedOrConnecting ?: false
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun isOnlineNow(context: Context): Boolean {
        val manager = context.getSystemService(ConnectivityManager::class.java)
        return runCatching {
            manager.getNetworkCapabilities(manager.activeNetwork)
                ?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        }.onFailure { e ->
            Logger.e(LOG_TAG, "Failed to get the NetworkCapabilities", e)
        }.getOrNull() ?: false
    }
}

internal typealias ConnectivitySubscriber = (Boolean) -> Unit

internal class ConnectivityObserver(private val context: Context) {
    private val connectivityManager: ConnectivityManager
        get() = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val subscribers = mutableSetOf<ConnectivitySubscriber>()
    private val mainHandler = Handler(context.mainLooper)

    private lateinit var callback: ConnectivityManager.NetworkCallback

    init {
        runCatching { start() }
    }

    private fun start() {
        callback = object : ConnectivityManager.NetworkCallback() {
            val networks = mutableSetOf<Network>()

            override fun onLost(network: Network) {
                super.onLost(network)
                networks.remove(network)
                flush(networks.isNotEmpty())
            }

            override fun onAvailable(network: Network) {
                super.onAvailable(network)
                networks.add(network)
                flush(networks.isNotEmpty())
            }
        }
        val builder = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M)
            builder.addCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        val request = builder.build()
        connectivityManager.registerNetworkCallback(request, callback)
    }

    private fun flush(isOnline: Boolean) {
        runOnMainThread { subscribers.forEach { it.invoke(isOnline) } }
    }

    fun subscribe(subscriber: ConnectivitySubscriber) {
        runOnMainThread { subscribers.add(subscriber) }
    }

    fun unsubscribe(subscriber: ConnectivitySubscriber) {
        runOnMainThread { subscribers.remove(subscriber) }
    }

    private fun runOnMainThread(function: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            function.invoke()
        } else {
            mainHandler.post(function)
        }
    }
}
