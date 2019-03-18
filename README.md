# Voiceloco Call SDK Android 시작하기

> ## 시작하기

* ### 시스템 요구사항

  [Sample project ](https://github.com/VoicelocoSDK/voice-sample-android)를 빌드하기 위해서는 [Android Studio](https://developer.android.com/studio/index.html)와 최소 17레벨 이상의 SDK 플랫폼, 그리고 지원 라이브러리가 설치되어야 합니다.

* ### Gradle 설정 수정

  Voiceloco의 Android SDK를 사용하기 위해서는 아래의 내용들을 프로젝트의 `build.gradle`파일에 추가해야 합니다.

  ```
  repositories {
        mavenCentral()
        jcenter()
    }
  }

  dependencies {
    // The Voice SDK resides on jCenter
    compile 'com.voiceloco:android:0.2.2'
  }
  ```

  최신 버전은 현재 0.2.2 버전입니다.

* ### AccessToken 요청 및 미디어 서버 등록

  보이스로코 Call SDK를 사용하기 위해서는 사전에 등록된 appId와 UserId를 사용하여 accessToken을 받아서 미디어 서버에 등록을 해야 합니다. accessToken을 받아오는 부분은 아래 코드입니다.

  ```
  jsonObject.put("userId", id);
  new GetAccessToken().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, jsonObject.toString(), apiKey);
  ```

  **위 과정은 자체 서버를 제작하여 accessToken의 유출을 방지할 것을 추천합니다** <br>
   현재 SampleApp에서는 appId, apiKey, id를 아래 부분에 정의해서 사용해야합니다. <br>
    실제 개발에서는 실제 등록된 데이터를 사용하여야 합니다. <br>
    등록을 원하시면 support@voiceloco.com로 연락하면 등록해드립니다. 
    ```
    public static final String id = "";
    public static final String appId = "";
    public static final String apiKey = "";
    ```

  받은 AccessToken을 통해 미디어 서버에 등록하는 방법은 SDK의 Register 클레스를 이용합니다.

  ```
    Register register = Register.getInstance();
    register.start(MainActivity.this, id, appId, result, fcmToken);
  ```

* ### 권한 설정

  보이스로코 Call SDK를 사용하여 전화를 걸거나 받기 위해서는 fcmToken을 Voiceloco 서버에 등록해야 합니다. 등록 방법은 아래 소스코드와 같습니다.

  ```
   //전화 걸때 Register 방법
   final String fcmToken = FirebaseInstanceId.getInstance().getToken();

   if (fcmToken!=null && !fcmToken.equals("")) {
      Register register = Register.getInstance();
      register.start(MainActivity.this, id, "", "", fcmToken);
   }
  ```

  Sample App을 사용하기 위해 AndroidManifest에 해당 Permission을 등록해줍니다.

  ```
   <uses-permission android:name="android.permission.INTERNET" />
   <uses-permission android:name="android.permission.MODIFY_AUDIO_SETTINGS" />
   <uses-permission android:name="android.permission.RECORD_AUDIO" />
   <uses-permission android:name="android.permission.VIBRATE" />
   <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
  ```

* ### 화면 구성

  전화를 발신하기 위해서 상대방 아이디를 입력받을 editText와 이벤트를 위한 ImageView\(Button\)을 배치합니다.

  ```
    <EditText
        android:id="@+id/et_id"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:layout_toLeftOf="@+id/iv_call"
        android:layout_toRightOf="@+id/tv_id"
        android:hint="except '@'domain"
        android:textColor="#000000"/>

    <ImageView
        android:id="@+id/iv_call"
        android:layout_width="50dp"
        android:layout_height="50dp"
        android:layout_alignParentRight="true"
        android:padding="15dp"/>
  ```
  
> ## 기본 동작

* ### 구독자 등록

  앱에서 전화 상태 및 통화 종류에 따라서 해당되는 이벤트를 감지하고, 대응하기 위해 구독자 패턴\(Observer patten\)을 사용합니다. SDK에 정의된 구독자를 등록해주고, 해당 이벤트들을 감지하기 위해 SDK에 정의된 인터페이스를 구현 해줍니다.

  ```
  CallManager callManager = CallManager.getInstance();
  callManager.add((CallObserver.Register) this);
  callManager.add((CallObserver.Call) this);
  callManager.add((CallObserver.AdvancedCall) this);
  ```

* ### 푸시 알림으로 전화 받기 설정.

  앱이 실행되지 않는 상태에서도 FCM으로 전화 수신 알림을 받기 위해 Sample App에서는 VoicelocoFirebaseMessageingService 서비스를 이용하여 push 메시지를 전달 받습니다. 그 후 통화 화면을 띄워주는 방식으로 전화 알림을 설정합니다.

  ```
  if (event != null) {
      switch (event) {
          case "call" :
              Intent intent = new Intent(this, MainActivity.class);
              intent.setAction(MainActivity.ACTION_INCOMING_CALL);
              StringTokenizer tokenizer = new StringTokenizer(fromAccount, "@");
              intent.putExtra(MainActivity.COUNTERPARTY_ACCOUNT, tokenizer.nextToken());
              intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
              this.startActivity(intent);
              break;
      }
  }
  ```

* ### 전화 발신하기

  새로운 전화를 발신하기 위해 Voice 클래스에 있는 call 메서드를 아래와 같이 사용합니다.

  ```
  callInfo = new Voice().call(this, counterpartyAccount, appId);
  ```

  전화 발신 후 등록해 놓은 AdvancedCall 옵저버의 onOutgoingCall 인터페이스가 수행됩니다. 통화 발신 후 처리를 위해 onOutgoingCall 인터페이스를 구현합니다.

  ```
   @Override
    public void onOutgoingCall() {
        Log.d(TAG, "onOutgoing");
        tvCallState.setText(String.format(Locale.getDefault(), "call state is : %s", callInfo.getCallState()));
        makeRingBackTone();
    }
  ```
* ### 앱 종료

    앱 종료시에는 미디어 서버에 등록과 관찰자들의 구독을 아래와 같이 해제합니다.
  
    ```
      protected void onDestroy() {
          super.onDestroy();
          unregisterReceiver(broadcastReceiver);
          Register.getInstance().stop();
  
          CallManager callManager = CallManager.getInstance();
          callManager.delete((CallObserver.Register) this);
          callManager.delete((CallObserver.Invite) this);
      }
    ```


