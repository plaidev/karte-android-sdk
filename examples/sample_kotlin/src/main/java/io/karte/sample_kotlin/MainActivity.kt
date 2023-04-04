package io.karte.sample_kotlin

import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import io.karte.android.inappmessaging.InAppMessaging
import io.karte.android.inappmessaging.InAppMessagingDelegate
import io.karte.android.inbox.Inbox
import io.karte.android.tracking.Tracker
import io.karte.android.visualtracking.VisualTracking
import io.karte.android.visualtracking.VisualTrackingDelegate
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        throwable.printStackTrace()
    }
    private val scope = CoroutineScope(Job() + Dispatchers.IO + exceptionHandler)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)
        setSupportActionBar(findViewById(R.id.tool_bar))
        supportActionBar?.title = "SampleApplication"
        setEventListeners()
        InAppMessaging.delegate = object : InAppMessagingDelegate() {
            override fun shouldOpenURL(url: Uri): Boolean {
                Log.i(TAG, "shouldOpenURL $url")
                return super.shouldOpenURL(url)
            }
        }

        VisualTracking.delegate = object : VisualTrackingDelegate() {
            override fun onDevicePairingStatusUpdated(isPaired: Boolean) {
                Log.i(TAG, "isPaired $isPaired")
            }
        }
    }

    private fun setEventListeners() {
        send_identify_event.setOnClickListener { sendIdentifyEvent() }

        send_view_event.setOnClickListener { sendViewEvent() }

        send_buy_event.setOnClickListener { sendBuyEvent() }

        button_fetch_inbox.setOnClickListener {
            scope.launch {
                fetchInboxMessages()
            }
        }
    }

    private fun sendIdentifyEvent() {
        val user_id = edit_user_id.getText().toString()
        if (user_id.length > 0) {
            val values = HashMap<String, Any>()
            values["is_app_user"] = true
            Tracker.identify(user_id, values)
        } else {
            Log.w(TAG, "no user_id")
        }
    }

    private fun sendViewEvent() {
        Log.i(TAG, "view event button clicked")
        val viewName = view_name_edit.text.toString()
        val values = HashMap<String, Any>()
        values["title"] = viewName
        Tracker.view(viewName, values)
    }

    private fun sendBuyEvent() {
        Log.i(TAG, "buy event button clicked")
        val values = HashMap<String, Any>()
        values["affiliation"] = "shop name"
        values["revenue"] = (Math.random() * 10000).toInt()
        values["shipping"] = 100
        values["tax"] = 10
        val item = HashMap<String, Any>()
        item["item_id"] = "test"
        item["name"] = "掃除機A"
        item["category"] = arrayOf("家電", "掃除機")
        item["price"] = (Math.random() * 1000).toInt()
        item["quantity"] = 1
        val items = ArrayList<Any>()
        items.add(item)
        values["items"] = items
        Tracker.track("buy", values)
    }

    private suspend fun fetchInboxMessages(limit: Int? = null, latestMessageId: String? = null) {
        // For suspend function version
        val result = Inbox.fetchMessages(limit, latestMessageId)
        result?.let {
            runOnUiThread {
                val count = it.count()
                var content = "Count: ${count}\n"
                for (item in it) {
                    content += "${item}\n"
                }
                inbox_content.text = content
            }
        }
        Log.d(TAG, result.toString())

        // For async version
        // Inbox.fetchMessagesAsync(limit, latestMessageId, Handler(Looper.getMainLooper())) { result ->
        //     result?.let {
        //         val count = it.count()
        //         var content = "Count: ${count}\n"
        //         for(item in it) {
        //             content += "${item}\n"
        //         }
        //         inbox_content.text = content
        //         Log.d(TAG, it.toString())
        //     }
        // }
    }

    override fun onResume() {
        super.onResume()
        Tracker.view("main")
    }

    companion object {
        val TAG = MainActivity::class.java.simpleName
    }
}
