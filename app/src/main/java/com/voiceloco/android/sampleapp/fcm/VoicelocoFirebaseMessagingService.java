package com.voiceloco.android.sampleapp.fcm;

import android.content.Intent;
import android.util.Log;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.voiceloco.android.sampleapp.MainActivity;

import java.util.Map;
import java.util.StringTokenizer;

public class VoicelocoFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = "VoiceFCMService";

    @Override
    public void onCreate() {
        super.onCreate();
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
                    // Notify Activity of FCM push
                    Intent intent = new Intent(this, MainActivity.class);
                    intent.setAction(MainActivity.ACTION_INCOMING_CALL);
                    StringTokenizer tokenizer = new StringTokenizer(fromAccount, "@");
                    intent.putExtra(MainActivity.COUNTERPARTY_ACCOUNT, tokenizer.nextToken());
                    intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    this.startActivity(intent);
                    break;
                case "cancel":
                    // Notify Activity of FCM push
                    intent = new Intent();
                    intent.setAction(MainActivity.ACTION_CANCEL_CALL);
                    this.sendBroadcast(intent);
                    break;
            }
        }
    }
}