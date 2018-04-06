package com.voiceloco.android.sampleapp;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;

import com.google.firebase.iid.FirebaseInstanceId;
import com.voiceloco.android.CallException;
import com.voiceloco.android.CallManager;
import com.voiceloco.android.CallObserver;
import com.voiceloco.android.Register;
import com.voiceloco.android.RegisterException;

import org.json.JSONException;
import org.json.JSONObject;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, CallObserver.Register, CallObserver.Invite {

    public static final String TAG = "MainActivity";
    public static final String CALL_DIRECTION = "callDirection";
    public static final String SEND = "send";
    public static final String RECV = "recv";
    public static final String MY_ACCOUNT = "myAccount";
    public static final String COUNTERPARTY_ACCOUNT = "counterpartyAccount";
    public static final String APP_ID = "appId";
    public static final String id = "s4";
    public static final String appId = "testAppId";
    public String fcmToken;

    private BroadcastReceiver broadcastReceiver;
    private EditText etId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, 0);

        CallManager callManager = CallManager.getInstance();
        callManager.add((CallObserver.Register) this);
        callManager.add((CallObserver.Invite) this);

        etId = findViewById(R.id.et_id);
        ImageView ivCall = findViewById(R.id.iv_call);

        ivCall.setOnClickListener(this);

        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals("action_fcm_token")) {
                    fcmToken = FirebaseInstanceId.getInstance().getToken();
                    getAccessToken();
                }
            }
        };

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("action_fcm_token");

        registerReceiver(broadcastReceiver, intentFilter);

        fcmToken = FirebaseInstanceId.getInstance().getToken();
        if (fcmToken!=null && !fcmToken.equals("")) {
            getAccessToken();
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
        callManager.delete((CallObserver.Invite) this);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.iv_call:
                Intent intent = new Intent(MainActivity.this, CallActivity.class);
                intent.putExtra(CALL_DIRECTION, SEND);
                intent.putExtra(MY_ACCOUNT, id);
                intent.putExtra(COUNTERPARTY_ACCOUNT, etId.getText().toString());
                intent.putExtra(APP_ID, appId);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
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
    public void onIncomingCall(final String counterpartyAccount) {
        Log.d(TAG, "Listener - onIncomingCall");
        Intent intent = new Intent(MainActivity.this, CallActivity.class);
        intent.putExtra(CALL_DIRECTION, RECV);
        intent.putExtra(MY_ACCOUNT, id);
        intent.putExtra(COUNTERPARTY_ACCOUNT, counterpartyAccount);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        //call Activity
    }

    @Override
    public void onCanceledCall(CallException callException) {

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
            return AccessToken.POST("/apps/testAppId/users", params[0]);
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            Log.d("MainActivity", result);
            Register register = Register.getInstance();
            register.start(MainActivity.this, id, appId, result, fcmToken);

        }
    }
}
