package com.voiceloco.android.sampleapp.fcm;

import android.app.NotificationManager;
import android.content.Context;
import android.util.Log;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Map;

public class VoicelocoFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = "VoiceFCMService";
    private static final String NOTIFICATION_ID_KEY = "NOTIFICATION_ID";
    private static final String CALL_SID_KEY = "CALL_SID";
    private static final String VOICE_CHANNEL = "default";

    private NotificationManager notificationManager;

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
                    break;
            }
        }
    }
}
