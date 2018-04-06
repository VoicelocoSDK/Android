package com.voiceloco.android.sampleapp.fcm;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.voiceloco.android.sampleapp.CallActivity;
import com.voiceloco.android.sampleapp.MainActivity;

import java.util.Map;
import java.util.StringTokenizer;

public class VoicelocoFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = "VoiceFCMService";
    private static final String NOTIFICATION_ID_KEY = "NOTIFICATION_ID";
    private static final String CALL_SID_KEY = "CALL_SID";
    private static final String VOICE_CHANNEL = "default";

    private NotificationManager notificationManager;
    private Notification.Builder mBuilder;

    @Override
    public void onCreate() {
        super.onCreate();
        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
    }

    /**
     * Called when message is received.
     *
     * @param remoteMessage Object representing the message received from Firebase Cloud Messaging.
     */
    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {

        Log.d(TAG, "From: " + remoteMessage.getFrom());

        Map<String, String> messageMap = remoteMessage.getData();
        String event = messageMap.get("eventType");
        String title = messageMap.get("title");
        String message = messageMap.get("message");

        Log.d(TAG, "Title: " + title);
        Log.d(TAG, "Message: " + message);
        Log.d(TAG, "eventType: " + event);

        String fromAccount = messageMap.get("caller");
        Log.d(TAG, "caller - " + fromAccount);
        Log.d(TAG, "callee - " + messageMap.get("callee"));

//        if(title.equals())
        if (event != null) {
            switch (event) {
                case "call" :
                    Intent intent = new Intent(this, CallActivity.class);
                    intent.setAction(CallActivity.ACTION_INCOMING_CALL);
                    StringTokenizer tokenizer = new StringTokenizer(fromAccount, "@");
                    intent.putExtra(MainActivity.COUNTERPARTY_ACCOUNT, tokenizer.nextToken());
//                    intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    this.startActivity(intent);
                    break;
            }
        }
    }
}
