package com.voiceloco.android.sampleapp;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Vibrator;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.firebase.iid.FirebaseInstanceId;
import com.voiceloco.android.CallException;
import com.voiceloco.android.CallInfo;
import com.voiceloco.android.CallManager;
import com.voiceloco.android.CallObserver;
import com.voiceloco.android.Register;
import com.voiceloco.android.Voice;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

public class CallActivity extends AppCompatActivity implements View.OnClickListener, CallObserver.Invite, CallObserver.Call, CallObserver.AdvancedCall {

    private static final String TAG = "CallActivity";
    public static final String ACTION_INCOMING_CALL = "incoming_call";
    public static final String COUNTERPARTY_ACCOUNT = "counterparty_account";

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

    private String myAccount;
    private String counterpartyAccount;
    private String appId;
    private boolean isRegistered = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_call);

        CallManager callManager = CallManager.getInstance();
        callManager.add((CallObserver.Invite) this);
        callManager.add((CallObserver.Call) this);
        callManager.add((CallObserver.AdvancedCall) this);

        audioManager = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
        mediaPlayer = new MediaPlayer();
        vibrator = (Vibrator)getSystemService(Context.VIBRATOR_SERVICE);

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

        ivMute.setOnClickListener(this);
        ivEnd.setOnClickListener(this);
        ivSpeaker.setOnClickListener(this);

        ivReject.setOnClickListener(this);
        ivRejectMessage.setOnClickListener(this);
        ivAccept.setOnClickListener(this);

        Intent intent = getIntent();
        if (MainActivity.SEND.equals(intent.getStringExtra(MainActivity.CALL_DIRECTION))) {
            counterpartyAccount = intent.getStringExtra(MainActivity.COUNTERPARTY_ACCOUNT);
            myAccount = intent.getStringExtra(MainActivity.MY_ACCOUNT);
            appId = intent.getStringExtra(MainActivity.APP_ID);
            callInfo = new Voice().call(this, counterpartyAccount, appId);
            tvCallState.setText(String.format(Locale.getDefault(), "send call to %s", counterpartyAccount));
            rlSend.setVisibility(View.VISIBLE);
            rlRecv.setVisibility(View.GONE);
        } else if (MainActivity.RECV.equals(intent.getStringExtra(MainActivity.CALL_DIRECTION))) {
            counterpartyAccount = intent.getStringExtra(MainActivity.COUNTERPARTY_ACCOUNT);
            myAccount = intent.getStringExtra(MainActivity.MY_ACCOUNT);
            tvCallState.setText(String.format(Locale.getDefault(), "receive call from %s", counterpartyAccount));
            rlSend.setVisibility(View.GONE);
            rlRecv.setVisibility(View.VISIBLE);
            makeRingtone();
            makeVibrator();
        }

        if (ACTION_INCOMING_CALL.equals(intent.getAction())) {
            Log.d(TAG, "onIncomingCall");
            isRegistered = true;

            final String fcmToken = FirebaseInstanceId.getInstance().getToken();
            if (fcmToken!=null && !fcmToken.equals("")) {
                getAccessToken();
            }
        }

        handler = new Handler();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        CallManager callManager = CallManager.getInstance();
        callManager.delete((CallObserver.Invite) this);
        callManager.delete((CallObserver.Call) this);
        callManager.delete((CallObserver.AdvancedCall) this);
        if (isRegistered) {
            Register.getInstance().stop();
        }
        abandonAudioFocus();
        audioManager.setMode(AudioManager.MODE_NORMAL);
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
                new Voice().reject();
                rlSend.setVisibility(View.GONE);
                rlRecv.setVisibility(View.GONE);
                stopRingtone();
                stopVibrator();
                Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        finish();
                    }
                }, 3000);
                break;
            case R.id.iv_reject_message:
                break;
            case R.id.iv_accept:
                callInfo = new Voice().accept(counterpartyAccount,appId);
                rlSend.setVisibility(View.VISIBLE);
                rlRecv.setVisibility(View.GONE);
                break;
        }
    }

    @Override
    public void onIncomingCall(String counterpartyAccount) {
        tvCallState.setText(String.format(Locale.getDefault(), "receive call from %s", counterpartyAccount));
        rlSend.setVisibility(View.GONE);
        rlRecv.setVisibility(View.VISIBLE);
        makeRingtone();
        makeVibrator();
    }

    @Override
    public void onCanceledCall(CallException callException) {
        tvCallState.setText(String.format(Locale.getDefault(), "code : %d, message : %s", callException.getErrCode(), callException.getErrMessage()));
        tvInformation.setText("통화 종료");
        rlSend.setVisibility(View.GONE);
        rlRecv.setVisibility(View.GONE);
        stopRingtone();
        stopVibrator();
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                finish();
            }
        }, 3000);
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
    public void onDisConnected(CallInfo callInfo, CallException callException) {
        Log.d(TAG, "onDisconnected");
        Log.d(TAG, "Code is : " + callException.getErrCode());

        audioManager.setSpeakerphoneOn(false);
        ivSpeaker.setImageResource(R.drawable.speaker_off);
        ivMute.setImageResource(R.drawable.mute_off);

        tvCallState.setText(String.format(Locale.getDefault(), "code : %d, message : %s", callException.getErrCode(), callException.getErrMessage()));
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
                finish();
            }
        }, 3000);
    }

    @Override
    public void onError(CallInfo callInfo, CallException exception) {
        Log.d(TAG, "onError");
        Log.d(TAG, "Code is : " + exception.getErrCode());
        stopRingtone();
        stopVibrator();
        makeEnding();
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                finish();
            }
        }, 3000);
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
            Log.d(TAG, "myAccount " + MainActivity.id);
            jsonObject.put("userId", myAccount);
            Log.d("Voiceloco", jsonObject.toString());
            new GetAccessToken().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, jsonObject.toString());

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @SuppressLint("StaticFieldLeak")
    private class GetAccessToken extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... params) {
            return AccessToken.POST("/apps/" + MainActivity.appId + "/users", params[0]);
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            Register register = Register.getInstance();
            register.start(CallActivity.this, MainActivity.id, MainActivity.appId, result, FirebaseInstanceId.getInstance().getToken());
        }
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
            timerTask.cancel(); //타이머task를 timer 큐에서 지워버린다
            timerTask=null;
        }
        if(timer!=null){
            timer.cancel(); //스케쥴task과 타이머를 취소한다.
            timer.purge(); //task큐의 모든 task를 제거한다.
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
            mediaPlayer.release();
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
            vibrator = null;
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
