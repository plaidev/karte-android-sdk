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

import android.app.Activity
import android.app.Application
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import io.karte.android.core.config.Config
import io.karte.android.core.library.ActionModule
import io.karte.android.core.library.CommandModule
import io.karte.android.core.library.DeepLinkModule
import io.karte.android.core.library.Library
import io.karte.android.core.library.LibraryConfig
import io.karte.android.core.library.Module
import io.karte.android.core.library.NotificationModule
import io.karte.android.core.logger.LogLevel
import io.karte.android.core.logger.Logger
import io.karte.android.core.optout.OptOutConfig
import io.karte.android.core.repository.PreferenceRepository
import io.karte.android.core.repository.Repository
import io.karte.android.tracking.AppInfo
import io.karte.android.tracking.AutoEventName
import io.karte.android.tracking.Event
import io.karte.android.tracking.PvId
import io.karte.android.tracking.TrackingService
import io.karte.android.tracking.VisitorId
import io.karte.android.tracking.generateOriginalPvId
import io.karte.android.utilities.ActivityLifecycleCallback
import io.karte.android.utilities.connectivity.ConnectivityObserver
import java.util.ServiceLoader

private const val LOG_TAG = "KarteApp"

/**
 * KARTE SDKのエントリポイントであると共に、SDKの構成および依存ライブラリ等の管理を行うクラスです。
 *
 * SDKを利用するには、[KarteApp.setup]を呼び出し初期化を行う必要があります。
 *
 * 初期化が行われていない状態では、イベントのトラッキングを始め、SDKの機能が利用できません。
 *
 * なおアプリ内メッセージ等のサブモジュールについても同様です。
 *
 *
 * SDKの設定については、初期化時に一部変更することが可能です。
 * 設定を変更して初期化を行う場合は、[Config]を指定して[KarteApp.setup]を呼び出してください。
 */
class KarteApp private constructor() : ActivityLifecycleCallback() {
    /** [KarteApp.setup] 呼び出し時に指定した[Application]インスタンスを返します。 */
    lateinit var application: Application private set

    /**
     * [KarteApp.setup] 呼び出し時に指定したアプリケーションキーを返します。
     *
     * 初期化が行われていない場合は空文字列を返します。
     */
    val appKey: String get() = config.appKey

    /**
     * [KarteApp.setup] 呼び出し時に指定した設定情報を返します。
     *
     * 初期化が行われていない場合はデフォルトの設定情報を返します。
     */
    var config: Config = Config.build()
        private set

    /**
     * 指定したクラスのライブラリ設定を返します。
     *
     * - 該当クラスが存在しない場合、`null` を返します。
     * - 該当クラスが複数存在する場合、最初の設定のみを返します。
     *
     * @param [clazz] [LibraryConfig]を実装したクラス
     */
    fun <R : LibraryConfig> libraryConfig(clazz: Class<R>): R? {
        return config.libraryConfigs.filterIsInstance(clazz).firstOrNull()
    }

    /** アプリケーション情報を返します。 */
    var appInfo: AppInfo? = null
        private set
    internal var connectivityObserver: ConnectivityObserver? = null
    internal var tracker: TrackingService? = null
    private var visitorId: VisitorId? = null
    private var optOutConfig: OptOutConfig? = null

    internal val libraries: MutableList<Library> = mutableListOf()
    internal val modules: MutableList<Module> = mutableListOf()
    private val isUnsupportedOsVersion: Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP
    internal val isInitialized get() = this.appKey.isNotEmpty()

    /** 現在のオリジナルページビューIDを返します。 */
    val originalPvId = generateOriginalPvId()
    internal val pvIdContainer: PvId = PvId(originalPvId)

    /** 現在のページビューIDを返します。*/
    val pvId get() = pvIdContainer.value

    /**
     * モジュールを登録します。
     * @param[module] [Module]を実装したインスタンス
     */
    fun register(module: Module) {
        Logger.i(LOG_TAG, "Register module: ${module.javaClass.name}(${module.name})")
        if (self.modules.none { it == module }) {
            self.modules.add(module)
        }
    }

    /**
     * モジュールの登録を解除します。
     * @param[module] [Module]を実装したインスタンス
     */
    fun unregister(module: Module) {
        Logger.i(LOG_TAG, "Unregister module: ${module.name}")
        self.modules.removeAll { it == module }
    }

    /**
     * 永続化等に使用する [Repository] インスタンスを返します。
     * @param[namespace] 永続化の領域分割をするNamespaceを指定します。
     */
    fun repository(namespace: String = ""): Repository {
        return PreferenceRepository(application, appKey, namespace)
    }

    internal fun teardown() {
        firstActivityCreated = false
        activityCount = 0
        libraries.forEach { library ->
            library.unconfigure(self)
        }
        libraries.clear()
        tracker?.teardown()

        config = Config.build()
        appInfo = null
        connectivityObserver = null
        tracker = null
        visitorId = null
        optOutConfig = null
    }

