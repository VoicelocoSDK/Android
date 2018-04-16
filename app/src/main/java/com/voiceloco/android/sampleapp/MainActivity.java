package com.voiceloco.android.sampleapp;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.AssetFileDescriptor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.firebase.iid.FirebaseInstanceId;
import com.voiceloco.android.CallException;
import com.voiceloco.android.CallInfo;
import com.voiceloco.android.CallManager;
import com.voiceloco.android.CallObserver;
import com.voiceloco.android.Register;
import com.voiceloco.android.RegisterException;
import com.voiceloco.android.Voice;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, CallObserver.Register, CallObserver.Call, CallObserver.AdvancedCall {

    public static final String TAG = "MainActivity";
    public static final String ACTION_INCOMING_CALL = "incomingCall";
    public static final String ACTION_CANCEL_CALL = "cancelCall";
    public static final String ACTION_FCM_TOKEN = "actionFCMToken";
    public static final String COUNTERPARTY_ACCOUNT = "counterpartyAccount";

    public static final String id = "";
    public static final String appId = "";
    public static final String apiKey = "";
    public String fcmToken;

    private CallInfo callInfo;
    private AudioManager audioManager;
    private MediaPlayer mediaPlayer;
    private Vibrator vibrator;

    private boolean isPlaying;
    private boolean isPause;

    private RelativeLayout rlSend;
    private RelativeLayout rlRecv;
    private TextView tvCallState;
    private TextView tvInformation;
    private ImageView ivMute;
    private ImageView ivSpeaker;

    private Timer timer;
    private TimerTask timerTask;
    private Handler handler;
    private int sec = 0;

    private String counterpartyAccount;
    private boolean createdByPush = false;

    private BroadcastReceiver broadcastReceiver;
    private EditText etId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, 0);

        CallManager callManager = CallManager.getInstance();
        callManager.add((CallObserver.Register) this);
        callManager.add((CallObserver.Call) this);
        callManager.add((CallObserver.AdvancedCall) this);

        audioManager = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
        mediaPlayer = new MediaPlayer();
        vibrator = (Vibrator)getSystemService(Context.VIBRATOR_SERVICE);

        etId = findViewById(R.id.et_id);
        ImageView ivCall = findViewById(R.id.iv_call);

        tvCallState = findViewById(R.id.tv_call_state);
        tvInformation = findViewById(R.id.tv_information);

        rlSend = findViewById(R.id.btns_send);
        ivMute = findViewById(R.id.iv_mute);
        ImageView ivEnd = findViewById(R.id.iv_end);
        ivSpeaker = findViewById(R.id.iv_speaker);

        rlRecv = findViewById(R.id.btns_recv);
        ImageView ivReject = findViewById(R.id.iv_reject);
        ImageView ivRejectMessage = findViewById(R.id.iv_reject_message);
        ImageView ivAccept = findViewById(R.id.iv_accept);

        ivCall.setOnClickListener(this);

        ivMute.setOnClickListener(this);
        ivEnd.setOnClickListener(this);
        ivSpeaker.setOnClickListener(this);

        ivReject.setOnClickListener(this);
        ivRejectMessage.setOnClickListener(this);
        ivAccept.setOnClickListener(this);

        Intent intent = getIntent();

        if (ACTION_INCOMING_CALL.equals(intent.getAction())) {
            Log.d(TAG, "onIncomingCall");
            createdByPush = true;

            counterpartyAccount = intent.getStringExtra(MainActivity.COUNTERPARTY_ACCOUNT);
            tvCallState.setText(String.format(Locale.getDefault(), "receive call from %s", counterpartyAccount));
            rlSend.setVisibility(View.GONE);
            rlRecv.setVisibility(View.VISIBLE);
            makeRingtone();
            makeVibrator();
        }

        handler = new Handler();

        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (ACTION_FCM_TOKEN.equals(intent.getAction())) {
                    fcmToken = FirebaseInstanceId.getInstance().getToken();
                    getAccessToken();
                }

                if (ACTION_CANCEL_CALL.equals(intent.getAction())) {
                    Log.d(TAG, "callCancel");
                    callDisconnected(null);
                }
            }
        };

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ACTION_FCM_TOKEN);
        intentFilter.addAction(ACTION_CANCEL_CALL);

        registerReceiver(broadcastReceiver, intentFilter);

        fcmToken = FirebaseInstanceId.getInstance().getToken();
        if (fcmToken!=null && !fcmToken.equals("")) {
            getAccessToken();
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        Log.d(TAG, "onNewIntent");
        if (ACTION_INCOMING_CALL.equals(intent.getAction())) {
            Log.d(TAG, "onIncomingCall");

            counterpartyAccount = intent.getStringExtra(MainActivity.COUNTERPARTY_ACCOUNT);
            tvCallState.setText(String.format(Locale.getDefault(), "receive call from %s", counterpartyAccount));
            rlSend.setVisibility(View.GONE);
            rlRecv.setVisibility(View.VISIBLE);
            makeRingtone();
            makeVibrator();

            final String fcmToken = FirebaseInstanceId.getInstance().getToken();
            if (fcmToken!=null && !fcmToken.equals("")) {
                getAccessToken();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(broadcastReceiver);
        Register.getInstance().stop();

        CallManager callManager = CallManager.getInstance();
        callManager.delete((CallObserver.Register) this);
        callManager.delete((CallObserver.Call) this);
        callManager.delete((CallObserver.AdvancedCall) this);
        vibrator = null;
        mediaPlayer.release();
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.iv_mute:
                boolean isMute = !callInfo.isMute();
                new Voice().mute(isMute);
                if (isMute) {
                    ivMute.setImageResource(R.drawable.mute_on);
                } else {
                    ivMute.setImageResource(R.drawable.mute_off);
                }
                callInfo.setMute(isMute);
                break;
            case R.id.iv_end:
                new Voice().cancel(callInfo);
                rlSend.setVisibility(View.GONE);
                rlRecv.setVisibility(View.GONE);
                break;
            case R.id.iv_speaker:
                boolean isSpeaker = !callInfo.isSpeaker();
                audioManager.setSpeakerphoneOn(isSpeaker);
                if (isSpeaker) {
                    ivSpeaker.setImageResource(R.drawable.speaker_on);
                } else {
                    ivSpeaker.setImageResource(R.drawable.speaker_off);
                }
                callInfo.setSpeaker(isSpeaker);
                break;
            case R.id.iv_reject:
                Log.d(TAG, "call Reject");
                new Voice().reject();
                break;
            case R.id.iv_reject_message:
                break;
            case R.id.iv_accept:
                callInfo = new Voice().accept(counterpartyAccount, appId);
                rlSend.setVisibility(View.VISIBLE);
                rlRecv.setVisibility(View.GONE);
                break;
            case R.id.iv_call:
                counterpartyAccount = etId.getText().toString();
                callInfo = new Voice().call(this, counterpartyAccount, appId);
                tvCallState.setText(String.format(Locale.getDefault(), "send call to %s", counterpartyAccount));
                rlSend.setVisibility(View.VISIBLE);
                rlRecv.setVisibility(View.GONE);
                InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(etId.getWindowToken(), 0);
                break;
        }
    }

    @Override
    public void onRegistrationSuccess() {
        Log.d(TAG, "onRegistrationSuccess");
    }

    @Override
    public void onRegistrationFailure(RegisterException exception) {
        Log.d(TAG, "onRegistrationFailure");
    }

    @Override
    public void onConnected(final CallInfo callInfo) {
        Log.d(TAG, "onConnected");

        tvCallState.setText(String.format(Locale.getDefault(), "call state is : %s", callInfo.getCallState()));
        startTimer();
        stopRingtone();
        stopVibrator();
    }

    @Override
    public void onDisConnected(final CallInfo callInfo, CallException callException) {
        Log.d(TAG, "onDisconnected");
        callDisconnected(callException);
    }

    @Override
    public void onError(final CallInfo callInfo, CallException exception) {
        Log.d(TAG, "onError");
        callDisconnected(exception);
    }

    @Override
    public void onOutgoingCall() {
        Log.d(TAG, "onOutgoing");
        tvCallState.setText(String.format(Locale.getDefault(), "call state is : %s", callInfo.getCallState()));
        makeRingBackTone();
    }

    @Override
    public void onEarlyMedia() {
        Log.d(TAG, "onEarlyMedia");
        tvCallState.setText(String.format(Locale.getDefault(), "call state is : %s", callInfo.getCallState()));
    }

    @Override
    public void onConnecting() {
        Log.d(TAG, "onConnecting");
        tvCallState.setText(String.format(Locale.getDefault(), "call state is : %s", callInfo.getCallState()));
    }

    private void getAccessToken() {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("userId", id);
            new GetAccessToken().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, jsonObject.toString());

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @SuppressLint("StaticFieldLeak")
    private class GetAccessToken extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... params) {
            Log.d(TAG, params[0]);
            return AccessToken.POST("/apps/"+appId+"/users", params[0], apiKey);
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            Log.d(TAG, result);
            Register register = Register.getInstance();
            register.start(MainActivity.this, id, appId, result, fcmToken);

        }
    }

    private void callDisconnected(CallException callException) {
        if (callException!=null) {
            Log.d(TAG, "Code is : " + callException.getErrCode());
            tvCallState.setText(String.format(Locale.getDefault(), "code : %d, message : %s", callException.getErrCode(), callException.getErrMessage()));
        }

        audioManager.setSpeakerphoneOn(false);
        ivSpeaker.setImageResource(R.drawable.speaker_off);
        ivMute.setImageResource(R.drawable.mute_off);

        tvInformation.setText("통화 종료");
        stopTimer();
        rlSend.setVisibility(View.GONE);
        rlRecv.setVisibility(View.GONE);
        stopRingtone();
        stopVibrator();
        makeEnding();
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (createdByPush) {
                    finish();
                } else {
                    tvCallState.setText("");
                    tvInformation.setText("");
                }
                abandonAudioFocus();
                audioManager.setMode(AudioManager.MODE_NORMAL);
            }
        }, 3000);
    }

    private void startTimer() {
        sec = 0;
        timerTask = new TimerTask() {
            @Override
            public void run() {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        String setText = "";
                        if (sec < 3600)
                            setText += String.format(Locale.getDefault(), "%02d:%02d", sec / 60, sec++ % 60);
                        else
                            setText += String.format(Locale.getDefault(), "%02d:%02d:%02d", sec / 3600, (sec % 3600) / 60, sec++ % 60);
                        tvInformation.setText(setText);
                    }
                });
            }
        };

        timer = new Timer();
        timer.schedule(timerTask, 0, 1000);
    }

    public void stopTimer(){
        if(timerTask != null){
            timerTask.cancel();
            timerTask=null;
        }
        if(timer!=null){
            timer.cancel();
            timer.purge();
            timer=null;
        }
    }

    public void makeEnding() {
        // 소리
        try {
            final MediaPlayer mpEnding = new MediaPlayer();
            mpEnding.setAudioStreamType(AudioManager.STREAM_VOICE_CALL);
            AssetFileDescriptor afd = getResources().openRawResourceFd(R.raw.ending);
            if (afd == null) return;
            mpEnding.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
            afd.close();
            mpEnding.prepare();
            mpEnding.start();
            mpEnding.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mediaPlayer) {
                    mpEnding.stop();
                    mpEnding.release();
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void makeRingtone() {
        int requestResult;
        requestResult = audioManager.requestAudioFocus(mAudioFocusListener, AudioManager.STREAM_RING, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);
//        audioManager.setMode(AudioManager.STREAM_RING);

        switch(audioManager.getRingerMode()) {
            case AudioManager.RINGER_MODE_VIBRATE:
            case AudioManager.RINGER_MODE_SILENT:
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            case AudioManager.RINGER_MODE_NORMAL:
                // 소리
                if(requestResult == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                    try {
                        mediaPlayer.setAudioStreamType(AudioManager.STREAM_RING);
                        AssetFileDescriptor afd = getResources().openRawResourceFd(R.raw.ringtone);
                        if (afd == null) return;
                        mediaPlayer.reset();
                        mediaPlayer.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
                        afd.close();
                        mediaPlayer.prepare();
                        mediaPlayer.start();
                        mediaPlayer.setLooping(true);
                        isPlaying = true;
                    } catch (IOException | IllegalStateException e) {
                        e.printStackTrace();
                    }
                }

                else if (requestResult == AudioManager.AUDIOFOCUS_REQUEST_FAILED){
                    Log.d(TAG,"Failure to request focus listener");
                }
                break;
        }
    }

    private void makeRingBackTone() {
        // 소리
        int requestResult = audioManager.requestAudioFocus(mAudioFocusListener, AudioManager.STREAM_VOICE_CALL,
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);
//        audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
//
        if(requestResult == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            try {
                mediaPlayer.setAudioStreamType(AudioManager.STREAM_VOICE_CALL);
                AssetFileDescriptor afd = getResources().openRawResourceFd(R.raw.ringtone);
                if (afd == null) return;
                mediaPlayer.reset();
                mediaPlayer.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
                afd.close();
                mediaPlayer.prepare();
                mediaPlayer.start();
                mediaPlayer.setLooping(true);
                isPlaying = true;


            } catch (IOException | IllegalStateException e) {
                e.printStackTrace();
            }
        }

        else if (requestResult == AudioManager.AUDIOFOCUS_REQUEST_FAILED){
            Log.d(TAG, "Failure to request focus listener");
        }
    }

    private void stopRingtone(){
        if (isPlaying) {
            isPlaying = false;
            isPause = false;
            mediaPlayer.stop();
        }
    }

    private void makeVibrator(){
        final long[] pattern = {400, 800};
        switch(audioManager.getRingerMode()){
            case AudioManager.RINGER_MODE_VIBRATE:
                // 진동
            case AudioManager.RINGER_MODE_NORMAL:
                // 소리
                vibrator.vibrate(pattern, 0);
                break;
        }
    }

    private void stopVibrator(){
        if(vibrator!=null) {
            vibrator.cancel();
        }
    }

    private AudioManager.OnAudioFocusChangeListener mAudioFocusListener = new AudioManager.OnAudioFocusChangeListener() {
        public void onAudioFocusChange(int focusChange) {
            // AudioFocus is a new feature: focus updates are made verbose on
            // purpose
            switch (focusChange) {
                case AudioManager.AUDIOFOCUS_LOSS:
                    if(isPlaying) {
                        mediaPlayer.pause();
                        isPause = true;
                    }
//                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, currentVolume, 0);
                    Log.d(TAG, "AudioFocus: received AUDIO_FOCUS_LOSS");
                    break;

                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                    if(isPlaying) {
                        mediaPlayer.pause();
                        isPause = true;
                    }
                    Log.d(TAG, "AudioFocus: received AUDIO_FOCUS_LOSS_TRANSIENT");
                    break;

                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                    if(isPlaying) {
                        mediaPlayer.setVolume(0.5f, 0.5f);
                    }
                    Log.d(TAG, "AudioFocus: received AUDIO_FOCUS_LOSS_TRANSIENT_CAN_DUCK");
                    break;

                case AudioManager.AUDIOFOCUS_GAIN:
                    if(isPause) {
                        isPause = false;
                        mediaPlayer.start();
                        mediaPlayer.setVolume(1.0f, 1.0f);
                    }
                    Log.d(TAG, "AudioFocus: received AUDIO_FOCUS_GAIN");
                    break;

                default:
                    Log.e(TAG, "Unknown audio focus change code");
            }

        }
    };

    public void abandonAudioFocus() {
        //Abandon audio focus
        int result = audioManager.abandonAudioFocus(mAudioFocusListener);
        if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            Log.d(TAG, "Audio focus abandoned");
        } else {
            Log.e(TAG, "Audio focus failed to abandon");
        }
    }
}