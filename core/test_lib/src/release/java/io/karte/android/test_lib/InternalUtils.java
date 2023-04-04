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
package io.karte.android.test_lib;

import io.karte.android.KarteApp;
import io.karte.android.core.optout.OptOutConfigKt;
import io.karte.android.tracking.queue.DispatcherKt;
import io.karte.android.utilities.GzipUtilKt;

public class InternalUtils {
    // KarteApp
    public static KarteApp karteApp = KarteApp.Companion.getSelf$core_release();

    public static void teardownKarteApp() {
        KarteApp.Companion.getSelf$core_release().teardown$core_release();
    }

    // Dispatcher
    public static String threadName = DispatcherKt.THREAD_NAME;

    // Utilities
    public static String gunzip(byte[] bytes) {
        return GzipUtilKt.gunzip(bytes);
    }

    // Optout
    public static String prefOptOutKey = OptOutConfigKt.PREF_KEY_OPT_OUT;
}