    /**
     * 一時的（アプリの次回起動時まで）にオプトアウトします。
     *
     * なお初期化が行われていない状態で呼び出した場合はオプトアウトは行われません。
     */
    fun optOutTemporarily() {
        OptOutConfig.optOutTemporarily()
    }

    /**
     * コマンドスキームを処理し、結果を返します。
     *
     * **SDK内部で利用するクラスであり、通常のSDK利用でこちらのクラスを利用することはありません。**
     *
     * @param[uri] コマンドを表現するURI
     * @param[isDelay] 通知タップなど、即時に実行すべきでない場合にtrueとします。デフォルトはfalseです。
     */
    @JvmOverloads
    fun executeCommand(uri: Uri, isDelay: Boolean = false): List<Any?> {
        return modules.filterIsInstance<CommandModule>().filter { it.validate(uri) }.map { it.execute(uri, isDelay) }
    }

    private fun handleDeeplink(intent: Intent) {
        self.modules.filterIsInstance<DeepLinkModule>()
            .forEach { it.handle(intent) }
    }

    //region ActivityLifecycleCallback
    private var firstActivityCreated = false
    private var activityCount = 0
    private var presentActivityHash: Int? = null

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        Logger.v(LOG_TAG, "onActivityCreated $activity")
        if (!firstActivityCreated) {
            self.appInfo?.trackAppLifecycle()
            self.tracker?.track(Event(AutoEventName.NativeAppOpen, values = null))
            firstActivityCreated = true
        }
        handleDeeplink(activity.intent)
    }

    override fun onActivityStarted(activity: Activity) {
        Logger.v(LOG_TAG, "onActivityStarted $activity")
        if (++activityCount == 1) {
            self.tracker?.track(Event(AutoEventName.NativeAppForeground, values = null))
        }
        handleDeeplink(activity.intent)
    }

    override fun onActivityResumed(activity: Activity) {
        val isNextActivity = presentActivityHash != activity.hashCode()
        Logger.v(LOG_TAG, "onActivityResumed $activity isNext:$isNextActivity")
        if (isNextActivity && config.isAutoScreenBoundaryEnabled) {
            self.pvIdContainer.renew()
        }
        presentActivityHash = activity.hashCode()
    }

    override fun onActivityPaused(activity: Activity) {
        Logger.v(LOG_TAG, "onActivityPaused $activity")
        self.pvIdContainer.set(self.originalPvId)
    }

    override fun onActivityStopped(activity: Activity) {
        Logger.v(LOG_TAG, "onActivityStopped $activity")
        if (--activityCount == 0) {
            self.tracker?.track(Event(AutoEventName.NativeAppBackground, values = null))
            Logger.flush()
        }
    }
    //endregion

    companion object {
        internal val self = KarteApp()

        /**
         * SDKの初期化を行います。
         *
         * 初期化オプションが未指定の場合は、デフォルト設定で初期化が行われます。
         * 初期化オプションのデフォルト値については `Configuration` クラスを参照してください。
         *
         * なお初期化後に初期化オプションを変更した場合、その変更はSDKには反映されません。
         *
         * また既に初期化されている状態で呼び出した場合は何もしません。
         *
         * @param[context] [Context]
         * @param[appKey] アプリケーションキー
         * @param[config] 設定
         */
        @JvmStatic
        @JvmOverloads
        fun setup(context: Context, appKey: String, config: Config? = null) {
            setup(context, (config ?: Config.build()).apply { this.appKey = appKey })
        }

        /**
         * SDKの初期化を行います。
         *
         * 初期化オプションが未指定の場合は、デフォルト設定で初期化が行われます。
         * 初期化オプションのデフォルト値については `Configuration` クラスを参照してください。
         *
         * なお初期化後に初期化オプションを変更した場合、その変更はSDKには反映されません。
         *
         * また既に初期化されている状態で呼び出した場合は何もしません。
         *
         * @param[context] [Context]
         * @param[config] 設定
         */
        @JvmStatic
        @JvmOverloads
        fun setup(context: Context, config: Config? = null) {
            if (self.isInitialized) {
                Logger.w(LOG_TAG, "APP_KEY is already exists.")
                return
            }
            val configWithAppKey = Config.fillFromResource(context, config)
            if (!configWithAppKey.isValidAppKey) {
                Logger.w(LOG_TAG, "Invalid APP_KEY is set. ${configWithAppKey.appKey}")
                return
            }
            if (self.isUnsupportedOsVersion) {
                Logger.i(LOG_TAG, "Initializing was canceled because os version is under 5.0.")
                return
            }
            if (configWithAppKey.isDryRun) {
                Logger.w(
                    LOG_TAG,
                    "======================================================================"
                )
                Logger.w(LOG_TAG, "Running mode is dry run.")
                Logger.w(
                    LOG_TAG,
                    "======================================================================\n"
                )
                return
            }
            self.application = if (context.applicationContext is Application) {
                context.applicationContext as Application
            } else {
                Logger.i(LOG_TAG, "Application context is not an Application instance.")
                return
            }
            self.application.registerActivityLifecycleCallbacks(self)
            self.connectivityObserver = ConnectivityObserver(self.application)

            self.config = configWithAppKey
            Logger.i(LOG_TAG, "KARTE SDK initialize. appKey=${self.appKey}, config=$config")
            val repository = self.repository()
            self.appInfo = AppInfo(context, repository, self.config)
            self.visitorId = VisitorId(repository)
            self.optOutConfig = OptOutConfig(self.config, repository)
            self.tracker = TrackingService()

            Logger.v(LOG_TAG, "load libraries")
            val libraries =
                ServiceLoader.load(Library::class.java, KarteApp::class.java.classLoader)
            libraries.forEach { register(it) }
            Logger.v(
                LOG_TAG, "auto loaded libraries: ${libraries.count()}, " +
                    "all libraries: ${self.libraries.count()}. start configure."
            )
            self.libraries.forEach { it.configure(self) }
            self.appInfo?.updateModuleInfo()
        }

        /**
         * ログレベルを設定します。
         *
         * なおデフォルトのログレベルは [LogLevel.WARN] です。
         *
         * @param[level] ログレベル
         */
        @JvmStatic
        fun setLogLevel(level: LogLevel) {
            Logger.level = level
        }

        /**
         * ライブラリを登録します。
         *
         * なお登録処理は `KarteApp.setup(appKey:)` を呼び出す前に行う必要があります。
         * @param[library] [Library] を実装したインスタンス
         */
        @JvmStatic
        fun register(library: Library) {
            Logger.i(
                LOG_TAG,
                "Register library: ${library.name}, ${library.version}, ${library.isPublic}"
            )
            if (self.libraries.none { it.name == library.name }) {
                self.libraries.add(library)
            }
        }

        /**
         * ライブラリの登録を解除します。
         * @param[library] [Library] を実装したインスタンス
         */
        @JvmStatic
        fun unregister(library: Library) {
            Logger.i(LOG_TAG, "Unregister library: ${library.name}")
            self.libraries.removeAll { it.name == library.name }
        }

        /**
         * オプトアウトの設定有無を返します。
         *
         * オプトアウトされている場合は `true` を返し、されていない場合は `false` を返します。
         * また初期化が行われていない場合は `false` を返します。
         */
        @JvmStatic
        val isOptOut: Boolean
            get() = self.optOutConfig?.isOptOut ?: false

        /**
         * オプトインします。
         *
         * なお初期化が行われていない状態で呼び出した場合はオプトインは行われません。
         */
        @JvmStatic
        fun optIn() {
            self.optOutConfig?.optIn()
        }

        /**
         * オプトアウトします。
         *
         * なお初期化が行われていない状態で呼び出した場合はオプトアウトは行われません。
         */
        @JvmStatic
        fun optOut() {
            if (self.optOutConfig == null || isOptOut) {
                return
            }

            self.modules.forEach { if (it is ActionModule) it.resetAll() }
            self.modules.forEach { if (it is NotificationModule) it.unsubscribe() }

            self.optOutConfig?.optOut()
        }

        /**
         * ユーザーを識別するためのID（ビジターID）を返します。
         *
         * 初期化が行われていない場合は空文字列を返します。
         */
        @JvmStatic
        val visitorId: String
            get() = self.visitorId?.value ?: ""

        /**
         * ビジターIDを再生成します。
         *
         * ビジターIDの再生成は、現在のユーザーとは異なるユーザーとして計測したい場合などに行います。
         * 例えば、アプリケーションでログアウトした際などがこれに該当します。
         *
         * なお初期化が行われていない状態で呼び出した場合は再生成は行われません。
         */
        @JvmStatic
        fun renewVisitorId() {
            self.visitorId?.renew()
        }

        /**
         * 渡されたintentを使用してKARTEのDeeplink処理を行います。
         *
         * launchModeにsingleTopなどを利用していて、[Activity.onNewIntent]にて渡される新しいintentで、KARTEのdeeplink処理を行いたい場合に呼び出してください。
         *
         * @param[intent] [Activity.onNewIntent]等で新しく渡された[Intent]
         */
        @JvmStatic
        fun onNewIntent(intent: Intent?) {
            intent?.let { self.handleDeeplink(it) }
        }

        /**
         * URLを開きます。
         * **SDK内部で利用するために用意している機能であり、通常利用で使用することはありません。**
         *
         * @param [uri] 対象のURI
         * @param [context] [Context]
         */
        @JvmStatic
        fun openUrl(uri: Uri, context: Context?): Boolean {
            try {
                var intent =
                    self.executeCommand(uri).filterIsInstance<Intent>().firstOrNull()
                if (intent == null) {
                    intent = Intent(Intent.ACTION_VIEW).apply { data = uri }
                }
                if (context != null) {
                    context.startActivity(intent)
                } else {
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    self.application.startActivity(intent)
                }
            } catch (e: ActivityNotFoundException) {
                Logger.e(LOG_TAG, "Failed to open url.", e)
                return false
            }
            return true
        }
    }
}
