package io.karte.sample_java;

import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.karte.android.inappmessaging.InAppMessaging;
import io.karte.android.inappmessaging.InAppMessagingDelegate;
import io.karte.android.tracking.Tracker;
import io.karte.android.visualtracking.VisualTracking;
import io.karte.android.visualtracking.VisualTrackingDelegate;

public class MainActivity extends AppCompatActivity {
    public static final String TAG = MainActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        setSupportActionBar((Toolbar) findViewById(R.id.tool_bar));
        getSupportActionBar().setTitle("SampleApplication");
        setEventListeners();

        InAppMessaging.setDelegate(new InAppMessagingDelegate() {
            @Override
            public boolean shouldOpenURL(@NonNull Uri url) {
                Log.i(TAG, "shouldOpenURL " + url);
                return super.shouldOpenURL(url);
            }
        });

        VisualTracking.setDelegate(new VisualTrackingDelegate() {
            @Override
            public void onDevicePairingStatusUpdated(boolean isPaired) {
                Log.i(TAG, "isPaired " + isPaired);
            }
        });
    }


    private void setEventListeners() {
        final Button buttonIdentifyEvent = findViewById(R.id.send_identify_event);
        buttonIdentifyEvent.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendIdentifyEvent();
            }
        });

        final Button buttonViewEvent = findViewById(R.id.send_view_event);
        buttonViewEvent.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendViewEvent();
            }
        });

        final Button buttonBuyEvent = findViewById(R.id.send_buy_event);
        buttonBuyEvent.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendBuyEvent();
            }
        });

    }

    private void sendIdentifyEvent() {

        final EditText editText = findViewById(R.id.edit_user_id);
        final String user_id = editText.getText().toString();
        if (user_id.length() > 0) {
            Map<String, Object> values = new HashMap<>();
            values.put("is_app_user", true);
            Tracker.identify(user_id, values);
        } else {
            Log.w(TAG, "no user_id");
        }
    }

    private void sendViewEvent() {
        Log.i(TAG, "view event button clicked");
        String viewName = ((EditText) findViewById(R.id.view_name_edit)).getText().toString();
        Map<String, Object> values = new HashMap<>();
        values.put("title", viewName);
        Tracker.view(viewName, values);
    }

    private void sendBuyEvent() {
        Log.i(TAG, "buy event button clicked");
        Map<String, Object> values = new HashMap<>();
        values.put("affiliation", "shop name");
        values.put("revenue", (int) (Math.random() * 10000));
        values.put("shipping", 100);
        values.put("tax", 10);
        Map<String, Object> item = new HashMap<>();
        item.put("item_id", "test");
        item.put("name", "掃除機A");
        item.put("category", new String[]{"家電", "掃除機"});
        item.put("price", (int) (Math.random() * 1000));
        item.put("quantity", 1);
        List<Object> items = new ArrayList<>();
        items.add(item);
        values.put("items", items);
        Tracker.track("buy", values);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Tracker.view("main");
    }
}
