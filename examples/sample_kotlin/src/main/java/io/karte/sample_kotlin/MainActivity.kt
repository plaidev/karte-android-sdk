package io.karte.sample_kotlin

import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import io.karte.android.inappmessaging.InAppMessaging
import io.karte.android.inappmessaging.InAppMessagingDelegate
import io.karte.android.tracking.Tracker
import io.karte.android.visualtracking.VisualTracking
import io.karte.android.visualtracking.VisualTrackingDelegate
import kotlinx.android.synthetic.main.activity_main.*
import java.util.ArrayList
import java.util.HashMap

class MainActivity : AppCompatActivity() {

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

    override fun onResume() {
        super.onResume()
        Tracker.view("main")
    }

    companion object {
        val TAG = MainActivity::class.java.simpleName
    }
}
